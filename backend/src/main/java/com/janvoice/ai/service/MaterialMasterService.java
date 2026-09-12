package com.janvoice.ai.service;

import com.janvoice.ai.entity.MaterialMaster;

import java.util.List;

public interface MaterialMasterService {
    List<MaterialMaster> findActive(String category, String search);
    MaterialMaster findActiveById(Long id);
}
