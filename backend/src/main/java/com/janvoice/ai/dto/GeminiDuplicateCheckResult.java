package com.janvoice.ai.dto;

/**
 * Data Transfer Object representing the output of a Gemini check
 * comparing a new complaint with existing complaints in the same category.
 * Standard Java implementation (No Lombok annotation wrappers).
 */
public class GeminiDuplicateCheckResult {
    private Boolean isDuplicate;
    private Long matchedComplaintId;
    private String explanation;

    // Default Constructor
    public GeminiDuplicateCheckResult() {
    }

    // Full constructor
    public GeminiDuplicateCheckResult(Boolean isDuplicate, Long matchedComplaintId, String explanation) {
        this.isDuplicate = isDuplicate;
        this.matchedComplaintId = matchedComplaintId;
        this.explanation = explanation;
    }

    // Getters and Setters
    public Boolean getIsDuplicate() {
        return isDuplicate;
    }

    public void setIsDuplicate(Boolean duplicate) {
        isDuplicate = duplicate;
    }

    public Long getMatchedComplaintId() {
        return matchedComplaintId;
    }

    public void setMatchedComplaintId(Long matchedComplaintId) {
        this.matchedComplaintId = matchedComplaintId;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    // Builder
    public static GeminiDuplicateCheckResultBuilder builder() {
        return new GeminiDuplicateCheckResultBuilder();
    }

    public static class GeminiDuplicateCheckResultBuilder {
        private Boolean isDuplicate;
        private Long matchedComplaintId;
        private String explanation;

        public GeminiDuplicateCheckResultBuilder isDuplicate(Boolean isDuplicate) {
            this.isDuplicate = isDuplicate;
            return this;
        }

        public GeminiDuplicateCheckResultBuilder matchedComplaintId(Long matchedComplaintId) {
            this.matchedComplaintId = matchedComplaintId;
            return this;
        }

        public GeminiDuplicateCheckResultBuilder explanation(String explanation) {
            this.explanation = explanation;
            return this;
        }

        public GeminiDuplicateCheckResult build() {
            return new GeminiDuplicateCheckResult(isDuplicate, matchedComplaintId, explanation);
        }
    }
}
