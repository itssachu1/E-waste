package com.janvoice.ai.controller;

import com.janvoice.ai.dto.TransactionRequest;
import com.janvoice.ai.dto.TransactionStatusRequest;
import com.janvoice.ai.service.SessionTokenService;
import com.janvoice.ai.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class TransactionController {
    private final TransactionService service;
    private final SessionTokenService tokens;
    public TransactionController(TransactionService service, SessionTokenService tokens){this.service=service;this.tokens=tokens;}
    @PostMapping("/transactions") public ResponseEntity<Map<String,Object>> create(@Valid @RequestBody TransactionRequest request,@RequestHeader(value="Authorization",required=false) String authorization){return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request,tokens.authenticate(authorization)));}
    @GetMapping("/transactions") public ResponseEntity<List<Map<String,Object>>> getAll(@RequestParam(required=false) String payment_status,@RequestParam(required=false) Long lot_id,@RequestHeader(value="Authorization",required=false) String authorization){return ResponseEntity.ok(service.find(tokens.authenticate(authorization),payment_status,lot_id));}
    @GetMapping("/transactions/{id}") public ResponseEntity<Map<String,Object>> getOne(@PathVariable Long id,@RequestHeader(value="Authorization",required=false) String authorization){return ResponseEntity.ok(service.findById(id,tokens.authenticate(authorization)));}
    @PatchMapping("/transactions/{id}/status") public ResponseEntity<Map<String,Object>> updateStatus(@PathVariable Long id,@Valid @RequestBody TransactionStatusRequest request,@RequestHeader(value="Authorization",required=false) String authorization){return ResponseEntity.ok(service.updateStatus(id,request,tokens.authenticate(authorization)));}
    @GetMapping("/earnings") public ResponseEntity<Map<String,Object>> earnings(@RequestHeader(value="Authorization",required=false) String authorization){return ResponseEntity.ok(service.earnings(tokens.authenticate(authorization)));}
}
