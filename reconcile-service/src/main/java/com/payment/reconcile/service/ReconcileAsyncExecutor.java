package com.payment.reconcile.service;

import com.payment.reconcile.config.ThreadPoolConfig;
import com.payment.reconcile.entity.ChannelBill;
import com.payment.reconcile.entity.PaymentOrder;
import com.payment.reconcile.entity.ReconcileDiff;
import com.payment.reconcile.mapper.mysql.PaymentOrderMapper;
import com.payment.reconcile.mapper.mysql.ReconcileDiffMapper;
import com.payment.reconcile.mapper.postgresql.ChannelBillMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ReconcileAsyncExecutor {

    private static final Logger log = LoggerFactory.getLogger(ReconcileAsyncExecutor.class);

    private static final int PROGRESS_UPDATE_INTERVAL = 100;

    @Resource
    private PaymentOrderMapper paymentOrderMapper;

    @Resource
    private ChannelBillMapper channelBillMapper;

    @Resource
    private ReconcileDiffMapper reconcileDiffMapper;

    @Resource
    private ReconcileService reconcileService;

    @Resource
    private ReconcileTaskService reconcileTaskService;

    @Async(ThreadPoolConfig.RECONCILE_EXECUTOR)
    public void doAsyncReconcile(String taskId, String startDate, String endDate,
                                 String batchNo, String remark) {
        log.info("========== [异步] 开始批量对账 ========== taskId={}, startDate={}, endDate={}, thread={}",
                taskId, startDate, endDate, Thread.currentThread().getName());

        reconcileTaskService.startTask(taskId);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger diffCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);
        AtomicInteger processedCount = new AtomicInteger(0);

        List<Map<String, Object>> diffDetails = new ArrayList<>();
        String errorMsg = null;
        boolean overallSuccess = true;

        try {
            List<PaymentOrder> orders = paymentOrderMapper.selectByDateRange(startDate, endDate);
            List<ChannelBill> bills = channelBillMapper.selectByDateRange(startDate, endDate);

            log.info("[异步taskId={}] 查询完成 - 订单侧记录数: {}, 渠道侧记录数: {}",
                    taskId, orders.size(), bills.size());

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

            int totalCount = allOrderNos.size();
            log.info("[异步taskId={}] 合并去重后总记录数: {}", taskId, totalCount);

            reconcileTaskService.updateProgress(taskId, totalCount, 0, 0, 0);

            int bothMissingCount = 0;

            for (String merchantOrderNo : allOrderNos) {
                try {
                    PaymentOrder order = orderMap.get(merchantOrderNo);
                    ChannelBill bill = billMap.get(merchantOrderNo);

                    if (order == null && bill == null) {
                        bothMissingCount++;
                        processedCount.incrementAndGet();
                        continue;
                    }

                    ReconcileDiff diff = reconcileService.doReconcileInternal(order, bill, merchantOrderNo, batchNo, remark);

                    if (diff != null) {
                        int rows = reconcileDiffMapper.insert(diff);
                        if (rows <= 0) {
                            log.error("[异步taskId={}] 差异记录插入失败!!! merchantOrderNo={}", taskId, merchantOrderNo);
                            failedCount.incrementAndGet();
                        } else {
                            diffCount.incrementAndGet();
                            Map<String, Object> diffInfo = new HashMap<>();
                            diffInfo.put("merchantOrderNo", merchantOrderNo);
                            diffInfo.put("diffType", diff.getDiffType());
                            diffInfo.put("diffField", diff.getDiffField());
                            diffInfo.put("diffId", diff.getId());
                            diffDetails.add(diffInfo);
                        }
                    } else {
                        successCount.incrementAndGet();
                    }
                    processedCount.incrementAndGet();

                } catch (Exception e) {
                    log.error("[异步taskId={}] 单条对账处理异常, merchantOrderNo={}, error={}",
                            taskId, merchantOrderNo, e.getMessage(), e);
                    failedCount.incrementAndGet();
                    processedCount.incrementAndGet();
                }

                int processed = processedCount.get();
                if (processed % PROGRESS_UPDATE_INTERVAL == 0 || processed == totalCount) {
                    reconcileTaskService.updateProgress(taskId, totalCount,
                            successCount.get(), diffCount.get(), failedCount.get());
                    log.debug("[异步taskId={}] 进度更新: {}/{} ({}%), success={}, diff={}, failed={}",
                            taskId, processed, totalCount,
                            new BigDecimal(processed).multiply(new BigDecimal(100))
                                    .divide(new BigDecimal(totalCount > 0 ? totalCount : 1), 2, BigDecimal.ROUND_HALF_UP),
                            successCount.get(), diffCount.get(), failedCount.get());
                }
            }

            reconcileTaskService.updateProgress(taskId, totalCount,
                    successCount.get(), diffCount.get(), failedCount.get());

            log.info("========== [异步] 批量对账完成 ========== taskId={}, 总记录: {}, 一致: {}, 差异: {}, 失败: {}, 两边缺失: {}",
                    taskId, totalCount, successCount.get(), diffCount.get(), failedCount.get(), bothMissingCount);

        } catch (Exception e) {
            overallSuccess = false;
            errorMsg = e.getMessage();
            log.error("========== [异步] 批量对账异常 ========== taskId={}, error={}", taskId, e.getMessage(), e);
        }

        reconcileTaskService.finishTask(taskId, overallSuccess && failedCount.get() == 0, errorMsg);
    }
}
