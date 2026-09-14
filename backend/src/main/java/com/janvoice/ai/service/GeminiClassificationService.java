package com.janvoice.ai.service;

import com.janvoice.ai.dto.ClassificationResponse;
import com.janvoice.ai.entity.MaterialMaster;

import java.util.List;

/**
 * Service that calls Google Gemini Vision API to suggest a material
 * classification for an uploaded e-waste photo.
 *
 * <p>The AI only suggests — it never auto-accepts. The frontend must
 * let the collector confirm or override the suggestion. If the AI's
 * answer doesn't match a real Material Master entry, {@code matched}
 * is {@code false} and no suggestion is returned.
 */
public interface GeminiClassificationService {

    /**
     * Sends the image to Gemini Vision API and returns a suggestion
     * matched against the active Material Master taxonomy.
     *
     * @param image  the uploaded photo bytes
     * @param contentType MIME type of the uploaded photo
     * @param allMaterials  the full list of active Material Master entries
     *                      (fetched from DB so the AI prompt stays in sync
     *                      with the real taxonomy)
     * @return a ClassificationResponse; matched=false if no confident match
     * @throws IllegalArgumentException if the API key is missing
     */
    ClassificationResponse classify(byte[] image, String contentType, List<MaterialMaster> allMaterials);
}
