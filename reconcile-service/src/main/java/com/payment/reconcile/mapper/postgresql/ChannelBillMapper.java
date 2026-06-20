package com.payment.reconcile.mapper.postgresql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.reconcile.entity.ChannelBill;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ChannelBillMapper extends BaseMapper<ChannelBill> {

    @Select("SELECT * FROM channel_bill WHERE merchant_order_no = #{merchantOrderNo}")
    ChannelBill selectByMerchantOrderNo(@Param("merchantOrderNo") String merchantOrderNo);

    @Select("SELECT * FROM channel_bill WHERE DATE(trade_time) BETWEEN #{startDate} AND #{endDate}")
    List<ChannelBill> selectByDateRange(@Param("startDate") String startDate,
                                        @Param("endDate") String endDate);
}
