package com.payment.reconcile.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("reconcile_diff")
public class ReconcileDiff {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String merchantOrderNo;

    private String diffType;

    private BigDecimal orderAmount;

    private BigDecimal channelAmount;

    private String orderStatus;

    private String channelStatus;

    private String diffField;

    private String diffDetail;

    private String reconcileBatchNo;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMerchantOrderNo() {
        return merchantOrderNo;
    }

    public void setMerchantOrderNo(String merchantOrderNo) {
        this.merchantOrderNo = merchantOrderNo;
    }

    public String getDiffType() {
        return diffType;
    }

    public void setDiffType(String diffType) {
        this.diffType = diffType;
    }

    public BigDecimal getOrderAmount() {
        return orderAmount;
    }

    public void setOrderAmount(BigDecimal orderAmount) {
        this.orderAmount = orderAmount;
    }

    public BigDecimal getChannelAmount() {
        return channelAmount;
    }

    public void setChannelAmount(BigDecimal channelAmount) {
        this.channelAmount = channelAmount;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public String getChannelStatus() {
        return channelStatus;
    }

    public void setChannelStatus(String channelStatus) {
        this.channelStatus = channelStatus;
    }

    public String getDiffField() {
        return diffField;
    }

    public void setDiffField(String diffField) {
        this.diffField = diffField;
    }

    public String getDiffDetail() {
        return diffDetail;
    }

    public void setDiffDetail(String diffDetail) {
        this.diffDetail = diffDetail;
    }

    public String getReconcileBatchNo() {
        return reconcileBatchNo;
    }

    public void setReconcileBatchNo(String reconcileBatchNo) {
        this.reconcileBatchNo = reconcileBatchNo;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
