package com.janvoice.ai.dto;

import com.janvoice.ai.entity.Lot;
import jakarta.validation.constraints.NotNull;

public class LotStatusRequest {
    @NotNull private Lot.Status status;
    public Lot.Status getStatus(){return status;} public void setStatus(Lot.Status v){status=v;}
}
