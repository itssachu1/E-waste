package com.janvoice.ai.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "complaints")
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "citizen_id", nullable = false)
    private User citizen;

    @Column(name = "original_text", nullable = false, columnDefinition = "TEXT")
    private String originalText;

    @Column(name = "translated_text", columnDefinition = "TEXT")
    private String translatedText;

    @Column(length = 15)
    private String language;

    @Column(length = 30)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private Urgency urgency;

    @Enumerated(EnumType.STRING)
    @Column(length = 15, nullable = false)
    private ComplaintStatus status;

    @Column(name = "ward_area", nullable = false, length = 50)
    private String wardArea;

    @Column(nullable = false)
    private Integer upvotes;

    @Column(name = "priority_score")
    private Integer priorityScore;

    @Column(name = "affected_people")
    private Integer affectedPeople;

    @Column(name = "duplicate_count")
    private Integer duplicateCount;

    @Column(name = "suggested_department", length = 100)
    private String suggestedDepartment;

    @Column(name = "priority_reason", columnDefinition = "TEXT")
    private String priorityReason;

    @Column(name = "is_emergency")
    private Boolean isEmergency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_complaint_id")
    private Complaint parentComplaint;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = ComplaintStatus.PENDING;
        }
        if (this.upvotes == null) {
            this.upvotes = 0;
        }
        if (this.priorityScore == null) {
            this.priorityScore = 0;
        }
        if (this.affectedPeople == null) {
            this.affectedPeople = Math.max(1, this.upvotes);
        }
        if (this.duplicateCount == null) {
            this.duplicateCount = 0;
        }
        if (this.isEmergency == null) {
            this.isEmergency = false;
        }
    }

    public Complaint() {
    }

    public Complaint(Long id, User citizen, String originalText, String translatedText, String language,
            String category, Urgency urgency, ComplaintStatus status, String wardArea,
            Integer upvotes, Integer priorityScore, Integer affectedPeople, Integer duplicateCount,
            String suggestedDepartment, String priorityReason, Boolean isEmergency,
            Complaint parentComplaint, LocalDateTime createdAt) {
        this.id = id;
        this.citizen = citizen;
        this.originalText = originalText;
        this.translatedText = translatedText;
        this.language = language;
        this.category = category;
        this.urgency = urgency;
        this.status = status;
        this.wardArea = wardArea;
        this.upvotes = upvotes;
        this.priorityScore = priorityScore;
        this.affectedPeople = affectedPeople;
        this.duplicateCount = duplicateCount;
        this.suggestedDepartment = suggestedDepartment;
        this.priorityReason = priorityReason;
        this.isEmergency = isEmergency;
        this.parentComplaint = parentComplaint;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getCitizen() {
        return citizen;
    }

    public void setCitizen(User citizen) {
        this.citizen = citizen;
    }

    public String getOriginalText() {
        return originalText;
    }

    public void setOriginalText(String originalText) {
        this.originalText = originalText;
    }

    public String getTranslatedText() {
        return translatedText;
    }

    public void setTranslatedText(String translatedText) {
        this.translatedText = translatedText;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Urgency getUrgency() {
        return urgency;
    }

    public void setUrgency(Urgency urgency) {
        this.urgency = urgency;
    }

    public ComplaintStatus getStatus() {
        return status;
    }

    public void setStatus(ComplaintStatus status) {
        this.status = status;
    }

    public String getWardArea() {
        return wardArea;
    }

    public void setWardArea(String wardArea) {
        this.wardArea = wardArea;
    }

    public Integer getUpvotes() {
        return upvotes;
    }

    public void setUpvotes(Integer upvotes) {
        this.upvotes = upvotes;
    }

    public Integer getPriorityScore() {
        return priorityScore;
    }

    public void setPriorityScore(Integer priorityScore) {
        this.priorityScore = priorityScore;
    }

    public Integer getAffectedPeople() {
        return affectedPeople;
    }

    public void setAffectedPeople(Integer affectedPeople) {
        this.affectedPeople = affectedPeople;
    }

    public Integer getDuplicateCount() {
        return duplicateCount;
    }

    public void setDuplicateCount(Integer duplicateCount) {
        this.duplicateCount = duplicateCount;
    }

    public String getSuggestedDepartment() {
        return suggestedDepartment;
    }

    public void setSuggestedDepartment(String suggestedDepartment) {
        this.suggestedDepartment = suggestedDepartment;
    }

    public String getPriorityReason() {
        return priorityReason;
    }

    public void setPriorityReason(String priorityReason) {
        this.priorityReason = priorityReason;
    }

    public Boolean getIsEmergency() {
        return isEmergency;
    }

    public void setIsEmergency(Boolean emergency) {
        isEmergency = emergency;
    }

    public Complaint getParentComplaint() {
        return parentComplaint;
    }

    public void setParentComplaint(Complaint parentComplaint) {
        this.parentComplaint = parentComplaint;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public static ComplaintBuilder builder() {
        return new ComplaintBuilder();
    }

    public static class ComplaintBuilder {
        private Long id;
        private User citizen;
        private String originalText;
        private String translatedText;
        private String language;
        private String category;
        private Urgency urgency;
        private ComplaintStatus status;
        private String wardArea;
        private Integer upvotes;
        private Integer priorityScore;
        private Integer affectedPeople;
        private Integer duplicateCount;
        private String suggestedDepartment;
        private String priorityReason;
        private Boolean isEmergency;
        private Complaint parentComplaint;
        private LocalDateTime createdAt;

        public ComplaintBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public ComplaintBuilder citizen(User citizen) {
            this.citizen = citizen;
            return this;
        }

        public ComplaintBuilder originalText(String originalText) {
            this.originalText = originalText;
            return this;
        }

        public ComplaintBuilder translatedText(String translatedText) {
            this.translatedText = translatedText;
            return this;
        }

        public ComplaintBuilder language(String language) {
            this.language = language;
            return this;
        }

        public ComplaintBuilder category(String category) {
            this.category = category;
            return this;
        }

        public ComplaintBuilder urgency(Urgency urgency) {
            this.urgency = urgency;
            return this;
        }

        public ComplaintBuilder status(ComplaintStatus status) {
            this.status = status;
            return this;
        }

        public ComplaintBuilder wardArea(String wardArea) {
            this.wardArea = wardArea;
            return this;
        }

        public ComplaintBuilder upvotes(Integer upvotes) {
            this.upvotes = upvotes;
            return this;
        }

        public ComplaintBuilder priorityScore(Integer priorityScore) {
            this.priorityScore = priorityScore;
            return this;
        }

        public ComplaintBuilder affectedPeople(Integer affectedPeople) {
            this.affectedPeople = affectedPeople;
            return this;
        }

        public ComplaintBuilder duplicateCount(Integer duplicateCount) {
            this.duplicateCount = duplicateCount;
            return this;
        }

        public ComplaintBuilder suggestedDepartment(String suggestedDepartment) {
            this.suggestedDepartment = suggestedDepartment;
            return this;
        }

        public ComplaintBuilder priorityReason(String priorityReason) {
            this.priorityReason = priorityReason;
            return this;
        }

        public ComplaintBuilder isEmergency(Boolean emergency) {
            this.isEmergency = emergency;
            return this;
        }

        public ComplaintBuilder parentComplaint(Complaint parentComplaint) {
            this.parentComplaint = parentComplaint;
            return this;
        }

        public ComplaintBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Complaint build() {
            return new Complaint(id, citizen, originalText, translatedText, language, category,
                    urgency, status, wardArea, upvotes, priorityScore, affectedPeople, duplicateCount,
                    suggestedDepartment, priorityReason, isEmergency, parentComplaint, createdAt);
        }
    }
}
