package com.janvoice.ai.dto;

import jakarta.validation.constraints.NotNull;

public class LotRecyclerRequest {
    @NotNull private Long recyclerId;
    public Long getRecyclerId(){return recyclerId;} public void setRecyclerId(Long v){recyclerId=v;}
}
