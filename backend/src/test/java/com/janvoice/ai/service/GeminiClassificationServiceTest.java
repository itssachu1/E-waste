package com.janvoice.ai.service;

import com.janvoice.ai.dto.ClassificationResponse;
import com.janvoice.ai.entity.MaterialMaster;
import com.janvoice.ai.entity.MaterialUnit;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Unit tests for the AI-suggests/collector-confirms classification contract. */
class GeminiClassificationServiceTest {

    private MaterialMaster material(Long id, String commonName, String deviceType) {
        MaterialMaster m = new MaterialMaster();
        m.setId(id);
        m.setCommonName(commonName);
        m.setDeviceType(deviceType);
        m.setCategory("IT");
        m.setSubcategory("Test");
        m.setTypicalUnit(MaterialUnit.KG);
        m.setActive(true);
        return m;
    }

    private GeminiClassificationServiceImpl service(RestTemplate http, String key) {
        return new GeminiClassificationServiceImpl(http, key, "gemini-2.0-flash", 10 * 1024 * 1024);
    }

    private Map<String, Object> geminiAnswer(String text) {
        return Map.of("candidates", List.of(
                Map.of("content", Map.of("parts", List.of(Map.of("text", text))))));
    }

    @Test
    void realCallShapeMatchedToTaxonomy() {
        FakeGeminiHttp http = new FakeGeminiHttp(geminiAnswer("Mobile Phone"));
        GeminiClassificationServiceImpl svc = service(http, "test-key");
        List<MaterialMaster> taxonomy = List.of(
                material(1L, "Mobile Phone", "Smartphone"),
                material(2L, "Laptop", "Laptop"));
        ClassificationResponse out = svc.classify(new byte[]{1, 2, 3}, "image/jpeg", taxonomy);
        assertTrue(out.isMatched());
        assertEquals(1L, out.getSuggestedMaterialId());
        assertEquals("Mobile Phone", out.getSuggestedMaterialName());
        assertEquals(1, http.calls);
        assertTrue(http.lastUrl.startsWith("https://generativelanguage.googleapis.com/"));
    }

    @Test
    void unrelatedPhotoReturnsUnmatched() {
        FakeGeminiHttp http = new FakeGeminiHttp(geminiAnswer("NO_MATCH"));
        GeminiClassificationServiceImpl svc = service(http, "test-key");
        List<MaterialMaster> taxonomy = List.of(material(1L, "Mobile Phone", "Smartphone"));
        ClassificationResponse out = svc.classify(new byte[]{1}, "image/png", taxonomy);
        assertFalse(out.isMatched());
        assertNull(out.getSuggestedMaterialId());
        assertNull(out.getSuggestedMaterialName());
    }

    @Test
    void gibberishAnswerNeverForcesAGuess() {
        FakeGeminiHttp http = new FakeGeminiHttp(geminiAnswer("a banana on a bicycle"));
        GeminiClassificationServiceImpl svc = service(http, "test-key");
        List<MaterialMaster> taxonomy = List.of(material(1L, "Mobile Phone", "Smartphone"));
        assertFalse(svc.classify(new byte[]{1}, "image/png", taxonomy).isMatched());
    }

    @Test
    void missingApiKeyFailsGracefullyWithoutHttpCall() {
        FakeGeminiHttp http = new FakeGeminiHttp(geminiAnswer("Mobile Phone"));
        GeminiClassificationServiceImpl svc = service(http, "");
        List<MaterialMaster> taxonomy = List.of(material(1L, "Mobile Phone", "Smartphone"));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> svc.classify(new byte[]{1}, "image/jpeg", taxonomy));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
        assertEquals(0, http.calls);
    }

    @Test
    void invalidImageRejectedBeforeHttpCall() {
        FakeGeminiHttp http = new FakeGeminiHttp(geminiAnswer("Mobile Phone"));
        GeminiClassificationServiceImpl svc = service(http, "test-key");
        List<MaterialMaster> taxonomy = List.of(material(1L, "Mobile Phone", "Smartphone"));
        assertThrows(ResponseStatusException.class,
                () -> svc.classify(new byte[0], "image/jpeg", taxonomy));
        assertThrows(ResponseStatusException.class,
                () -> svc.classify(new byte[]{1}, "application/pdf", taxonomy));
        assertEquals(0, http.calls);
    }

    /** Minimal fake for the Gemini HTTP boundary — no Mockito needed. */
    static class FakeGeminiHttp extends RestTemplate {
        final Map<?, ?> answer;
        int calls = 0;
        String lastUrl;

        FakeGeminiHttp(Map<?, ?> answer) {
            this.answer = answer;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T postForObject(String url, Object request, Class<T> responseType, Object... uriVars) {
            calls++;
            lastUrl = url;
            return (T) answer;
        }
    }
}