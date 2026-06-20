package com.payment.reconcile.dto;

import javax.validation.constraints.NotBlank;

public class BatchReconcileRequest {

    @NotBlank(message = "开始日期不能为空")
    private String startDate;

    @NotBlank(message = "结束日期不能为空")
    private String endDate;

    private String reconcileBatchNo;

    private String remark;

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
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
