package com.janvoice.ai.controller;

import com.janvoice.ai.dto.ClassificationResponse;
import com.janvoice.ai.entity.MaterialMaster;
import com.janvoice.ai.entity.User;
import com.janvoice.ai.service.GeminiClassificationService;
import com.janvoice.ai.service.MaterialMasterService;
import com.janvoice.ai.service.SessionTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * AI-assisted scan endpoint: POST /api/scan/classify.
 * Authenticated collectors only. The AI only suggests a material;
 * the collector must still confirm it on the Scan page.
 */
@RestController
@RequestMapping("/api/scan")
public class ScanController {

    private static final long MAX_CALLS_PER_MINUTE = 10;

    private final GeminiClassificationService classifier;
    private final MaterialMasterService materials;
    private final SessionTokenService tokens;
    private final ConcurrentHashMap<Long, long[]> usage = new ConcurrentHashMap<>();

    public ScanController(GeminiClassificationService classifier,
                          MaterialMasterService materials,
                          SessionTokenService tokens) {
        this.classifier = classifier;
        this.materials = materials;
        this.tokens = tokens;
    }

    @PostMapping(value = "/classify", consumes = "multipart/form-data")
    public ResponseEntity<Map<String, Object>> classify(
            @RequestParam("image") MultipartFile image,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        User collector = tokens.authenticate(authorization);
        requireCollector(collector);
        checkRateLimit(collector.getId());
        if (image == null || image.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "An image file is required.");
        }
        byte[] bytes;
        try {
            bytes = image.getBytes();
        } catch (java.io.IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read the image.", ex);
        }
        List<MaterialMaster> taxonomy = materials.findActive(null, null);
        ClassificationResponse result = classifier.classify(bytes, image.getContentType(), taxonomy);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("matched", result.isMatched());
        body.put("suggested_material_id", result.getSuggestedMaterialId());
        body.put("suggested_material_name", result.getSuggestedMaterialName());
        return ResponseEntity.ok(body);
    }

    private void requireCollector(User actor) {
        if (actor == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Valid session token required");
        }
        String role = actor.getRole() == null ? "" : actor.getRole();
        if (!"COLLECTOR".equalsIgnoreCase(role) && !"CITIZEN".equalsIgnoreCase(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Collector role required");
        }
    }

    private void checkRateLimit(Long userId) {
        long now = System.currentTimeMillis();
        long[] slot = usage.computeIfAbsent(userId, k -> new long[]{0, 0});
        synchronized (slot) {
            if (now - slot[1] > 60_000) {
                slot[0] = 0;
                slot[1] = now;
            }
            slot[0]++;
            if (slot[0] > MAX_CALLS_PER_MINUTE) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Too many identification requests. Please wait a minute and try again.");
            }
        }
    }
}
