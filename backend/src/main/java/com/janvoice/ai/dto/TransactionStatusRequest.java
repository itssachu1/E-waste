package com.janvoice.ai.dto;

import com.janvoice.ai.entity.Transaction;
import jakarta.validation.constraints.NotNull;

public class TransactionStatusRequest {
    @NotNull private Transaction.PaymentStatus paymentStatus;
    public Transaction.PaymentStatus getPaymentStatus(){return paymentStatus;} public void setPaymentStatus(Transaction.PaymentStatus v){paymentStatus=v;}
}
