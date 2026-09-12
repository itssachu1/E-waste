package com.janvoice.ai.service;

import com.janvoice.ai.dto.GeminiAnalysisResult;
import com.janvoice.ai.dto.GeminiDuplicateCheckResult;
import com.janvoice.ai.entity.Complaint;
import java.util.List;

/**
 * Service interface outlining artificial intelligence operations
 * powered by Google Gemini API.
 */
public interface GeminiService {

    /**
     * Translates, detects language, categorizes, and determines urgency
     * of a single complaint text.
     */
    GeminiAnalysisResult analyzeComplaint(String rawText);

    /**
     * Checks if a new incoming complaint is a duplicate of any existing local
     * complaints.
     */
    GeminiDuplicateCheckResult checkForDuplicates(Complaint target, List<Complaint> existingComplaints);

    /**
     * Generates a structural summary briefing of all complaints in a ward area.
     */
    String generateAreaBriefing(String wardArea, List<Complaint> activeComplaints);
}
