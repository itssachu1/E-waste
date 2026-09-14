package com.janvoice.ai.dto;

public class ClassificationResponse {

    private Long suggestedMaterialId;

    private String suggestedMaterialName;

    private boolean matched;

    public ClassificationResponse() {}

    public ClassificationResponse(Long suggestedMaterialId, String suggestedMaterialName, boolean matched) {
        this.suggestedMaterialId = suggestedMaterialId;
        this.suggestedMaterialName = suggestedMaterialName;
        this.matched = matched;
    }

    public static ClassificationResponse matched(Long id, String name) {
        return new ClassificationResponse(id, name, true);
    }

    public static ClassificationResponse unmatched() {
        return new ClassificationResponse(null, null, false);
    }

    public Long getSuggestedMaterialId() {
        return suggestedMaterialId;
    }

    public void setSuggestedMaterialId(Long suggestedMaterialId) {
        this.suggestedMaterialId = suggestedMaterialId;
    }

    public String getSuggestedMaterialName() {
        return suggestedMaterialName;
    }

    public void setSuggestedMaterialName(String suggestedMaterialName) {
        this.suggestedMaterialName = suggestedMaterialName;
    }

    public boolean isMatched() {
        return matched;
    }

    public void setMatched(boolean matched) {
        this.matched = matched;
    }
}
