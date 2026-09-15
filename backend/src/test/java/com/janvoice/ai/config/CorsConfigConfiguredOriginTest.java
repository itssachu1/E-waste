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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The exact production configuration that used to break the deployed site: an
 * explicit CORS origin is configured (as {@code .env.example} instructs) while
 * the request comes from a different origin.
 *
 * <p>Before the fix Spring merged this list with the per-controller
 * {@code @CrossOrigin(origins = "*")} into
 * {@code allowedOrigins=["*"] + allowCredentials=true} and failed every
 * preflight with an {@code IllegalArgumentException} (HTTP 500), while a plain
 * origin mismatch returned 403 "Invalid CORS request" — both invisible to demo
 * mode because demo profiles never call the API.
 */
@WebMvcTest(controllers = DashboardController.class)
@Import(CorsConfig.class)
@TestPropertySource(properties = "cors.allowed-origin=https://configured-frontend.example.com")
class CorsConfigConfiguredOriginTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private DashboardService dashboardService;
    @MockBean private RecyclerDashboardService recyclerDashboardService;
    @MockBean private SessionTokenService sessionTokenService;

    @Test
    void configuredOriginIsAcceptedWithoutAnyException() throws Exception {
        when(dashboardService.summary(any())).thenReturn(Map.of());
        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Origin", "https://configured-frontend.example.com")
                        .header("Authorization", "Bearer session-token"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://configured-frontend.example.com"));
    }

    @Test
    void deployedFrontendOriginStillWorksWhenTheConfiguredValueIsWrong() throws Exception {
        // A mistyped/renamed environment variable must not lock the SPA out again.
        for (String origin : new String[]{"https://e-waste-saathi.vercel.app", "https://frontend.up.railway.app", "http://localhost:5173"}) {
            when(dashboardService.summary(any())).thenReturn(Map.of());
            mockMvc.perform(get("/api/dashboard/summary")
                            .header("Origin", origin)
                            .header("Authorization", "Bearer session-token"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", origin));
        }
    }

    @Test
    void preflightForAPostFromTheConfiguredOriginSucceeds() throws Exception {
        mockMvc.perform(options("/api/dashboard/summary")
                        .header("Origin", "https://configured-frontend.example.com")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://configured-frontend.example.com"));
    }
}