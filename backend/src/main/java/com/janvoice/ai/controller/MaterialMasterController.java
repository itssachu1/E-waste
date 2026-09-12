package com.janvoice.ai.controller;

import com.janvoice.ai.entity.MaterialMaster;
import com.janvoice.ai.service.MaterialMasterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/materials")
@CrossOrigin(origins = "*")
public class MaterialMasterController {

    private final MaterialMasterService materialMasterService;

    public MaterialMasterController(MaterialMasterService materialMasterService) {
        this.materialMasterService = materialMasterService;
    }

    @GetMapping
    public ResponseEntity<List<MaterialMaster>> getMaterials(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(materialMasterService.findActive(category, search));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MaterialMaster> getMaterial(@PathVariable Long id) {
        return ResponseEntity.ok(materialMasterService.findActiveById(id));
    }
}
