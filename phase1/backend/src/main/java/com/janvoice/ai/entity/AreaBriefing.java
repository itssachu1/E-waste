package com.janvoice.ai.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * JPA Entity mapping the 'area_briefings' database table.
 * Standard Java implementation (No Lombok annotation wrappers).
 */
@Entity
@Table(name = "area_briefings")
public class AreaBriefing {

    @Id
    @Column(name = "ward_area", length = 50)
    private String wardArea;

    @Column(name = "ai_summary", nullable = false, columnDefinition = "TEXT")
    private String aiSummary;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Default Constructor
    public AreaBriefing() {
    }

    // Full Constructor
    public AreaBriefing(String wardArea, String aiSummary, LocalDateTime updatedAt) {
        this.wardArea = wardArea;
        this.aiSummary = aiSummary;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public String getWardArea() {
        return wardArea;
    }

    public void setWardArea(String wardArea) {
        this.wardArea = wardArea;
    }

    public String getAiSummary() {
        return aiSummary;
    }

    public void setAiSummary(String aiSummary) {
        this.aiSummary = aiSummary;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Builder
    public static AreaBriefingBuilder builder() {
        return new AreaBriefingBuilder();
    }

    public static class AreaBriefingBuilder {
        private String wardArea;
        private String aiSummary;
        private LocalDateTime updatedAt;

        public AreaBriefingBuilder wardArea(String wardArea) {
            this.wardArea = wardArea;
            return this;
        }

        public AreaBriefingBuilder aiSummary(String aiSummary) {
            this.aiSummary = aiSummary;
            return this;
        }

        public AreaBriefingBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public AreaBriefing build() {
            return new AreaBriefing(wardArea, aiSummary, updatedAt);
        }
    }
}
