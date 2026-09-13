package com.janvoice.ai.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_transactions_lot", columnList = "lot_id"),
        @Index(name = "idx_transactions_collector", columnList = "collector_id"),
        @Index(name = "idx_transactions_recycler", columnList = "recycler_id"),
        @Index(name = "idx_transactions_payment_status", columnList = "payment_status"),
        @Index(name = "idx_transactions_transaction_time", columnList = "transaction_time")
})
public class Transaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "lot_id", nullable = false) private Lot lot;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "collector_id", nullable = false) private User collector;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "recycler_id") private Recycler recycler;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal amount;
    // Stored as VARCHAR by the Flyway schema (V1); force the JDBC type so
    // Hibernate's MySQL dialect does not expect a native ENUM column.
    @JdbcTypeCode(SqlTypes.VARCHAR) @Enumerated(EnumType.STRING) @Column(name = "payment_mode", nullable = false, length = 30) private PaymentMode paymentMode;
    @Column(name = "payment_reference", length = 160) private String paymentReference;
    @JdbcTypeCode(SqlTypes.VARCHAR) @Enumerated(EnumType.STRING) @Column(name = "payment_status", nullable = false, length = 30) private PaymentStatus paymentStatus;
    @Column(length = 1000) private String notes;
    @Column(name = "transaction_time") private LocalDateTime transactionTime;
    @Column(name = "created_by") private Long createdBy;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
    @PrePersist void onCreate(){LocalDateTime now=LocalDateTime.now();createdAt=now;updatedAt=now;} @PreUpdate void onUpdate(){updatedAt=LocalDateTime.now();}
    public Long getId(){return id;} public void setId(Long v){id=v;} public Lot getLot(){return lot;} public void setLot(Lot v){lot=v;} public User getCollector(){return collector;} public void setCollector(User v){collector=v;} public Recycler getRecycler(){return recycler;} public void setRecycler(Recycler v){recycler=v;} public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;} public PaymentMode getPaymentMode(){return paymentMode;} public void setPaymentMode(PaymentMode v){paymentMode=v;} public String getPaymentReference(){return paymentReference;} public void setPaymentReference(String v){paymentReference=v;} public PaymentStatus getPaymentStatus(){return paymentStatus;} public void setPaymentStatus(PaymentStatus v){paymentStatus=v;} public String getNotes(){return notes;} public void setNotes(String v){notes=v;} public LocalDateTime getTransactionTime(){return transactionTime;} public void setTransactionTime(LocalDateTime v){transactionTime=v;} public Long getCreatedBy(){return createdBy;} public void setCreatedBy(Long v){createdBy=v;} public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;} public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime v){updatedAt=v;}
    public enum PaymentMode { CASH, UPI, BANK_TRANSFER, OTHER }
    public enum PaymentStatus { PENDING, PAID, FAILED, CANCELLED }
}
