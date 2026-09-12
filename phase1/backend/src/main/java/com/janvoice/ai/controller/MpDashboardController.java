package com.janvoice.ai.controller;

import com.janvoice.ai.entity.Complaint;
import com.janvoice.ai.entity.ComplaintStatus;
import com.janvoice.ai.service.ComplaintService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for MP dashboard actions.
 * Exposes category metrics, status counters, cached AI briefs, and ticket
 * status updating.
 */
@RestController
@RequestMapping("/api/mp")
@CrossOrigin(origins = "*")
public class MpDashboardController {

    @Autowired
    private ComplaintService complaintService;

    /**
     * Retrieve aggregated statistics and Gemini summaries for a ward area.
     */
    @GetMapping("/dashboard/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats(@RequestParam String wardArea) {
        Map<String, Object> stats = complaintService.getMpDashboardStats(wardArea);
        return ResponseEntity.ok(stats);
    }

    /**
     * Transition a complaint's status (PENDING -> IN_PROGRESS -> RESOLVED).
     */
    @PutMapping("/complaints/{id}/status")
    public ResponseEntity<Complaint> updateComplaintStatus(
            @PathVariable Long id,
            @RequestParam ComplaintStatus status) {
        Complaint updated = complaintService.updateComplaintStatus(id, status);
        return ResponseEntity.ok(updated);
    }
}
