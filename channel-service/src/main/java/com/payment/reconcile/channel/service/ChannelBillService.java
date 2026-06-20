package com.payment.reconcile.channel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payment.reconcile.channel.entity.ChannelBill;
import com.payment.reconcile.channel.mapper.ChannelBillMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChannelBillService extends ServiceImpl<ChannelBillMapper, ChannelBill> {

    public ChannelBill getByMerchantOrderNo(String merchantOrderNo) {
        LambdaQueryWrapper<ChannelBill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChannelBill::getMerchantOrderNo, merchantOrderNo);
        return getOne(wrapper);
    }

    public List<ChannelBill> listByDateRange(String startDate, String endDate) {
        LambdaQueryWrapper<ChannelBill> wrapper = new LambdaQueryWrapper<>();
        wrapper.apply("DATE(trade_time) BETWEEN {0} AND {1}", startDate, endDate);
        return list(wrapper);
    }
}
