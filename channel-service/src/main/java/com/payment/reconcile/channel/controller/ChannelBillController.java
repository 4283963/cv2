package com.payment.reconcile.channel.controller;

import com.payment.reconcile.channel.entity.ChannelBill;
import com.payment.reconcile.channel.service.ChannelBillService;
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
@RequestMapping("/api/bills")
public class ChannelBillController {

    @Resource
    private ChannelBillService channelBillService;

    @GetMapping("/{merchantOrderNo}")
    public Map<String, Object> getByMerchantOrderNo(@PathVariable String merchantOrderNo) {
        ChannelBill bill = channelBillService.getByMerchantOrderNo(merchantOrderNo);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", bill);
        return result;
    }

    @GetMapping("/list")
    public Map<String, Object> listByDateRange(@RequestParam String startDate,
                                               @RequestParam String endDate) {
        List<ChannelBill> bills = channelBillService.listByDateRange(startDate, endDate);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", bills);
        return result;
    }
}
