package com.janvoice.ai.controller;

import com.janvoice.ai.dto.RecyclerDecisionRequest;
import com.janvoice.ai.dto.RecyclerRequest;
import com.janvoice.ai.entity.Recycler;
import com.janvoice.ai.service.RecyclerService;
import com.janvoice.ai.service.SessionTokenService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recyclers")
@CrossOrigin(origins = "*")
public class RecyclerController {
    private final RecyclerService service;
    private final SessionTokenService tokens;
    public RecyclerController(RecyclerService service, SessionTokenService tokens) { this.service = service; this.tokens = tokens; }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAll(@RequestParam(required = false) Long material_id, @RequestParam(required = false) String city, @RequestParam(required = false) Recycler.AuthorizationStatus authorization_status, @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(service.find(material_id, city, authorization_status, active));
    }

    @GetMapping("/match")
    public ResponseEntity<List<Map<String, Object>>> match(@RequestParam Long material_id, @RequestParam(required = false) String location) {
        return ResponseEntity.ok(service.match(material_id, location));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getOne(@PathVariable Long id) { return ResponseEntity.ok(service.findById(id)); }

    /** Return the recycler profile owned by the currently authenticated user. */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return ResponseEntity.ok(service.findByUser(tokens.authenticate(authorization).getId()));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody RecyclerRequest request, @RequestHeader(value = "Authorization", required = false) String authorization) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request, tokens.authenticate(authorization)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id, @Valid @RequestBody RecyclerRequest request, @RequestHeader(value = "Authorization", required = false) String authorization) {
        return ResponseEntity.ok(service.update(id, request, tokens.authenticate(authorization)));
    }

    @PatchMapping("/{id}/verify")
    public ResponseEntity<Map<String, Object>> verify(@PathVariable Long id, @Valid @RequestBody RecyclerDecisionRequest request, @RequestHeader(value = "Authorization", required = false) String authorization) {
        return ResponseEntity.ok(service.verify(id, request, tokens.authenticate(authorization)));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<Map<String, Object>> reject(@PathVariable Long id, @Valid @RequestBody RecyclerDecisionRequest request, @RequestHeader(value = "Authorization", required = false) String authorization) {
        return ResponseEntity.ok(service.reject(id, request, tokens.authenticate(authorization)));
    }
}
