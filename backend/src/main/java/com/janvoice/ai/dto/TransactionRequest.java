package com.janvoice.ai.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import com.janvoice.ai.entity.Transaction;

public class TransactionRequest {
    @NotNull private Long lotId;
    @NotNull @Positive private BigDecimal amount;
    @NotNull private Transaction.PaymentMode paymentMode;
    private String paymentReference;
    private String notes;
    public Long getLotId(){return lotId;} public void setLotId(Long v){lotId=v;} public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;} public Transaction.PaymentMode getPaymentMode(){return paymentMode;} public void setPaymentMode(Transaction.PaymentMode v){paymentMode=v;} public String getPaymentReference(){return paymentReference;} public void setPaymentReference(String v){paymentReference=v;} public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
}
