package com.janvoice.ai.service;

import com.janvoice.ai.entity.Complaint;
import com.janvoice.ai.entity.ComplaintStatus;

import java.util.List;
import java.util.Map;

/**
 * Service interface defining operations related to Citizen Grievance
 * Complaints.
 */
public interface ComplaintService {

    /**
     * Creates and records a new citizen complaint.
     * Integrates Gemini API for translation, classification, and duplication
     * checking.
     */
    Complaint createComplaint(String originalText, String wardArea, Long citizenId);

    /**
     * Upvotes a complaint (or its parent master issue if it is linked as a
     * duplicate)
     * and tracks it to prevent citizen double-upvoting.
     */
    Complaint upvoteComplaint(Long complaintId, Long userId);

    /**
     * Retrieves all main (parent) complaints in a ward area sorted by priority.
     */
    List<Complaint> getComplaintsInWardArea(String wardArea);

    /**
     * Updates the status of a specific complaint (for MPs).
     */
    Complaint updateComplaintStatus(Long complaintId, ComplaintStatus newStatus);

    /**
     * Searches active parent complaints in a ward area matching database query
     * keywords.
     */
    List<Complaint> searchComplaints(String wardArea, String query);

    /**
     * Compiles and outputs analytical statistics for the MP dashboard (charts and
     * summaries).
     */
    Map<String, Object> getMpDashboardStats(String wardArea);
}
