package com.janvoice.ai.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "price_records", indexes = {
        @Index(name = "idx_price_records_material", columnList = "material_id"),
        @Index(name = "idx_price_records_location", columnList = "location"),
        @Index(name = "idx_price_records_verification_status", columnList = "verification_status"),
        @Index(name = "idx_price_records_quoted_at", columnList = "quoted_at")
})
public class PriceRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private MaterialMaster material;

    @Column(nullable = false, length = 160)
    private String location;

    @Enumerated(EnumType.STRING) @Column(name = "buyer_type", nullable = false, length = 40)
    private BuyerType buyerType;

    @Column(name = "buyer_id")
    private Long buyerId;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal rate;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private MaterialUnit unit;

    @Column(length = 80) private String grade;
    @Column(name = "condition_description", length = 300) private String condition;
    @Enumerated(EnumType.STRING) @Column(name = "source_type", nullable = false, length = 40) private SourceType sourceType;
    @Column(name = "source_reference", length = 500) private String sourceReference;
    @Column(name = "quoted_at", nullable = false) private LocalDateTime quotedAt;
    @Column(name = "valid_until") private LocalDateTime validUntil;
    @Enumerated(EnumType.STRING) @Column(name = "verification_status", nullable = false, length = 20) private VerificationStatus verificationStatus = VerificationStatus.UNVERIFIED;
    @Column(name = "created_by") private Long createdBy;
    @Column(name = "verified_by") private Long verifiedBy;
    @Column(name = "verified_at") private LocalDateTime verifiedAt;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

    @PrePersist void onCreate() { LocalDateTime now = LocalDateTime.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; } public void setId(Long value) { id = value; }
    public MaterialMaster getMaterial() { return material; } public void setMaterial(MaterialMaster value) { material = value; }
    public String getLocation() { return location; } public void setLocation(String value) { location = value; }
    public BuyerType getBuyerType() { return buyerType; } public void setBuyerType(BuyerType value) { buyerType = value; }
    public Long getBuyerId() { return buyerId; } public void setBuyerId(Long value) { buyerId = value; }
    public BigDecimal getRate() { return rate; } public void setRate(BigDecimal value) { rate = value; }
    public MaterialUnit getUnit() { return unit; } public void setUnit(MaterialUnit value) { unit = value; }
    public String getGrade() { return grade; } public void setGrade(String value) { grade = value; }
    public String getCondition() { return condition; } public void setCondition(String value) { condition = value; }
    public SourceType getSourceType() { return sourceType; } public void setSourceType(SourceType value) { sourceType = value; }
    public String getSourceReference() { return sourceReference; } public void setSourceReference(String value) { sourceReference = value; }
    public LocalDateTime getQuotedAt() { return quotedAt; } public void setQuotedAt(LocalDateTime value) { quotedAt = value; }
    public LocalDateTime getValidUntil() { return validUntil; } public void setValidUntil(LocalDateTime value) { validUntil = value; }
    public VerificationStatus getVerificationStatus() { return verificationStatus; } public void setVerificationStatus(VerificationStatus value) { verificationStatus = value; }
    public Long getCreatedBy() { return createdBy; } public void setCreatedBy(Long value) { createdBy = value; }
    public Long getVerifiedBy() { return verifiedBy; } public void setVerifiedBy(Long value) { verifiedBy = value; }
    public LocalDateTime getVerifiedAt() { return verifiedAt; } public void setVerifiedAt(LocalDateTime value) { verifiedAt = value; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime value) { createdAt = value; }
    public LocalDateTime getUpdatedAt() { return updatedAt; } public void setUpdatedAt(LocalDateTime value) { updatedAt = value; }

    public enum BuyerType { RECYCLER, AGGREGATOR, KABADIWALA, OTHER }
    public enum SourceType { RECYCLER_QUOTE, AGGREGATOR_QUOTE, FIELD_SURVEY, VERIFIED_TRANSACTION, OTHER_VERIFIED_SOURCE }
    public enum VerificationStatus { UNVERIFIED, VERIFIED, EXPIRED }
}
