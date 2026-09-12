package com.janvoice.ai.controller;

import com.janvoice.ai.dto.ComplaintRequest;
import com.janvoice.ai.entity.Complaint;
import com.janvoice.ai.service.ComplaintService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for citizen complaint logging, browsing, search, and upvoting
 * interactions.
 */
@RestController
@RequestMapping("/api/complaints")
@CrossOrigin(origins = "*")
public class ComplaintController {

    @Autowired
    private ComplaintService complaintService;

    /**
     * Submit a new citizen complaint.
     */
    @PostMapping
    public ResponseEntity<Complaint> submitComplaint(@Valid @RequestBody ComplaintRequest request) {
        Complaint saved = complaintService.createComplaint(
                request.getOriginalText(),
                request.getWardArea(),
                request.getCitizenId());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * Retrieve prioritised complaints for a constituency area.
     */
    @GetMapping
    public ResponseEntity<List<Complaint>> getComplaints(@RequestParam String wardArea) {
        List<Complaint> list = complaintService.getComplaintsInWardArea(wardArea);
        return ResponseEntity.ok(list);
    }

    /**
     * Search complaints matching query.
     */
    @GetMapping("/search")
    public ResponseEntity<List<Complaint>> searchComplaints(
            @RequestParam String wardArea,
            @RequestParam String query) {
        List<Complaint> list = complaintService.searchComplaints(wardArea, query);
        return ResponseEntity.ok(list);
    }

    /**
     * Increment vote supporting a specific issue.
     */
    @PostMapping("/{id}/upvote")
    public ResponseEntity<Complaint> upvoteComplaint(
            @PathVariable Long id,
            @RequestParam Long userId) {
        Complaint updated = complaintService.upvoteComplaint(id, userId);
        return ResponseEntity.ok(updated);
    }
}
