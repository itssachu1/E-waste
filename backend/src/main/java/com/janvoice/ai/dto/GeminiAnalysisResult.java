package com.janvoice.ai.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GeminiAnalysisResult {
    @JsonAlias({"detectedLanguage", "language"})
    private String language;
    private String translatedText;
    private String category;
    private String urgency;
    private Double confidence;
    private String summary;
    private String suggestedDepartment;
    private String priorityReason;
    private Boolean isEmergency;

    public GeminiAnalysisResult() {
    }

    public GeminiAnalysisResult(String language, String translatedText, String category, String urgency,
            Double confidence, String summary, String suggestedDepartment, String priorityReason,
            Boolean isEmergency) {
        this.language = language;
        this.translatedText = translatedText;
        this.category = category;
        this.urgency = urgency;
        this.confidence = confidence;
        this.summary = summary;
        this.suggestedDepartment = suggestedDepartment;
        this.priorityReason = priorityReason;
        this.isEmergency = isEmergency;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getDetectedLanguage() {
        return language;
    }

    public void setDetectedLanguage(String detectedLanguage) {
        this.language = detectedLanguage;
    }

    public String getTranslatedText() {
        return translatedText;
    }

    public void setTranslatedText(String translatedText) {
        this.translatedText = translatedText;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getUrgency() {
        return urgency;
    }

    public void setUrgency(String urgency) {
        this.urgency = urgency;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
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

    public static GeminiAnalysisResultBuilder builder() {
        return new GeminiAnalysisResultBuilder();
    }

    public static class GeminiAnalysisResultBuilder {
        private String language;
        private String translatedText;
        private String category;
        private String urgency;
        private Double confidence;
        private String summary;
        private String suggestedDepartment;
        private String priorityReason;
        private Boolean isEmergency;

        public GeminiAnalysisResultBuilder language(String language) {
            this.language = language;
            return this;
        }

        public GeminiAnalysisResultBuilder translatedText(String translatedText) {
            this.translatedText = translatedText;
            return this;
        }

        public GeminiAnalysisResultBuilder category(String category) {
            this.category = category;
            return this;
        }

        public GeminiAnalysisResultBuilder urgency(String urgency) {
            this.urgency = urgency;
            return this;
        }

        public GeminiAnalysisResultBuilder confidence(Double confidence) {
            this.confidence = confidence;
            return this;
        }

        public GeminiAnalysisResultBuilder summary(String summary) {
            this.summary = summary;
            return this;
        }

        public GeminiAnalysisResultBuilder suggestedDepartment(String suggestedDepartment) {
            this.suggestedDepartment = suggestedDepartment;
            return this;
        }

        public GeminiAnalysisResultBuilder priorityReason(String priorityReason) {
            this.priorityReason = priorityReason;
            return this;
        }

        public GeminiAnalysisResultBuilder isEmergency(Boolean isEmergency) {
            this.isEmergency = isEmergency;
            return this;
        }

        public GeminiAnalysisResult build() {
            return new GeminiAnalysisResult(language, translatedText, category, urgency, confidence,
                    summary, suggestedDepartment, priorityReason, isEmergency);
        }
    }
}
