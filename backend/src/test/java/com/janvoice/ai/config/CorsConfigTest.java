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
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regression tests for the CORS failures that blocked real (non-demo) logins on
 * the deployed site.
 *
 * <p>Every API call from the SPA carries an {@code Authorization} header, so the
 * browser always sends a preflight first. Those preflights used to fail — 500
 * when {@code CORS_ALLOWED_ORIGIN} was configured (Spring rejects
 * {@code allowedOrigins="*"} combined with {@code allowCredentials=true}) and 403
 * when the request origin was not listed — which made dashboard, earnings, lots
 * and transactions all appear broken while demo mode (no API calls) worked.
 *
 * <p>With no origin configured the API accepts every origin, so the deployed
 * frontend never depends on an environment variable being spelled correctly.
 */
@WebMvcTest(controllers = DashboardController.class)
@Import(CorsConfig.class)
class CorsConfigTest {

    private static final String[] DEPLOYED_ORIGINS = {
            "https://e-waste-saathi.vercel.app",
            "https://frontend-production.up.railway.app",
            "http://localhost:5173",
            "http://127.0.0.1:4173"
    };

    @Autowired private MockMvc mockMvc;
    @MockBean private DashboardService dashboardService;
    @MockBean private RecyclerDashboardService recyclerDashboardService;
    @MockBean private SessionTokenService sessionTokenService;

    @Test
    void preflightSucceedsForEveryDeployedFrontendOrigin() throws Exception {
        for (String origin : DEPLOYED_ORIGINS) {
            mockMvc.perform(options("/api/dashboard/summary")
                            .header("Origin", origin)
                            .header("Access-Control-Request-Method", "GET")
                            .header("Access-Control-Request-Headers", "authorization,content-type"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", origin))
                    .andExpect(header().string("Access-Control-Allow-Methods", containsString("GET")))
                    .andExpect(header().string("Access-Control-Allow-Headers", allOf(
                            containsStringIgnoringCase("authorization"),
                            containsStringIgnoringCase("content-type"))));
        }
    }

    @Test
    void authenticatedRequestFromDeployedOriginIsAccepted() throws Exception {
        when(dashboardService.summary(any())).thenReturn(Map.of());
        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Origin", "https://e-waste-saathi.vercel.app")
                        .header("Authorization", "Bearer session-token"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://e-waste-saathi.vercel.app"));
    }

    @Test
    void credentialsHeaderIsNotSentBecauseTheApiUsesBearerTokens() throws Exception {
        mockMvc.perform(options("/api/dashboard/summary")
                        .header("Origin", "https://e-waste-saathi.vercel.app")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                // Keeps the wildcard/credentials combination (the production crash) impossible.
                .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
    }

    // NOTE: authentication (401) is enforced inside the service layer, so it is
    // asserted in the full-context tests (DashboardControllerTest,
    // RecyclerRoleAccessTest) rather than in this MockMvc slice, where the
    // services are mocked.
}