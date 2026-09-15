package com.janvoice.ai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Single, authoritative CORS configuration for the whole API.
 *
 * <h2>Why origin patterns instead of origins</h2>
 * {@code allowedOrigins} must never receive the {@code "*"} wildcard while
 * {@code allowCredentials} is {@code true}. Spring <em>merges</em> the global
 * configuration with every matching {@code @CrossOrigin} annotation, and that
 * merged configuration makes {@code CorsConfiguration.checkOrigin} throw
 * <pre>
 * java.lang.IllegalArgumentException: When allowCredentials is true,
 * allowedOrigins cannot contain the special value "*" ...
 * </pre>
 * surfacing as {@code ServletException: Request processing failed} — a failed
 * preflight for every {@code /api/**} call carrying an {@code Authorization}
 * header (all of them, the SPA uses Bearer tokens). That was the production
 * failure mode: the API worked with CORS unset and broke as soon as the
 * documented {@code CORS_ALLOWED_ORIGIN}/ALLOWED_ORIGIN variable was configured.
 *
 * <p>This class therefore (a) only ever uses {@code allowedOriginPatterns}
 * (patterns are compiled by Spring, {@code *} becomes {@code .*}), (b) sets
 * {@code allowCredentials=false} because the SPA keeps its Bearer token in
 * {@code localStorage} and never sends cookies, and (c) additionally accepts
 * local development hosts plus common PaaS preview domains, so a typo in the
 * environment variable can never lock the deployed frontend out again.
 *
 * <h2>Configuration</h2>
 * <pre>
 * CORS_ALLOWED_ORIGIN / CORS_ALLOWED_ORIGINS / cors.allowed-origins
 *     comma separated origins or patterns, e.g.
 *     https://e-waste-saathi.vercel.app,https://*.up.railway.app
 * CORS_STRICT=true   (optional) honour only the configured list.
 * </pre>
 */
@Configuration
public class CorsConfig {

    /**
     * Always-allowed origin patterns (local development and PaaS preview hosts),
     * kept as patterns so any port / any sub-domain works.
     */
    static final List<String> RELAXED_ORIGIN_PATTERNS = List.of(
            "http://localhost:*",
            "http://127.0.0.1:*",
            "http://0.0.0.0:*",
            "https://localhost:*",
            "https://127.0.0.1:*",
            "https://*.vercel.app",
            "https://*.netlify.app",
            "https://*.railway.app",
            "https://*.up.railway.app",
            "https://*.onrender.com",
            "https://*.render.com",
            "https://*.herokuapp.com",
            "https://*.github.dev",
            "https://*.app.github.dev");

    private final List<String> configuredOrigins;
    private final boolean strict;

    public CorsConfig(
            @Value("${cors.allowed-origins:${cors.allowed-origin:${allowed.origins:${allowed.origin:}}}}") String allowedOrigins,
            @Value("${cors.strict:false}") boolean strict) {
        this.configuredOrigins = parse(allowedOrigins);
        this.strict = strict;
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        List<String> patterns = resolvePatterns();
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOriginPatterns(patterns.toArray(new String[0]))
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD")
                        .allowedHeaders("*")
                        .exposedHeaders("Location")
                        // Bearer-token API: no cookies, so credentials stay off and the
                        // wildcard/credentials conflict is structurally impossible.
                        .allowCredentials(false)
                        .maxAge(3600);
            }
        };
    }

    /**
     * Patterns actually registered:
     * <ul>
     *   <li>nothing configured → every origin (previous behaviour, cookie-free API);</li>
     *   <li>configured → configured entries + {@link #RELAXED_ORIGIN_PATTERNS};</li>
     *   <li>configured with {@code CORS_STRICT=true} → only the configured entries.</li>
     * </ul>
     */
    List<String> resolvePatterns() {
        if (configuredOrigins.isEmpty()) {
            return List.of("*");
        }
        if (strict) {
            return List.copyOf(configuredOrigins);
        }
        LinkedHashSet<String> patterns = new LinkedHashSet<>(configuredOrigins);
        patterns.addAll(RELAXED_ORIGIN_PATTERNS);
        return List.copyOf(patterns);
    }

    /** Splits a comma separated origin list, trimming blanks and trailing slashes. */
    static List<String> parse(String value) {
        List<String> origins = new ArrayList<>();
        if (value == null || value.isBlank()) {
            return origins;
        }
        for (String part : value.split(",")) {
            String origin = part.trim();
            while (origin.endsWith("/")) {
                origin = origin.substring(0, origin.length() - 1);
            }
            if (!origin.isEmpty() && !origins.contains(origin)) {
                origins.add(origin);
            }
        }
        return origins;
    }
}

