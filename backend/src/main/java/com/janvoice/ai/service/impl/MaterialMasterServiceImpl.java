package com.janvoice.ai.service.impl;

import com.janvoice.ai.entity.MaterialMaster;
import com.janvoice.ai.repository.MaterialMasterRepository;
import com.janvoice.ai.service.MaterialMasterService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class MaterialMasterServiceImpl implements MaterialMasterService {

    private final MaterialMasterRepository repository;

    public MaterialMasterServiceImpl(MaterialMasterRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<MaterialMaster> findActive(String category, String search) {
        if (search != null && !search.isBlank()) {
            return repository.searchActive(search.trim());
        }
        if (category != null && !category.isBlank()) {
            return repository.findByActiveTrueAndCategoryIgnoreCaseOrderByCommonNameAsc(category.trim());
        }
        return repository.findByActiveTrueOrderByCategoryAscCommonNameAsc();
    }

    @Override
    public MaterialMaster findActiveById(Long id) {
        return repository.findById(id)
                .filter(MaterialMaster::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Material not found"));
    }
}
