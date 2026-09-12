package com.janvoice.ai.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public class LotRequest {
    @NotNull private Long materialId;
    @NotNull @Positive private BigDecimal approximateWeight;
    private String description;
    private String photoUrl;
    private Long selectedPriceRecordId;
    public Long getMaterialId(){return materialId;} public void setMaterialId(Long v){materialId=v;} public BigDecimal getApproximateWeight(){return approximateWeight;} public void setApproximateWeight(BigDecimal v){approximateWeight=v;} public String getDescription(){return description;} public void setDescription(String v){description=v;} public String getPhotoUrl(){return photoUrl;} public void setPhotoUrl(String v){photoUrl=v;} public Long getSelectedPriceRecordId(){return selectedPriceRecordId;} public void setSelectedPriceRecordId(Long v){selectedPriceRecordId=v;}
}
