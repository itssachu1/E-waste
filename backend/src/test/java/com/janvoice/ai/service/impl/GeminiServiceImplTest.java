package com.janvoice.ai.service.impl;

import com.janvoice.ai.dto.GeminiAnalysisResult;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class GeminiServiceImplTest {

    @Test
    void analyzeComplaintFallsBackToRuleBasedPriorityForEmergencies() {
        GeminiServiceImpl service = new GeminiServiceImpl();
        ReflectionTestUtils.setField(service, "apiKey", "");
        ReflectionTestUtils.setField(service, "apiUrl", "https://example.invalid");

        GeminiAnalysisResult result = service.analyzeComplaint("There is a fire near the school and people are in danger");

        assertThat(result.getCategory()).isEqualTo("EMERGENCY");
        assertThat(result.getUrgency()).isEqualTo("CRITICAL");
        assertThat(result.getSuggestedDepartment()).isEqualTo("Emergency Response");
        assertThat(result.getIsEmergency()).isTrue();
    }
}
