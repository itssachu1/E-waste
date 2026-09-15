package com.janvoice.ai.controller;

import com.janvoice.ai.dto.LotRecyclerRequest;
import com.janvoice.ai.dto.LotRequest;
import com.janvoice.ai.dto.LotStatusRequest;
import com.janvoice.ai.service.LotService;
import com.janvoice.ai.service.SessionTokenService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lots")
public class LotController {
    private final LotService service;
    private final SessionTokenService tokens;
    public LotController(LotService service, SessionTokenService tokens) { this.service=service; this.tokens=tokens; }
    @PostMapping public ResponseEntity<Map<String,Object>> create(@Valid @RequestBody LotRequest request, @RequestHeader(value="Authorization", required=false) String authorization) { return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request, tokens.authenticate(authorization))); }
    @GetMapping public ResponseEntity<List<Map<String,Object>>> getAll(@RequestParam(required=false) String status, @RequestHeader(value="Authorization", required=false) String authorization) { return ResponseEntity.ok(service.find(tokens.authenticate(authorization), status)); }
    @GetMapping("/{id}") public ResponseEntity<Map<String,Object>> getOne(@PathVariable Long id, @RequestHeader(value="Authorization", required=false) String authorization) { return ResponseEntity.ok(service.findById(id, tokens.authenticate(authorization))); }
    @PatchMapping("/{id}/status") public ResponseEntity<Map<String,Object>> updateStatus(@PathVariable Long id, @Valid @RequestBody LotStatusRequest request, @RequestHeader(value="Authorization", required=false) String authorization) { return ResponseEntity.ok(service.updateStatus(id, request, tokens.authenticate(authorization))); }
    @PatchMapping("/{id}/recycler") public ResponseEntity<Map<String,Object>> assignRecycler(@PathVariable Long id, @Valid @RequestBody LotRecyclerRequest request, @RequestHeader(value="Authorization", required=false) String authorization) { return ResponseEntity.ok(service.assignRecycler(id, request, tokens.authenticate(authorization))); }
}
