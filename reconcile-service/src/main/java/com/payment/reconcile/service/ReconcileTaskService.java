package com.payment.reconcile.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payment.reconcile.entity.ReconcileTask;
import com.payment.reconcile.mapper.mysql.ReconcileTaskMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReconcileTaskService extends ServiceImpl<ReconcileTaskMapper, ReconcileTask> {

    private static final Logger log = LoggerFactory.getLogger(ReconcileTaskService.class);

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";

    public ReconcileTask createTask(String taskType, String startDate, String endDate,
                                     String reconcileBatchNo, String remark) {
        ReconcileTask task = new ReconcileTask();
        task.setTaskId(generateTaskId());
        task.setTaskStatus(STATUS_PENDING);
        task.setTaskType(taskType);
        task.setStartDate(startDate);
        task.setEndDate(endDate);
        task.setReconcileBatchNo(reconcileBatchNo);
        task.setTotalCount(0);
        task.setSuccessCount(0);
        task.setDiffCount(0);
        task.setFailedCount(0);
        task.setProgressPct(BigDecimal.ZERO);
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        task.setRemark(remark);

        save(task);
        log.info("创建对账任务成功, taskId={}, taskType={}", task.getTaskId(), taskType);
        return task;
    }

    public boolean startTask(String taskId) {
        ReconcileTask task = getByTaskId(taskId);
        if (task == null) {
            return false;
        }
        task.setTaskStatus(STATUS_PROCESSING);
        task.setStartTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        boolean result = updateById(task);
        log.info("任务开始, taskId={}, result={}", taskId, result);
        return result;
    }

    public void updateProgress(String taskId, int totalCount, int successCount,
                               int diffCount, int failedCount) {
        BigDecimal progressPct = BigDecimal.ZERO;
        if (totalCount > 0) {
            int processed = successCount + diffCount + failedCount;
            progressPct = new BigDecimal(processed)
                    .multiply(new BigDecimal(100))
                    .divide(new BigDecimal(totalCount), 2, RoundingMode.HALF_UP);
        }

        String status = STATUS_PROCESSING;
        if (totalCount > 0 && (successCount + diffCount + failedCount) >= totalCount) {
            status = failedCount > 0 ? STATUS_FAILED : STATUS_SUCCESS;
        }

        int rows = baseMapper.updateProgress(taskId, status, totalCount,
                successCount, diffCount, failedCount, progressPct);
        log.debug("更新任务进度, taskId={}, progress={}%, total={}, success={}, diff={}, failed={}, rows={}",
                taskId, progressPct, totalCount, successCount, diffCount, failedCount, rows);
    }

    public boolean finishTask(String taskId, boolean success, String errorMsg) {
        ReconcileTask task = getByTaskId(taskId);
        if (task == null) {
            return false;
        }
        task.setTaskStatus(success ? STATUS_SUCCESS : STATUS_FAILED);
        task.setFinishTime(LocalDateTime.now());
        task.setErrorMsg(errorMsg);
        task.setUpdateTime(LocalDateTime.now());

        if (success && task.getTotalCount() != null && task.getTotalCount() > 0) {
            task.setProgressPct(new BigDecimal(100));
        }

        boolean result = updateById(task);
        log.info("任务结束, taskId={}, success={}, errorMsg={}, result={}", taskId, success, errorMsg, result);
        return result;
    }

    public ReconcileTask getByTaskId(String taskId) {
        LambdaQueryWrapper<ReconcileTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReconcileTask::getTaskId, taskId);
        return getOne(wrapper);
    }

    public List<ReconcileTask> listTasks(String taskStatus, int limit) {
        LambdaQueryWrapper<ReconcileTask> wrapper = new LambdaQueryWrapper<>();
        if (taskStatus != null && !taskStatus.isEmpty()) {
            wrapper.eq(ReconcileTask::getTaskStatus, taskStatus);
        }
        wrapper.orderByDesc(ReconcileTask::getCreateTime);
        wrapper.last("LIMIT " + limit);
        return list(wrapper);
    }

    private String generateTaskId() {
        return "TASK" + System.currentTimeMillis() + String.format("%04d", (int) (Math.random() * 10000));
    }
}
