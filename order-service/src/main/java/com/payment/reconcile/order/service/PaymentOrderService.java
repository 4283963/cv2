package com.payment.reconcile.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payment.reconcile.order.entity.PaymentOrder;
import com.payment.reconcile.order.mapper.PaymentOrderMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PaymentOrderService extends ServiceImpl<PaymentOrderMapper, PaymentOrder> {

    public PaymentOrder getByMerchantOrderNo(String merchantOrderNo) {
        LambdaQueryWrapper<PaymentOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentOrder::getMerchantOrderNo, merchantOrderNo);
        return getOne(wrapper);
    }

    public List<PaymentOrder> listByDateRange(String startDate, String endDate) {
        LambdaQueryWrapper<PaymentOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.apply("DATE(create_time) BETWEEN {0} AND {1}", startDate, endDate);
        return list(wrapper);
    }
}
