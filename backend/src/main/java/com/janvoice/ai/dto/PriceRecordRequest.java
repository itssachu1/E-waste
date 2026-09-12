package com.janvoice.ai.dto;

import com.janvoice.ai.entity.PriceRecord;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PriceRecordRequest {
    @NotNull private Long materialId;
    @NotBlank private String location;
    @NotNull private PriceRecord.BuyerType buyerType;
    private Long buyerId;
    @NotNull @Positive private BigDecimal rate;
    @NotNull private com.janvoice.ai.entity.MaterialUnit unit;
    private String grade;
    private String condition;
    @NotNull private PriceRecord.SourceType sourceType;
    private String sourceReference;
    @NotNull private LocalDateTime quotedAt;
    private LocalDateTime validUntil;

    public Long getMaterialId() { return materialId; } public void setMaterialId(Long v) { materialId = v; }
    public String getLocation() { return location; } public void setLocation(String v) { location = v; }
    public PriceRecord.BuyerType getBuyerType() { return buyerType; } public void setBuyerType(PriceRecord.BuyerType v) { buyerType = v; }
    public Long getBuyerId() { return buyerId; } public void setBuyerId(Long v) { buyerId = v; }
    public BigDecimal getRate() { return rate; } public void setRate(BigDecimal v) { rate = v; }
    public com.janvoice.ai.entity.MaterialUnit getUnit() { return unit; } public void setUnit(com.janvoice.ai.entity.MaterialUnit v) { unit = v; }
    public String getGrade() { return grade; } public void setGrade(String v) { grade = v; }
    public String getCondition() { return condition; } public void setCondition(String v) { condition = v; }
    public PriceRecord.SourceType getSourceType() { return sourceType; } public void setSourceType(PriceRecord.SourceType v) { sourceType = v; }
    public String getSourceReference() { return sourceReference; } public void setSourceReference(String v) { sourceReference = v; }
    public LocalDateTime getQuotedAt() { return quotedAt; } public void setQuotedAt(LocalDateTime v) { quotedAt = v; }
    public LocalDateTime getValidUntil() { return validUntil; } public void setValidUntil(LocalDateTime v) { validUntil = v; }
}
