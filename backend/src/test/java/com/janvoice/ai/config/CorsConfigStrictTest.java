package com.janvoice.ai.config;

import com.janvoice.ai.controller.DashboardController;
import com.janvoice.ai.service.DashboardService;
import com.janvoice.ai.service.RecyclerDashboardService;
import com.janvoice.ai.service.SessionTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code CORS_STRICT=true} genuinely restricts the API to the configured origins
 * (opt-in lock-down for a deployment that wants no convenience defaults).
 * Unlisted origins get the CORS 403 "Invalid CORS request" — and, importantly,
 * no server side exception (the 500 this class of misconfiguration used to cause).
 */
@WebMvcTest(controllers = DashboardController.class)
@Import(CorsConfig.class)
@TestPropertySource(properties = {
        "cors.strict=true",
        "cors.allowed-origins=https://e-waste-saathi.vercel.app,https://*.up.railway.app"
})
class CorsConfigStrictTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private DashboardService dashboardService;
    @MockBean private RecyclerDashboardService recyclerDashboardService;
    @MockBean private SessionTokenService sessionTokenService;

    @Test
    void listedOriginsAndPatternsAreAllowed() throws Exception {
        when(dashboardService.summary(any())).thenReturn(Map.of());
        for (String origin : new String[]{"https://e-waste-saathi.vercel.app", "https://frontend.up.railway.app"}) {
            mockMvc.perform(get("/api/dashboard/summary")
                            .header("Origin", origin)
                            .header("Authorization", "Bearer session-token"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", origin));
        }
    }

    @Test
    void unlistedOriginIsRejectedWithCorsForbiddenAndNoServerError() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Origin", "https://unlisted.example.com")
                        .header("Authorization", "Bearer session-token"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}