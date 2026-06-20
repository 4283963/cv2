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
        log.info("开始单笔对账, merchantOrderNo: {}", request.getMerchantOrderNo());

        String merchantOrderNo = request.getMerchantOrderNo();
        String batchNo = request.getReconcileBatchNo() != null ?
                request.getReconcileBatchNo() : generateBatchNo();

        PaymentOrder order = paymentOrderMapper.selectByMerchantOrderNo(merchantOrderNo);
        ChannelBill bill = channelBillMapper.selectByMerchantOrderNo(merchantOrderNo);

        Map<String, Object> result = new HashMap<>();
        result.put("merchantOrderNo", merchantOrderNo);
        result.put("reconcileBatchNo", batchNo);

        if (order == null && bill == null) {
            result.put("status", "BOTH_MISSING");
            result.put("message", "两边都无此订单");
            return result;
        }

        ReconcileDiff diff = doReconcile(order, bill, merchantOrderNo, batchNo, request.getRemark());

        if (diff != null) {
            reconcileDiffMapper.insert(diff);
            result.put("status", "DIFF");
            result.put("diffType", diff.getDiffType());
            result.put("diffField", diff.getDiffField());
            result.put("diffDetail", diff.getDiffDetail());
            log.info("对账发现差异, merchantOrderNo: {}, diffType: {}", merchantOrderNo, diff.getDiffType());
        } else {
            result.put("status", "MATCH");
            result.put("message", "对账一致");
            log.info("对账一致, merchantOrderNo: {}", merchantOrderNo);
        }

        return result;
    }

    @Transactional(transactionManager = "mysqlTransactionManager", rollbackFor = Exception.class)
    public Map<String, Object> reconcileBatch(BatchReconcileRequest request) {
        log.info("开始批量对账, startDate: {}, endDate: {}", request.getStartDate(), request.getEndDate());

        String batchNo = request.getReconcileBatchNo() != null ?
                request.getReconcileBatchNo() : generateBatchNo();

        List<PaymentOrder> orders = paymentOrderMapper.selectByDateRange(
                request.getStartDate(), request.getEndDate());
        List<ChannelBill> bills = channelBillMapper.selectByDateRange(
                request.getStartDate(), request.getEndDate());

        log.info("订单侧记录数: {}, 渠道侧记录数: {}", orders.size(), bills.size());

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

        List<Map<String, Object>> diffDetails = new ArrayList<>();
        int matchCount = 0;
        int diffCount = 0;

        for (String merchantOrderNo : allOrderNos) {
            PaymentOrder order = orderMap.get(merchantOrderNo);
            ChannelBill bill = billMap.get(merchantOrderNo);

            ReconcileDiff diff = doReconcile(order, bill, merchantOrderNo, batchNo, request.getRemark());

            if (diff != null) {
                reconcileDiffMapper.insert(diff);
                diffCount++;
                Map<String, Object> diffInfo = new HashMap<>();
                diffInfo.put("merchantOrderNo", merchantOrderNo);
                diffInfo.put("diffType", diff.getDiffType());
                diffInfo.put("diffField", diff.getDiffField());
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
        result.put("diffDetails", diffDetails);

        log.info("批量对账完成, 批次号: {}, 总记录: {}, 一致: {}, 差异: {}",
                batchNo, allOrderNos.size(), matchCount, diffCount);

        return result;
    }

    private ReconcileDiff doReconcile(PaymentOrder order, ChannelBill bill,
                                      String merchantOrderNo, String batchNo, String remark) {
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
            return diff;
        }

        if (bill == null) {
            diff.setDiffType("CHANNEL_MISSING");
            diff.setOrderAmount(order.getAmount());
            diff.setOrderStatus(order.getStatus());
            diff.setDiffField("channel_side");
            diff.setDiffDetail("渠道侧无此记录，订单侧金额=" + order.getAmount() +
                    ", 状态=" + order.getStatus());
            return diff;
        }

        diff.setOrderAmount(order.getAmount());
        diff.setOrderStatus(order.getStatus());
        diff.setChannelAmount(bill.getBillAmount());
        diff.setChannelStatus(bill.getBillStatus());

        List<String> diffFields = new ArrayList<>();
        List<String> diffDetails = new ArrayList<>();

        if (order.getAmount().compareTo(bill.getBillAmount()) != 0) {
            diffFields.add("amount");
            diffDetails.add("金额不一致, 订单侧=" + order.getAmount() +
                    ", 渠道侧=" + bill.getBillAmount());
        }

        String orderStatus = normalizeStatus(order.getStatus());
        String channelStatus = normalizeStatus(bill.getBillStatus());
        if (!orderStatus.equals(channelStatus)) {
            diffFields.add("status");
            diffDetails.add("状态不一致, 订单侧=" + order.getStatus() +
                    ", 渠道侧=" + bill.getBillStatus());
        }

        if (!diffFields.isEmpty()) {
            diff.setDiffType("DATA_DIFF");
            diff.setDiffField(String.join(",", diffFields));
            diff.setDiffDetail(String.join("; ", diffDetails));
            return diff;
        }

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
