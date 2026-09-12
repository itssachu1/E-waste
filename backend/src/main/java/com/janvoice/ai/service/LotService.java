package com.janvoice.ai.service;

import com.janvoice.ai.dto.LotRecyclerRequest;
import com.janvoice.ai.dto.LotRequest;
import com.janvoice.ai.dto.LotStatusRequest;
import com.janvoice.ai.entity.User;
import java.util.List;
import java.util.Map;

public interface LotService {
    Map<String, Object> create(LotRequest request, User collector);
    List<Map<String, Object>> find(User actor, String status);
    Map<String, Object> findById(Long id, User actor);
    Map<String, Object> updateStatus(Long id, LotStatusRequest request, User actor);
    Map<String, Object> assignRecycler(Long id, LotRecyclerRequest request, User actor);
}
