package com.janvoice.ai.service;

import com.janvoice.ai.dto.RecyclerDecisionRequest;
import com.janvoice.ai.dto.RecyclerRequest;
import com.janvoice.ai.entity.Recycler;
import com.janvoice.ai.entity.User;
import java.util.List;
import java.util.Map;

public interface RecyclerService {
    List<Map<String, Object>> find(Long materialId, String city, Recycler.AuthorizationStatus status, Boolean active);
    Map<String, Object> findById(Long id);
    List<Map<String, Object>> match(Long materialId, String location);
    Map<String, Object> create(RecyclerRequest request, User actor);
    Map<String, Object> update(Long id, RecyclerRequest request, User actor);
    Map<String, Object> verify(Long id, RecyclerDecisionRequest request, User actor);
    Map<String, Object> reject(Long id, RecyclerDecisionRequest request, User actor);
}
