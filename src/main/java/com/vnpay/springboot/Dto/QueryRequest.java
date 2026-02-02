package com.vnpay.springboot.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class QueryRequest {

    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9]{1,32}$")
    private String txnRef;

    @NotBlank
    @Pattern(regexp = "^\\d{14}$")
    private String transDate;

    @Size(max = 20)
    private String transactionNo;

    public String getTxnRef() {
        return txnRef;
    }

    public void setTxnRef(String txnRef) {
        this.txnRef = txnRef;
    }

    public String getTransDate() {
        return transDate;
    }

    public void setTransDate(String transDate) {
        this.transDate = transDate;
    }

    public String getTransactionNo() {
        return transactionNo;
    }

    public void setTransactionNo(String transactionNo) {
        this.transactionNo = transactionNo;
    }
}
