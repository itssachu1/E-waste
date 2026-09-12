package com.janvoice.ai.service;

import com.janvoice.ai.dto.TransactionRequest;
import com.janvoice.ai.dto.TransactionStatusRequest;
import com.janvoice.ai.entity.User;
import java.util.List;
import java.util.Map;

public interface TransactionService {
    Map<String,Object> create(TransactionRequest request, User actor);
    List<Map<String,Object>> find(User actor, String status, Long lotId);
    Map<String,Object> findById(Long id, User actor);
    Map<String,Object> updateStatus(Long id, TransactionStatusRequest request, User actor);
    Map<String,Object> earnings(User actor);
}
