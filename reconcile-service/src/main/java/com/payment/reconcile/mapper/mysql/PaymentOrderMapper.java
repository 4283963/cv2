package com.payment.reconcile.mapper.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.reconcile.entity.PaymentOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PaymentOrderMapper extends BaseMapper<PaymentOrder> {

    @Select("SELECT * FROM payment_order WHERE merchant_order_no = #{merchantOrderNo}")
    PaymentOrder selectByMerchantOrderNo(@Param("merchantOrderNo") String merchantOrderNo);

    @Select("SELECT * FROM payment_order WHERE DATE(create_time) BETWEEN #{startDate} AND #{endDate}")
    List<PaymentOrder> selectByDateRange(@Param("startDate") String startDate,
                                         @Param("endDate") String endDate);
}
