package com.payment.reconcile.controller;

import com.payment.reconcile.dto.BatchReconcileRequest;
import com.payment.reconcile.dto.ReconcileRequest;
import com.payment.reconcile.entity.ReconcileDiff;
import com.payment.reconcile.entity.ReconcileTask;
import com.payment.reconcile.service.ReconcileService;
import com.payment.reconcile.service.ReconcileTaskService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/reconcile")
public class ReconcileController {

    @Resource
    private ReconcileService reconcileService;

    @Resource
    private ReconcileTaskService reconcileTaskService;

    @PostMapping("/single")
    public Map<String, Object> reconcileSingle(@RequestBody @Valid ReconcileRequest request) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> reconcileResult = reconcileService.reconcileSingle(request);
            result.put("code", 200);
            result.put("message", "对账完成");
            result.put("data", reconcileResult);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "对账失败: " + e.getMessage());
        }
        return result;
    }

    @PostMapping("/batch")
    public Map<String, Object> reconcileBatch(@RequestBody @Valid BatchReconcileRequest request) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> reconcileResult = reconcileService.reconcileBatch(request);
            result.put("code", 200);
            result.put("message", "批量对账完成");
            result.put("data", reconcileResult);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "批量对账失败: " + e.getMessage());
        }
        return result;
    }

    @PostMapping("/batch/async")
    public Map<String, Object> reconcileBatchAsync(@RequestBody @Valid BatchReconcileRequest request) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> submitResult = reconcileService.submitBatchTask(request);
            result.put("code", 200);
            result.put("message", "任务已提交");
            result.put("data", submitResult);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "任务提交失败: " + e.getMessage());
        }
        return result;
    }

    @GetMapping("/task/status")
    public Map<String, Object> getTaskStatus(@RequestParam String taskId) {
        Map<String, Object> result = new HashMap<>();
        try {
            ReconcileTask task = reconcileTaskService.getByTaskId(taskId);
            if (task == null) {
                result.put("code", 404);
                result.put("message", "任务不存在");
            } else {
                result.put("code", 200);
                result.put("message", "success");
                Map<String, Object> taskInfo = new HashMap<>();
                taskInfo.put("taskId", task.getTaskId());
                taskInfo.put("taskStatus", task.getTaskStatus());
                taskInfo.put("taskType", task.getTaskType());
                taskInfo.put("startDate", task.getStartDate());
                taskInfo.put("endDate", task.getEndDate());
                taskInfo.put("reconcileBatchNo", task.getReconcileBatchNo());
                taskInfo.put("totalCount", task.getTotalCount());
                taskInfo.put("successCount", task.getSuccessCount());
                taskInfo.put("diffCount", task.getDiffCount());
                taskInfo.put("failedCount", task.getFailedCount());
                taskInfo.put("progressPct", task.getProgressPct());
                taskInfo.put("errorMsg", task.getErrorMsg());
                taskInfo.put("startTime", task.getStartTime());
                taskInfo.put("finishTime", task.getFinishTime());
                taskInfo.put("createTime", task.getCreateTime());
                taskInfo.put("remark", task.getRemark());
                result.put("data", taskInfo);
            }
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "查询失败: " + e.getMessage());
        }
        return result;
    }

    @GetMapping("/task/list")
    public Map<String, Object> getTaskList(
            @RequestParam(required = false) String taskStatus,
            @RequestParam(defaultValue = "20") int limit) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<ReconcileTask> tasks = reconcileTaskService.listTasks(taskStatus, limit);
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", tasks);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "查询失败: " + e.getMessage());
        }
        return result;
    }

    @GetMapping("/diff/list")
    public Map<String, Object> getDiffList(
            @RequestParam(required = false) String merchantOrderNo,
            @RequestParam(required = false) String diffType) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<ReconcileDiff> diffList = reconcileService.getDiffList(merchantOrderNo, diffType);
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", diffList);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "查询失败: " + e.getMessage());
        }
        return result;
    }
}
