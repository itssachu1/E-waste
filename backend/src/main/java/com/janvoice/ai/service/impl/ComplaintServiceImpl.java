package com.janvoice.ai.service.impl;

import com.janvoice.ai.dto.GeminiAnalysisResult;
import com.janvoice.ai.dto.GeminiDuplicateCheckResult;
import com.janvoice.ai.entity.*;
import com.janvoice.ai.repository.AreaBriefingRepository;
import com.janvoice.ai.repository.ComplaintRepository;
import com.janvoice.ai.repository.UpvoteRepository;
import com.janvoice.ai.repository.UserRepository;
import com.janvoice.ai.service.ComplaintService;
import com.janvoice.ai.service.GeminiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service implementation handling grievance ingestion, duplication merges,
 * upvotes, search capabilities, and compiling admin metrics for MP Dashboard
 * charts.
 */
@Service
public class ComplaintServiceImpl implements ComplaintService {

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UpvoteRepository upvoteRepository;

    @Autowired
    private AreaBriefingRepository areaBriefingRepository;

    @Autowired
    private GeminiService geminiService;

    /**
     * Ingest a new complaint.
     * Uses Gemini to analyze text, and merges the ticket if it matches an active
     * issue.
     */
    @Override
    @Transactional
    public Complaint createComplaint(String originalText, String wardArea, Long citizenId) {
        User citizen = userRepository.findById(citizenId)
                .orElseThrow(() -> new IllegalArgumentException("User with ID " + citizenId + " not found."));

        // Step 1: Use Gemini AI to translate, detect language, categorize, and check
        // urgency
        GeminiAnalysisResult analysis = geminiService.analyzeComplaint(originalText);

        Complaint complaint = Complaint.builder()
                .citizen(citizen)
                .originalText(originalText)
                .translatedText(analysis.getTranslatedText())
                .language(analysis.getLanguage())
                .category(analysis.getCategory())
                .urgency(Urgency.valueOf(analysis.getUrgency()))
                .status(ComplaintStatus.PENDING)
                .wardArea(wardArea)
                .upvotes(1)
                .priorityScore(calculatePriorityScore(analysis, 1, 0, false))
                .affectedPeople(1)
                .duplicateCount(0)
                .suggestedDepartment(analysis.getSuggestedDepartment())
                .priorityReason(analysis.getPriorityReason())
                .isEmergency(Boolean.TRUE.equals(analysis.getIsEmergency()))
                .build();

        // Step 2: Check for potential duplicates in the same Ward Area and Category
        List<Complaint> potentialDuplicates = complaintRepository
                .findByWardAreaAndCategoryAndParentComplaintIsNull(wardArea, analysis.getCategory());

        if (!potentialDuplicates.isEmpty()) {
            GeminiDuplicateCheckResult duplicateCheck = geminiService.checkForDuplicates(complaint,
                    potentialDuplicates);
            if (duplicateCheck.getIsDuplicate() && duplicateCheck.getMatchedComplaintId() != null) {
                // Fetch the parent master complaint
                Optional<Complaint> parentOpt = complaintRepository.findById(duplicateCheck.getMatchedComplaintId());
                if (parentOpt.isPresent()) {
                    Complaint parent = parentOpt.get();
                    complaint.setParentComplaint(parent);
                    complaint.setStatus(parent.getStatus()); // Sync status with master issue

                    // Save the child duplicate record
                    Complaint savedChild = complaintRepository.save(complaint);

                    // Increment upvotes on the parent master ticket to raise its priority
                    parent.setUpvotes(parent.getUpvotes() + 1);
                    parent.setDuplicateCount(parent.getDuplicateCount() + 1);
                    parent.setPriorityScore(calculatePriorityScore(
                            analysis,
                            parent.getUpvotes(),
                            parent.getDuplicateCount(),
                            Boolean.TRUE.equals(parent.getIsEmergency())));
                    complaintRepository.save(parent);

                    // Insert voter junction record for the creator on the parent so they cannot
                    // upvote it again
                    if (!upvoteRepository.existsByUserAndComplaint(citizen, parent)) {
                        upvoteRepository.save(Upvote.builder().user(citizen).complaint(parent).build());
                    }

                    return savedChild;
                }
            }
        }

        // Step 3: If no duplicate is detected, save as a new master ticket
        Complaint savedComplaint = complaintRepository.save(complaint);

        // Record the initial upvote junction record for the creator
        upvoteRepository.save(Upvote.builder().user(citizen).complaint(savedComplaint).build());

        return savedComplaint;
    }

    /**
     * Upvotes a complaint. Prevents double entry.
     * Crucially: if a complaint is a duplicate (has a parent), the upvote is routed
     * directly
     * to the master ticket so it gets aggregate priority weight.
     */
    @Override
    @Transactional
    public Complaint upvoteComplaint(Long complaintId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User with ID " + userId + " not found."));
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new IllegalArgumentException("Complaint with ID " + complaintId + " not found."));

        // If the ticket is a duplicate, route upvote to the parent master ticket
        Complaint targetComplaint = complaint.getParentComplaint() != null ? complaint.getParentComplaint() : complaint;

        // Check duplicate votes
        if (upvoteRepository.existsByUserAndComplaint(user, targetComplaint)) {
            return targetComplaint; // Just return, no exception needed for a hackathon
        }

        // Create vote record
        upvoteRepository.save(Upvote.builder().user(user).complaint(targetComplaint).build());

        targetComplaint.setUpvotes(targetComplaint.getUpvotes() + 1);
        targetComplaint.setAffectedPeople(Math.max(targetComplaint.getAffectedPeople(), targetComplaint.getUpvotes()));
        targetComplaint.setPriorityScore(calculatePriorityScore(
                null,
                targetComplaint.getUpvotes(),
                targetComplaint.getDuplicateCount(),
                Boolean.TRUE.equals(targetComplaint.getIsEmergency())));
        return complaintRepository.save(targetComplaint);
    }

    @Override
    public List<Complaint> getComplaintsInWardArea(String wardArea) {
        List<Complaint> complaints = complaintRepository.findByWardAreaAndParentComplaintIsNullOrderByPriorityScoreDesc(wardArea);
        complaints.sort(Comparator.comparing(Complaint::getPriorityScore, Comparator.nullsLast(Integer::compareTo)).reversed());
        return complaints;
    }

    @Override
    @Transactional
    public Complaint updateComplaintStatus(Long complaintId, ComplaintStatus newStatus) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new IllegalArgumentException("Complaint with ID " + complaintId + " not found."));

        // Update target
        complaint.setStatus(newStatus);
        Complaint saved = complaintRepository.save(complaint);

        // Also sync status for all its duplicates (child complaints)
        List<Complaint> children = complaintRepository.findByParentComplaintId(complaintId);
        for (Complaint child : children) {
            child.setStatus(newStatus);
            complaintRepository.save(child);
        }

        return saved;
    }

    @Override
    public List<Complaint> searchComplaints(String wardArea, String query) {
        return complaintRepository.searchComplaints(wardArea, query);
    }

    /**
     * Compiles charts counts, aggregates, and dynamically updates the cached AI
     * executive briefing.
     */
    @Override
    @Transactional
    public Map<String, Object> getMpDashboardStats(String wardArea) {
        Map<String, Object> stats = new HashMap<>();

        // 1. Status Aggregates
        long pending = complaintRepository.countByWardAreaAndStatus(wardArea, ComplaintStatus.PENDING);
        long inProgress = complaintRepository.countByWardAreaAndStatus(wardArea, ComplaintStatus.IN_PROGRESS);
        long resolved = complaintRepository.countByWardAreaAndStatus(wardArea, ComplaintStatus.RESOLVED);

        stats.put("pendingCount", pending);
        stats.put("inProgressCount", inProgress);
        stats.put("resolvedCount", resolved);

        // 2. Category Distribution
        List<Object[]> categoryCounts = complaintRepository.countByCategoryInWardArea(wardArea);
        Map<String, Long> categoriesMap = new HashMap<>();
        for (Object[] row : categoryCounts) {
            categoriesMap.put((String) row[0], (Long) row[1]);
        }
        stats.put("categories", categoriesMap);

        // 3. AI Briefing Handling
        List<Complaint> activeComplaints = complaintRepository
                .findByWardAreaAndParentComplaintIsNullOrderByPriorityScoreDesc(wardArea);
        activeComplaints.sort(Comparator.comparing(Complaint::getPriorityScore, Comparator.nullsLast(Integer::compareTo)).reversed());

        Optional<AreaBriefing> briefingOpt = areaBriefingRepository.findById(wardArea);
        String briefingText;

        // Cache brief checks: Generate new one if cache doesn't exist or is older than
        // 5 minutes
        if (briefingOpt.isEmpty() || briefingOpt.get().getUpdatedAt().isBefore(LocalDateTime.now().minusMinutes(5))) {
            briefingText = geminiService.generateAreaBriefing(wardArea, activeComplaints);
            AreaBriefing briefingEntity = AreaBriefing.builder()
                    .wardArea(wardArea)
                    .aiSummary(briefingText)
                    .build();
            areaBriefingRepository.save(briefingEntity);
        } else {
            briefingText = briefingOpt.get().getAiSummary();
        }

        stats.put("aiSummary", briefingText);
        return stats;
    }

    private int calculatePriorityScore(GeminiAnalysisResult analysis, int upvotes, int duplicateCount, boolean isEmergency) {
        int score = 0;
        if (analysis != null) {
            switch (analysis.getUrgency()) {
                case "CRITICAL":
                    score += 50;
                    break;
                case "HIGH":
                    score += 35;
                    break;
                case "MEDIUM":
                    score += 20;
                    break;
                default:
                    score += 10;
                    break;
            }
            if (Boolean.TRUE.equals(analysis.getIsEmergency())) {
                score += 30;
            }
            if (analysis.getCategory() != null && (analysis.getCategory().contains("SAFETY") || analysis.getCategory().contains("EMERGENCY"))) {
                score += 20;
            }
        }
        score += upvotes * 5;
        score += duplicateCount * 8;
        if (isEmergency) {
            score += 30;
        }
        return score;
    }
}
