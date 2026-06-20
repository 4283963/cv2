package com.payment.reconcile.dto;

import javax.validation.constraints.NotBlank;

public class ReconcileRequest {

    @NotBlank(message = "商户订单号不能为空")
    private String merchantOrderNo;

    private String reconcileBatchNo;

    private String remark;

    public String getMerchantOrderNo() {
        return merchantOrderNo;
    }

    public void setMerchantOrderNo(String merchantOrderNo) {
        this.merchantOrderNo = merchantOrderNo;
    }

    public String getReconcileBatchNo() {
        return reconcileBatchNo;
    }

    public void setReconcileBatchNo(String reconcileBatchNo) {
        this.reconcileBatchNo = reconcileBatchNo;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
