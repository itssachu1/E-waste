package com.janvoice.ai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer(@Value("${cors.allowed-origin:#{null}}") String allowedOrigin) {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                if (allowedOrigin != null && !allowedOrigin.isBlank()) {
                    // Production: single explicit origin from env var (e.g. https://your-app.up.railway.app)
                    registry.addMapping("/**")
                            .allowedOrigins(allowedOrigin)
                            .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD")
                            .allowedHeaders("*")
                            .allowCredentials(true);
                } else {
                    // Local dev fallback: permissive pattern matching for localhost
                    registry.addMapping("/**")
                            .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*",
                                    "http://0.0.0.0:*", "https://*.app.github.dev",
                                    "https://*.up.railway.app")
                            .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD")
                            .allowedHeaders("*")
                            .allowCredentials(false);
                }
            }
        };
    }
}

