package com.vnpay.springboot.Dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class PaymentRequest {

    @Min(10000)
    private long amount;

    @NotBlank
    @Size(max = 255)
    private String orderInfo;

    @Size(max = 30)
    private String bankcode;

    @NotBlank
    @Size(max = 50)
    private String ordertype;

    @Size(max = 50)
    private String promocode;

    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9]{1,32}$")
    private String txnRef;

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public String getOrderInfo() {
        return orderInfo;
    }

    public void setOrderInfo(String orderInfo) {
        this.orderInfo = orderInfo;
    }

    public String getBankcode() {
        return bankcode;
    }

    public void setBankcode(String bankcode) {
        this.bankcode = bankcode;
    }

    public String getOrdertype() {
        return ordertype;
    }

    public void setOrdertype(String ordertype) {
        this.ordertype = ordertype;
    }

    public String getPromocode() {
        return promocode;
    }

    public void setPromocode(String promocode) {
        this.promocode = promocode;
    }

    public String getTxnRef() {
        return txnRef;
    }

    public void setTxnRef(String txnRef) {
        this.txnRef = txnRef;
    }
}
