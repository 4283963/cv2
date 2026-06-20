package com.payment.reconcile.controller;

import com.payment.reconcile.dto.BatchReconcileRequest;
import com.payment.reconcile.dto.ReconcileRequest;
import com.payment.reconcile.entity.ReconcileDiff;
import com.payment.reconcile.service.ReconcileService;
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
