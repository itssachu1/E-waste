package com.janvoice.ai.dto;

import com.janvoice.ai.entity.Lot;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * Status-transition payload. {@code finalWeight} is optional and only honoured
 * on the RECYCLER_CONFIRMED transition (Phase 8 handover confirmation).
 */
public class LotStatusRequest {
    @NotNull private Lot.Status status;
    @Positive private BigDecimal finalWeight;
    public Lot.Status getStatus(){return status;} public void setStatus(Lot.Status v){status=v;}
    public BigDecimal getFinalWeight(){return finalWeight;} public void setFinalWeight(BigDecimal v){finalWeight=v;}
}
