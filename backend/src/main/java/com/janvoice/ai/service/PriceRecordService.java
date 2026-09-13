package com.janvoice.ai.service;

import com.janvoice.ai.dto.PriceRecordRequest;
import com.janvoice.ai.entity.User;
import java.util.List;
import java.util.Map;

public interface PriceRecordService {
    List<Map<String, Object>> find(Long materialId, String location, String verificationStatus, boolean includeExpired);
    List<Map<String, Object>> history(Long materialId, String location);
    Map<String, Object> create(PriceRecordRequest request, User actor);
    Map<String, Object> verify(Long id, User actor);
}
