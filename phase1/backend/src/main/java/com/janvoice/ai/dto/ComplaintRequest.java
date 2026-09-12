package com.janvoice.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Data Transfer Object containing parameters required to log a new complaint.
 * Standard Java implementation (No Lombok annotation wrappers).
 */
public class ComplaintRequest {

    @NotBlank(message = "Complaint description text is required")
    private String originalText;

    @NotBlank(message = "Ward Area constituency location is required")
    private String wardArea;

    @NotNull(message = "Citizen creator User ID is required")
    private Long citizenId;

    // Default Constructor
    public ComplaintRequest() {
    }

    // Full Constructor
    public ComplaintRequest(String originalText, String wardArea, Long citizenId) {
        this.originalText = originalText;
        this.wardArea = wardArea;
        this.citizenId = citizenId;
    }

    // Getters and Setters
    public String getOriginalText() {
        return originalText;
    }

    public void setOriginalText(String originalText) {
        this.originalText = originalText;
    }

    public String getWardArea() {
        return wardArea;
    }

    public void setWardArea(String wardArea) {
        this.wardArea = wardArea;
    }

    public Long getCitizenId() {
        return citizenId;
    }

    public void setCitizenId(Long citizenId) {
        this.citizenId = citizenId;
    }
}
