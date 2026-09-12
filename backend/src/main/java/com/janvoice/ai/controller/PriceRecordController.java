package com.janvoice.ai.controller;

import com.janvoice.ai.dto.PriceRecordRequest;
import com.janvoice.ai.service.PriceRecordService;
import com.janvoice.ai.service.SessionTokenService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/prices")
@CrossOrigin(origins = "*")
public class PriceRecordController {
    private final PriceRecordService service;
    private final SessionTokenService sessionTokenService;

    public PriceRecordController(PriceRecordService service, SessionTokenService sessionTokenService) {
        this.service = service;
        this.sessionTokenService = sessionTokenService;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getPrices(
            @RequestParam(required = false) Long material_id,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String verification_status,
            @RequestParam(defaultValue = "false") boolean includeExpired) {
        return ResponseEntity.ok(service.find(material_id, location, verification_status, includeExpired));
    }

    @GetMapping("/history")
    public ResponseEntity<List<Map<String, Object>>> getHistory(
            @RequestParam(required = false) Long material_id,
            @RequestParam(required = false) String location) {
        return ResponseEntity.ok(service.history(material_id, location));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody PriceRecordRequest request,
                                                       @RequestHeader(value = "Authorization", required = false) String authorization) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request, sessionTokenService.authenticate(authorization)));
    }

    @PatchMapping("/{id}/verify")
    public ResponseEntity<Map<String, Object>> verify(@PathVariable Long id,
                                                       @RequestHeader(value = "Authorization", required = false) String authorization) {
        return ResponseEntity.ok(service.verify(id, sessionTokenService.authenticate(authorization)));
    }
}
