package com.janvoice.ai.service;

import com.janvoice.ai.dto.ClassificationResponse;
import com.janvoice.ai.entity.MaterialMaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Real AI material classification backed by Google Gemini Vision API.
 *
 * <p>Contract (Phase 13): the AI only ever suggests. The collector
 * confirms or overrides on the Scan page. If the AI answer does not match a
 * real active Material Master entry, matched is false and no suggestion is
 * returned. No confidence percentage is ever fabricated.
 */
@Service
public class GeminiClassificationServiceImpl implements GeminiClassificationService {

    private static final Logger log = LoggerFactory.getLogger(GeminiClassificationServiceImpl.class);

    static final long DEFAULT_MAX_IMAGE_BYTES = 10L * 1024 * 1024;

    private static final List<String> ALLOWED_TYPES = List.of(
            "image/jpeg", "image/png", "image/webp", "image/heic", "image/heif");

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String model;
    private final long maxImageBytes;

    public GeminiClassificationServiceImpl(
            RestTemplate restTemplate,
            @Value("${gemini.api.key:}") String apiKey,
            @Value("${gemini.classification.model:gemini-2.0-flash}") String model,
            @Value("${gemini.classification.max-image-bytes:10485760}") long maxImageBytes) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = (model == null || model.isBlank()) ? "gemini-2.0-flash" : model.trim();
        this.maxImageBytes = maxImageBytes > 0 ? maxImageBytes : DEFAULT_MAX_IMAGE_BYTES;
    }


    @Override
    public ClassificationResponse classify(byte[] image, String contentType, List<MaterialMaster> materials) {
        validateUpload(image, contentType);
        requireApiKey();
        if (materials == null || materials.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Material taxonomy is unavailable. Please select the material manually.");
        }
        List<String> taxonomy = new ArrayList<>();
        for (MaterialMaster m : materials) {
            if (m.isActive() && m.getCommonName() != null) taxonomy.add(m.getCommonName());
        }
        if (taxonomy.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Material taxonomy is unavailable. Please select the material manually.");
        }
        String prompt = buildPrompt(taxonomy);
        String base64 = Base64.getEncoder().encodeToString(image);
        Map<String, Object> imagePart = Map.of("inline_data",
                Map.of("mime_type", contentType, "data", base64));
        Map<String, Object> textPart = Map.of("text", prompt);
        Map<String, Object> body = Map.of("contents",
                List.of(Map.of("parts", List.of(textPart, imagePart))));
        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + model + ":generateContent?key=" + apiKey;
        Map<?, ?> gemini;
        try {
            gemini = restTemplate.postForObject(url, body, Map.class);
        } catch (RestClientException ex) {
            log.warn("Gemini request failed: {}", ex.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Identification is temporarily unavailable. Please select the material manually.", ex);
        }
        String answer = extractText(gemini);
        if (answer == null || answer.isBlank()) {
            log.warn("Gemini returned an empty answer");
            return ClassificationResponse.unmatched();
        }
        MaterialMaster best = ClassificationMatcher.bestMatch(answer, materials);
        if (best == null) {
            String shown = answer.length() > 120 ? answer.substring(0, 120) + "..." : answer;
            log.info("Gemini answer did not match taxonomy: '{}'", shown);
            return ClassificationResponse.unmatched();
        }
        log.info("Gemini suggested '{}' (id={})", best.getCommonName(), best.getId());
        return ClassificationResponse.matched(best.getId(), best.getCommonName());
    }

    private void validateUpload(byte[] image, String contentType) {
        if (image == null || image.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "An image file is required.");
        }
        if (image.length > maxImageBytes) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "Image is too large (max " + (maxImageBytes / 1024 / 1024) + " MB).");
        }
        if (contentType == null || ALLOWED_TYPES.stream().noneMatch(contentType::equalsIgnoreCase)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Only JPG, PNG, WEBP or HEIC photos are accepted.");
        }
    }

    private void requireApiKey() {
        if (apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Automatic identification is not configured. Please select the material manually.");
        }
    }

    private String buildPrompt(List<String> taxonomy) {
        StringBuilder names = new StringBuilder();
        for (int i = 0; i < taxonomy.size(); i++) {
            if (i > 0) names.append("; ");
            names.append('"').append(taxonomy.get(i)).append('"');
        }
        return "You are identifying an e-waste item from a photo taken by a waste collector in India. "
                + "Reply with exactly one line: the single best-matching material name chosen verbatim "
                + "from this list: " + names + ". "
                + "If the photo clearly does not show electronic or electrical waste, or you cannot "
                + "confidently match any entry, reply with exactly: NO_MATCH. "
                + "Output nothing else. No explanation, no confidence score.";
    }

    @SuppressWarnings("unchecked")
    static String extractText(Map<?, ?> response) {
        if (response == null) return null;
        try {
            List<Map<String, Object>> cands = (List<Map<String, Object>>) response.get("candidates");
            if (cands == null || cands.isEmpty()) return null;
            Map<String, Object> content = (Map<String, Object>) cands.get(0).get("content");
            if (content == null) return null;
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            if (parts == null || parts.isEmpty()) return null;
            Object text = parts.get(0).get("text");
            return text == null ? null : text.toString().trim();
        } catch (ClassCastException | NullPointerException ex) {
            log.warn("Could not parse Gemini response: {}", ex.getMessage());
            return null;
        }
    }
}

