package com.payment.reconcile.order.controller;

import com.payment.reconcile.order.entity.PaymentOrder;
import com.payment.reconcile.order.service.PaymentOrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class PaymentOrderController {

    @Resource
    private PaymentOrderService paymentOrderService;

    @GetMapping("/{merchantOrderNo}")
    public Map<String, Object> getByMerchantOrderNo(@PathVariable String merchantOrderNo) {
        PaymentOrder order = paymentOrderService.getByMerchantOrderNo(merchantOrderNo);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", order);
        return result;
    }

    @GetMapping("/list")
    public Map<String, Object> listByDateRange(@RequestParam String startDate,
                                               @RequestParam String endDate) {
        List<PaymentOrder> orders = paymentOrderService.listByDateRange(startDate, endDate);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", orders);
        return result;
    }
}
