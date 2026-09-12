package com.janvoice.ai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.janvoice.ai.dto.GeminiAnalysisResult;
import com.janvoice.ai.dto.GeminiDuplicateCheckResult;
import com.janvoice.ai.entity.Complaint;
import com.janvoice.ai.entity.Urgency;
import com.janvoice.ai.service.GeminiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service implementation connecting Spring Boot to Google Gemini API
 * using Java's native HttpClient and Jackson's ObjectMapper for JSON
 * serialization.
 */
@Service
public class GeminiServiceImpl implements GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    /**
     * Translates local dialects, classifies category and calculates priority
     * status.
     */
    @Override
    public GeminiAnalysisResult analyzeComplaint(String rawText) {
        String prompt = "You are a professional civic grievance analyzer. " +
                "Analyze the following citizen complaint and return JSON only. " +
                "Rules:\n" +
                "1. Return valid JSON with keys: translatedText, detectedLanguage, category, urgency, summary, suggestedDepartment, priorityReason, isEmergency.\n" +
                "2. Category must be one of: ROAD, WATER, ELECTRICITY, SANITATION, HEALTH, SAFETY, TRANSPORT, EDUCATION, EMERGENCY, OTHER.\n" +
                "3. Urgency must be one of: LOW, MEDIUM, HIGH, CRITICAL. Mark CRITICAL for fire, accident, electric danger, medical emergency, violence, public safety risk, or urgent danger.\n" +
                "4. If many people are affected, mark HIGH or CRITICAL.\n" +
                "5. Return JSON only with no markdown.\n" +
                "Complaint Text: \"" + rawText.replace("\"", "\\\"") + "\"";

        try {
            String rawJson = queryGemini(prompt);
            GeminiAnalysisResult result = objectMapper.readValue(rawJson, GeminiAnalysisResult.class);
            return normalizeAnalysisResult(result, rawText);
        } catch (Exception e) {
            System.err.println("Gemini analysis error: " + e.getMessage());
            return buildFallbackAnalysis(rawText);
        }
    }

    /**
     * Deduplicates incoming grievances by cross-referencing previous reports.
     */
    @Override
    public GeminiDuplicateCheckResult checkForDuplicates(Complaint target, List<Complaint> existingComplaints) {
        if (existingComplaints == null || existingComplaints.isEmpty()) {
            return GeminiDuplicateCheckResult.builder().isDuplicate(false).build();
        }

        // Format active complains list to send to Gemini
        String activeList = existingComplaints.stream()
                .map(c -> String.format("- ID: %d | Content: %s", c.getId(), c.getTranslatedText()))
                .collect(Collectors.joining("\n"));

        String prompt = "You are a duplicate complaint detector. Compare target complaint against active issue logs.\n"
                +
                "Determine if target refers to the SAME underlying civic issue in the area (e.g. same pothole, same garbage pile, same leakage).\n\n"
                +
                "Active Complaints:\n" + activeList + "\n\n" +
                "Target Complaint: \"" + target.getTranslatedText() + "\"\n\n" +
                "Return a strictly formatted JSON response:\n" +
                "{\n" +
                "  \"isDuplicate\": true/false,\n" +
                "  \"matchedComplaintId\": matched_integer_or_null,\n" +
                "  \"explanation\": \"Brief rationale detailing structural match patterns\"\n" +
                "}";

        try {
            String rawJson = queryGemini(prompt);
            return objectMapper.readValue(rawJson, GeminiDuplicateCheckResult.class);
        } catch (Exception e) {
            System.err.println("Gemini duplicate check error: " + e.getMessage());
            return GeminiDuplicateCheckResult.builder().isDuplicate(false).build();
        }
    }


    /**
     * Synthesizes structural complaints list into a high-level briefing.
     */
    @Override
    public String generateAreaBriefing(String wardArea, List<Complaint> activeComplaints) {
        if (activeComplaints == null || activeComplaints.isEmpty()) {
            return "No active grievances logged in Ward Area: " + wardArea;
        }

        String complaintLog = activeComplaints.stream()
                .map(c -> String.format("- [%s] Status: %s | Issue: %s [Upvotes: %d]",
                        c.getCategory(), c.getStatus(), c.getTranslatedText(), c.getUpvotes()))
                .collect(Collectors.joining("\n"));

        String prompt = "Provide a concise executive briefing (max 150 words) for the Member of Parliament (MP) " +
                "summarizing active civic grievances in the constituency ward: " + wardArea + ".\n\n" +
                "Active Grievance Log:\n" + complaintLog + "\n\n" +
                "Compile key urgency factors and dominant categories in a short digest. Do not repeat greeting text. Output only plain informative text.";

        try {
            // A simple plain text return response from Gemini is fine for summary
            return queryGeminiRawText(prompt);
        } catch (Exception e) {
            System.err.println("Gemini briefing error: " + e.getMessage());
            return "Briefing generation unavailable. Active complaint count: " + activeComplaints.size();
        }
    }

    /**
     * Execute Gemini API REST request. Formats request structure and returns the
     * inner text.
     */
    private GeminiAnalysisResult normalizeAnalysisResult(GeminiAnalysisResult result, String rawText) {
        if (result == null) {
            return buildFallbackAnalysis(rawText);
        }
        String normalizedCategory = normalizeCategory(result.getCategory());
        String normalizedUrgency = normalizeUrgency(result.getUrgency());
        String department = inferDepartment(normalizedCategory, normalizedUrgency, rawText);
        String priorityReason = result.getPriorityReason();
        if (priorityReason == null || priorityReason.isBlank()) {
            priorityReason = "AI detected a " + normalizedUrgency.toLowerCase(Locale.ROOT) + " issue in the " + normalizedCategory.toLowerCase(Locale.ROOT) + " domain.";
        }
        boolean isEmergency = Boolean.TRUE.equals(result.getIsEmergency()) || "CRITICAL".equals(normalizedUrgency) || containsEmergencySignals(rawText);
        return GeminiAnalysisResult.builder()
                .language(result.getLanguage() != null ? result.getLanguage() : "en")
                .translatedText(result.getTranslatedText() != null ? result.getTranslatedText() : rawText)
                .category(normalizedCategory)
                .urgency(normalizedUrgency)
                .confidence(result.getConfidence() != null ? result.getConfidence() : 0.8)
                .summary(result.getSummary() != null ? result.getSummary() : result.getTranslatedText())
                .suggestedDepartment(department)
                .priorityReason(priorityReason)
                .isEmergency(isEmergency)
                .build();
    }

    private GeminiAnalysisResult buildFallbackAnalysis(String rawText) {
        String lowered = rawText.toLowerCase(Locale.ROOT);
        boolean emergency = containsEmergencySignals(lowered);
        String category = inferCategory(lowered);
        String urgency = emergency ? "CRITICAL" : inferFallbackUrgency(lowered);
        return GeminiAnalysisResult.builder()
                .language("en")
                .translatedText(rawText)
                .category(category)
                .urgency(urgency)
                .confidence(0.7)
                .summary("Rule-based civic analysis applied because AI service was unavailable.")
                .suggestedDepartment(inferDepartment(category, urgency, lowered))
                .priorityReason(emergency ? "Immediate public safety or danger signal detected." : "Community impact and urgency were inferred from the complaint text.")
                .isEmergency(emergency)
                .build();
    }

    private String inferCategory(String text) {
        if (text.contains("fire") || text.contains("accident") || text.contains("violence") || text.contains("danger") || text.contains("electric")) {
            return "EMERGENCY";
        }
        if (text.contains("water") || text.contains("supply") || text.contains("leak")) {
            return "WATER";
        }
        if (text.contains("road") || text.contains("pothole") || text.contains("street")) {
            return "ROAD";
        }
        if (text.contains("sewer") || text.contains("garbage") || text.contains("sanitation") || text.contains("drain")) {
            return "SANITATION";
        }
        if (text.contains("hospital") || text.contains("doctor") || text.contains("medical") || text.contains("health")) {
            return "HEALTH";
        }
        if (text.contains("school") || text.contains("education") || text.contains("teacher")) {
            return "EDUCATION";
        }
        if (text.contains("bus") || text.contains("transport") || text.contains("traffic")) {
            return "TRANSPORT";
        }
        if (text.contains("safety") || text.contains("unsafe") || text.contains("hazard")) {
            return "SAFETY";
        }
        return "OTHER";
    }

    private String normalizeCategory(String category) {
        if (category == null) {
            return "OTHER";
        }
        String normalized = category.trim().toUpperCase(Locale.ROOT);
        switch (normalized) {
            case "ROADS":
            case "ROAD":
                return "ROAD";
            case "WATER":
            case "WATER SUPPLY":
                return "WATER";
            case "ELECTRICITY":
            case "POWER":
                return "ELECTRICITY";
            case "SANITATION":
            case "SEWAGE":
            case "GARBAGE":
                return "SANITATION";
            case "HEALTHCARE":
            case "HEALTH":
                return "HEALTH";
            case "SAFETY":
            case "CRIME":
            case "PUBLIC SAFETY":
                return "SAFETY";
            case "PUBLIC TRANSPORT":
            case "TRANSPORT":
                return "TRANSPORT";
            case "EDUCATION":
                return "EDUCATION";
            case "EMERGENCY":
            case "FIRE":
                return "EMERGENCY";
            default:
                return "OTHER";
        }
    }

    private String normalizeUrgency(String urgency) {
        if (urgency == null) {
            return "MEDIUM";
        }
        switch (urgency.trim().toUpperCase(Locale.ROOT)) {
            case "CRITICAL":
                return "CRITICAL";
            case "HIGH":
                return "HIGH";
            case "MEDIUM":
                return "MEDIUM";
            case "LOW":
                return "LOW";
            default:
                return "MEDIUM";
        }
    }

    private String inferFallbackUrgency(String text) {
        if (text.contains("fire") || text.contains("accident") || text.contains("danger") || text.contains("violence") || text.contains("medical")) {
            return "CRITICAL";
        }
        if (text.contains("water") || text.contains("road") || text.contains("sewer") || text.contains("electric")) {
            return "HIGH";
        }
        return "MEDIUM";
    }

    private String inferDepartment(String category, String urgency, String text) {
        if ("EMERGENCY".equals(category) || "CRITICAL".equals(urgency)) {
            return "Emergency Response";
        }
        if ("ELECTRICITY".equals(category)) {
            return "Electricity Department";
        }
        if ("WATER".equals(category)) {
            return "Water Department";
        }
        if ("SANITATION".equals(category)) {
            return "Municipal Sanitation";
        }
        if ("ROAD".equals(category)) {
            return "Public Works";
        }
        if ("HEALTH".equals(category)) {
            return "Health Department";
        }
        if ("TRANSPORT".equals(category)) {
            return "Transport Department";
        }
        if ("EDUCATION".equals(category)) {
            return "Education Department";
        }
        if (text.contains("school") || text.contains("hospital")) {
            return "Municipal Coordination";
        }
        return "Municipal Coordination";
    }

    private boolean containsEmergencySignals(String text) {
        String lowered = text.toLowerCase(Locale.ROOT);
        return lowered.contains("fire") || lowered.contains("accident") || lowered.contains("violence") || lowered.contains("medical emergency")
                || lowered.contains("electric danger") || lowered.contains("danger") || lowered.contains("injury") || lowered.contains("collapse");
    }

    private String queryGeminiRawText(String prompt) throws Exception {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException(
                    "Gemini API Key is missing. Configure GEMINI_API_KEY environment variable.");
        }

        // Construct Gemini request schema structure
        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", prompt);

        Map<String, Object> partsObj = new HashMap<>();
        partsObj.put("parts", List.of(textPart));

        Map<String, Object> contentObj = new HashMap<>();
        contentObj.put("contents", List.of(partsObj));

        String requestBodyJson = objectMapper.writeValueAsString(contentObj);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + "?key=" + apiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException(
                    "Gemini HTTP Error. Status: " + response.statusCode() + " | Body: " + response.body());
        }

        JsonNode rootNode = objectMapper.readTree(response.body());
        return rootNode.path("candidates").get(0)
                .path("content").path("parts").get(0)
                .path("text").asText().trim();
    }

    /**
     * Execute Gemini API REST request, enforcing a JSON output configuration
     * schema.
     */
    private String queryGemini(String prompt) throws Exception {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException(
                    "Gemini API Key is missing. Configure GEMINI_API_KEY environment variable.");
        }

        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", prompt);

        Map<String, Object> partsObj = new HashMap<>();
        partsObj.put("parts", List.of(textPart));

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("responseMimeType", "application/json");

        Map<String, Object> requestBodyMap = new HashMap<>();
        requestBodyMap.put("contents", List.of(partsObj));
        requestBodyMap.put("generationConfig", generationConfig);

        String requestBodyJson = objectMapper.writeValueAsString(requestBodyMap);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + "?key=" + apiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException(
                    "Gemini HTTP Error. Status: " + response.statusCode() + " | Body: " + response.body());
        }

        JsonNode rootNode = objectMapper.readTree(response.body());
        return rootNode.path("candidates").get(0)
                .path("content").path("parts").get(0)
                .path("text").asText().trim();
    }
}

