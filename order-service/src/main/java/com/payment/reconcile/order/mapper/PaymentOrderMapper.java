package com.payment.reconcile.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.reconcile.order.entity.PaymentOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaymentOrderMapper extends BaseMapper<PaymentOrder> {
}
