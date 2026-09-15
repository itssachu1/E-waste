package com.janvoice.ai.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure unit tests for the origin-list parsing / pattern resolution of
 * {@link CorsConfig} (no Spring context, so they are instant to run).
 */
class CorsConfigParsingTest {

    @Test
    void splitsCommaSeparatedOriginsAndTrimsTrailingSlashes() {
        assertEquals(
                List.of("https://a.example.com", "https://b.example.com"),
                CorsConfig.parse(" https://a.example.com/ , https://b.example.com ,, "));
    }

    @Test
    void blankOrNullConfigurationYieldsNoOrigins() {
        assertTrue(CorsConfig.parse(null).isEmpty());
        assertTrue(CorsConfig.parse("   ").isEmpty());
    }

    @Test
    void nothingConfiguredAllowsEveryOrigin() {
        assertEquals(List.of("*"), new CorsConfig("", false).resolvePatterns());
    }

    @Test
    void configuredOriginAlsoKeepsDevelopmentAndPaasHosts() {
        List<String> patterns = new CorsConfig("https://e-waste-saathi.vercel.app", false).resolvePatterns();
        assertTrue(patterns.contains("https://e-waste-saathi.vercel.app"), "configured origin kept");
        assertTrue(patterns.contains("http://localhost:*"), "local development kept");
        assertTrue(patterns.contains("https://*.up.railway.app"), "Railway previews kept");
    }

    @Test
    void strictModeReturnsOnlyTheConfiguredOrigins() {
        List<String> patterns = new CorsConfig("https://e-waste-saathi.vercel.app,https://*.up.railway.app", true).resolvePatterns();
        assertEquals(List.of("https://e-waste-saathi.vercel.app", "https://*.up.railway.app"), patterns);
    }

    @Test
    void wildcardConfigurationIsHonoured() {
        assertEquals(List.of("*"), new CorsConfig("*", true).resolvePatterns());
    }
}