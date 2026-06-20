package com.payment.reconcile.service;

import cn.hutool.core.util.IdUtil;
import com.payment.reconcile.dto.BatchReconcileRequest;
import com.payment.reconcile.dto.ReconcileRequest;
import com.payment.reconcile.entity.ChannelBill;
import com.payment.reconcile.entity.PaymentOrder;
import com.payment.reconcile.entity.ReconcileDiff;
import com.payment.reconcile.mapper.mysql.PaymentOrderMapper;
import com.payment.reconcile.mapper.mysql.ReconcileDiffMapper;
import com.payment.reconcile.mapper.postgresql.ChannelBillMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ReconcileService {

    private static final Logger log = LoggerFactory.getLogger(ReconcileService.class);

    @Resource
    private PaymentOrderMapper paymentOrderMapper;

    @Resource
    private ChannelBillMapper channelBillMapper;

    @Resource
    private ReconcileDiffMapper reconcileDiffMapper;

    @Transactional(transactionManager = "mysqlTransactionManager", rollbackFor = Exception.class)
    public Map<String, Object> reconcileSingle(ReconcileRequest request) {
        String merchantOrderNo = request.getMerchantOrderNo();
        log.info("========== 开始单笔对账 ========== merchantOrderNo: {}", merchantOrderNo);

        String batchNo = request.getReconcileBatchNo() != null ?
                request.getReconcileBatchNo() : generateBatchNo();

        PaymentOrder order = paymentOrderMapper.selectByMerchantOrderNo(merchantOrderNo);
        ChannelBill bill = channelBillMapper.selectByMerchantOrderNo(merchantOrderNo);

        log.info("查询结果 - 订单侧: {}, 渠道侧: {}",
                order != null ? "存在(amount=" + order.getAmount() + ", status=" + order.getStatus() + ")" : "不存在",
                bill != null ? "存在(billAmount=" + bill.getBillAmount() + ", billStatus=" + bill.getBillStatus() + ")" : "不存在");

        Map<String, Object> result = new HashMap<>();
        result.put("merchantOrderNo", merchantOrderNo);
        result.put("reconcileBatchNo", batchNo);

        if (order == null && bill == null) {
            result.put("status", "BOTH_MISSING");
            result.put("message", "两边都无此订单");
            log.warn("两边都无此记录, merchantOrderNo: {}", merchantOrderNo);
            return result;
        }

        ReconcileDiff diff = doReconcile(order, bill, merchantOrderNo, batchNo, request.getRemark());

        if (diff != null) {
            log.info("判定为差异记录, diffType={}, diffField={}, diffDetail={}",
                    diff.getDiffType(), diff.getDiffField(), diff.getDiffDetail());

            int rows = reconcileDiffMapper.insert(diff);
            if (rows <= 0) {
                log.error("差异记录插入失败!!! merchantOrderNo={}, 受影响行数={}", merchantOrderNo, rows);
                throw new RuntimeException("差异记录插入失败, merchantOrderNo=" + merchantOrderNo);
            }
            log.info("差异记录插入成功, 受影响行数={}, 生成的diffId={}", rows, diff.getId());

            result.put("status", "DIFF");
            result.put("diffType", diff.getDiffType());
            result.put("diffField", diff.getDiffField());
            result.put("diffDetail", diff.getDiffDetail());
            result.put("diffId", diff.getId());
        } else {
            result.put("status", "MATCH");
            result.put("message", "对账一致");
            log.info("对账一致, merchantOrderNo: {}", merchantOrderNo);
        }

        return result;
    }

    @Transactional(transactionManager = "mysqlTransactionManager", rollbackFor = Exception.class)
    public Map<String, Object> reconcileBatch(BatchReconcileRequest request) {
        log.info("========== 开始批量对账 ========== startDate: {}, endDate: {}", request.getStartDate(), request.getEndDate());

        String batchNo = request.getReconcileBatchNo() != null ?
                request.getReconcileBatchNo() : generateBatchNo();

        List<PaymentOrder> orders = paymentOrderMapper.selectByDateRange(
                request.getStartDate(), request.getEndDate());
        List<ChannelBill> bills = channelBillMapper.selectByDateRange(
                request.getStartDate(), request.getEndDate());

        log.info("查询完成 - 订单侧记录数: {}, 渠道侧记录数: {}", orders.size(), bills.size());

        Set<String> allOrderNos = new HashSet<>();
        Map<String, PaymentOrder> orderMap = new HashMap<>();
        for (PaymentOrder order : orders) {
            allOrderNos.add(order.getMerchantOrderNo());
            orderMap.put(order.getMerchantOrderNo(), order);
        }

        Map<String, ChannelBill> billMap = new HashMap<>();
        for (ChannelBill bill : bills) {
            allOrderNos.add(bill.getMerchantOrderNo());
            billMap.put(bill.getMerchantOrderNo(), bill);
        }

        log.info("合并后去重的商户订单号总数: {}", allOrderNos.size());

        List<Map<String, Object>> diffDetails = new ArrayList<>();
        int matchCount = 0;
        int diffCount = 0;
        int bothMissingCount = 0;

        for (String merchantOrderNo : allOrderNos) {
            PaymentOrder order = orderMap.get(merchantOrderNo);
            ChannelBill bill = billMap.get(merchantOrderNo);

            if (order == null && bill == null) {
                bothMissingCount++;
                continue;
            }

            ReconcileDiff diff = doReconcile(order, bill, merchantOrderNo, batchNo, request.getRemark());

            if (diff != null) {
                int rows = reconcileDiffMapper.insert(diff);
                if (rows <= 0) {
                    log.error("批量对账-差异记录插入失败!!! merchantOrderNo={}", merchantOrderNo);
                    throw new RuntimeException("批量差异记录插入失败, merchantOrderNo=" + merchantOrderNo);
                }
                diffCount++;
                Map<String, Object> diffInfo = new HashMap<>();
                diffInfo.put("merchantOrderNo", merchantOrderNo);
                diffInfo.put("diffType", diff.getDiffType());
                diffInfo.put("diffField", diff.getDiffField());
                diffInfo.put("diffId", diff.getId());
                diffDetails.add(diffInfo);
            } else {
                matchCount++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("reconcileBatchNo", batchNo);
        result.put("totalCount", allOrderNos.size());
        result.put("matchCount", matchCount);
        result.put("diffCount", diffCount);
        result.put("bothMissingCount", bothMissingCount);
        result.put("diffDetails", diffDetails);

        log.info("========== 批量对账完成 ========== 批次号: {}, 总记录: {}, 一致: {}, 差异: {}, 两边缺失: {}",
                batchNo, allOrderNos.size(), matchCount, diffCount, bothMissingCount);

        return result;
    }

    private ReconcileDiff doReconcile(PaymentOrder order, ChannelBill bill,
                                      String merchantOrderNo, String batchNo, String remark) {
        log.debug("[doReconcile] 进入比对, merchantOrderNo={}", merchantOrderNo);

        ReconcileDiff diff = new ReconcileDiff();
        diff.setMerchantOrderNo(merchantOrderNo);
        diff.setReconcileBatchNo(batchNo);
        diff.setCreateTime(LocalDateTime.now());
        diff.setUpdateTime(LocalDateTime.now());
        diff.setRemark(remark);

        if (order == null) {
            diff.setDiffType("ORDER_MISSING");
            diff.setChannelAmount(bill.getBillAmount());
            diff.setChannelStatus(bill.getBillStatus());
            diff.setDiffField("order_side");
            diff.setDiffDetail("订单侧无此记录，渠道侧金额=" + bill.getBillAmount() +
                    ", 状态=" + bill.getBillStatus());
            log.debug("[doReconcile] 判定差异: ORDER_MISSING, merchantOrderNo={}", merchantOrderNo);
            return diff;
        }

        if (bill == null) {
            diff.setDiffType("CHANNEL_MISSING");
            diff.setOrderAmount(order.getAmount());
            diff.setOrderStatus(order.getStatus());
            diff.setDiffField("channel_side");
            diff.setDiffDetail("渠道侧无此记录，订单侧金额=" + order.getAmount() +
                    ", 状态=" + order.getStatus());
            log.debug("[doReconcile] 判定差异: CHANNEL_MISSING, merchantOrderNo={}", merchantOrderNo);
            return diff;
        }

        BigDecimal orderAmount = order.getAmount();
        BigDecimal billAmount = bill.getBillAmount();
        String orderStatusRaw = order.getStatus();
        String billStatusRaw = bill.getBillStatus();

        diff.setOrderAmount(orderAmount);
        diff.setOrderStatus(orderStatusRaw);
        diff.setChannelAmount(billAmount);
        diff.setChannelStatus(billStatusRaw);

        List<String> diffFields = new ArrayList<>();
        List<String> diffDetails = new ArrayList<>();

        int amountCompareResult;
        if (orderAmount == null && billAmount == null) {
            amountCompareResult = 0;
            log.debug("[doReconcile] 两边金额都为null, merchantOrderNo={}", merchantOrderNo);
        } else if (orderAmount == null) {
            amountCompareResult = -1;
            diffFields.add("amount");
            diffDetails.add("金额不一致, 订单侧=null(缺失), 渠道侧=" + billAmount);
            log.warn("[doReconcile] 订单侧金额为null, merchantOrderNo={}", merchantOrderNo);
        } else if (billAmount == null) {
            amountCompareResult = 1;
            diffFields.add("amount");
            diffDetails.add("金额不一致, 订单侧=" + orderAmount + ", 渠道侧=null(缺失)");
            log.warn("[doReconcile] 渠道侧金额为null, merchantOrderNo={}", merchantOrderNo);
        } else {
            amountCompareResult = orderAmount.compareTo(billAmount);
            log.debug("[doReconcile] 金额比对: orderAmount={}, billAmount={}, compareResult={}, merchantOrderNo={}",
                    orderAmount, billAmount, amountCompareResult, merchantOrderNo);
            if (amountCompareResult != 0) {
                diffFields.add("amount");
                diffDetails.add("金额不一致, 订单侧=" + orderAmount + ", 渠道侧=" + billAmount);
            }
        }

        String orderStatus = normalizeStatus(orderStatusRaw);
        String channelStatus = normalizeStatus(billStatusRaw);
        log.debug("[doReconcile] 状态比对: 原始订单侧={}→标准化后={}, 原始渠道侧={}→标准化后={}, merchantOrderNo={}",
                orderStatusRaw, orderStatus, billStatusRaw, channelStatus, merchantOrderNo);

        if (!orderStatus.equals(channelStatus)) {
            diffFields.add("status");
            diffDetails.add("状态不一致, 订单侧=" + orderStatusRaw + "(" + orderStatus + ")" +
                    ", 渠道侧=" + billStatusRaw + "(" + channelStatus + ")");
        }

        log.debug("[doReconcile] 差异字段汇总: diffFields={}, merchantOrderNo={}", diffFields, merchantOrderNo);

        if (!diffFields.isEmpty()) {
            diff.setDiffType("DATA_DIFF");
            diff.setDiffField(String.join(",", diffFields));
            diff.setDiffDetail(String.join("; ", diffDetails));
            log.info("[doReconcile] 判定差异: DATA_DIFF, fields=[{}], detail=[{}], merchantOrderNo={}",
                    diff.getDiffField(), diff.getDiffDetail(), merchantOrderNo);
            return diff;
        }

        log.debug("[doReconcile] 对账完全一致, merchantOrderNo={}", merchantOrderNo);
        return null;
    }

    private String normalizeStatus(String status) {
        if (status == null) {
            return "";
        }
        switch (status.toUpperCase()) {
            case "SUCCESS":
            case "PAY_SUCCESS":
            case "TRADE_SUCCESS":
            case "PAID":
                return "SUCCESS";
            case "FAILED":
            case "FAIL":
            case "PAY_FAIL":
            case "TRADE_FAIL":
                return "FAILED";
            case "PROCESSING":
            case "PAYING":
            case "WAIT_PAY":
                return "PROCESSING";
            case "REFUND":
            case "REFUNDED":
            case "TRADE_REFUND":
                return "REFUND";
            case "CLOSED":
            case "CANCEL":
            case "TRADE_CLOSED":
                return "CLOSED";
            default:
                return status.toUpperCase();
        }
    }

    private String generateBatchNo() {
        return "RECON" + System.currentTimeMillis() + IdUtil.randomUUID().substring(0, 6).toUpperCase();
    }

    public List<ReconcileDiff> getDiffList(String merchantOrderNo, String diffType) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ReconcileDiff> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();

        if (merchantOrderNo != null && !merchantOrderNo.isEmpty()) {
            wrapper.eq(ReconcileDiff::getMerchantOrderNo, merchantOrderNo);
        }
        if (diffType != null && !diffType.isEmpty()) {
            wrapper.eq(ReconcileDiff::getDiffType, diffType);
        }
        wrapper.orderByDesc(ReconcileDiff::getCreateTime);

        return reconcileDiffMapper.selectList(wrapper);
    }
}
