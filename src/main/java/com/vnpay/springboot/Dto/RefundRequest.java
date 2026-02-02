package com.vnpay.springboot.Dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RefundRequest {

    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9]{1,32}$")
    private String txnRef;

    @NotBlank
    @Size(max = 20)
    private String transactionNo;

    @Min(1000)
    private long amount;

    @NotBlank
    @Pattern(regexp = "^(02|03)$")
    private String transType;

    @NotBlank
    @Size(max = 50)
    private String createBy;

    @NotBlank
    @Pattern(regexp = "^\\d{14}$")
    private String transDate;

    public String getTxnRef() {
        return txnRef;
    }

    public void setTxnRef(String txnRef) {
        this.txnRef = txnRef;
    }

    public String getTransactionNo() {
        return transactionNo;
    }

    public void setTransactionNo(String transactionNo) {
        this.transactionNo = transactionNo;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public String getTransType() {
        return transType;
    }

    public void setTransType(String transType) {
        this.transType = transType;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public String getTransDate() {
        return transDate;
    }

    public void setTransDate(String transDate) {
        this.transDate = transDate;
    }
}
