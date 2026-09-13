package com.janvoice.ai.service.impl;

import com.janvoice.ai.entity.User;
import com.janvoice.ai.repository.UserRepository;
import com.janvoice.ai.service.SessionTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

@Service
public class SessionTokenServiceImpl implements SessionTokenService {
    private static final long TOKEN_LIFETIME_SECONDS = 24 * 60 * 60;
    private final UserRepository users;
    private final byte[] secret;

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SessionTokenServiceImpl.class);

    public SessionTokenServiceImpl(UserRepository users,
                                   @Value("${session.token.secret}") String secret,
                                   org.springframework.core.env.Environment env) {
        this.users = users;
        // Determine if we are running in the default (dev) profile (no explicit profiles).
        boolean isDefaultProfile = env.getActiveProfiles().length == 0;
        // List of known placeholder values that must never be used in production.
        java.util.Set<String> placeholders = java.util.Set.of(
                "local-development-session-secret-change-me-32chars",
                "your_32_character_minimum_secure_random_string_here",
                "");
        if (secret == null || secret.isBlank() || placeholders.contains(secret)) {
            if (isDefaultProfile) {
                // Generate a temporary secure secret for this dev session.
                byte[] random = new byte[24]; // 24 bytes -> 32 Base64 chars
                new java.security.SecureRandom().nextBytes(random);
                secret = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(random);
                logger.warn("No SESSION_TOKEN_SECRET set — generated a temporary secret for this session only. Set SESSION_TOKEN_SECRET explicitly before deploying to production.");
            } else {
                throw new IllegalStateException("A secure SESSION_TOKEN_SECRET must be configured.");
            }
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        if (secret.length() < 32) {
            throw new IllegalArgumentException("session.token.secret must be at least 32 characters");
        }
    }

    @Override
    public String issue(User user) {
        long expiresAt = Instant.now().plusSeconds(TOKEN_LIFETIME_SECONDS).getEpochSecond();
        String payload = user.getId() + "." + expiresAt;
        return encode(payload) + "." + encode(sign(payload));
    }

    @Override
    public User authenticate(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) throw unauthorized();
        String token = authorizationHeader.substring("Bearer ".length()).trim();
        String[] parts = token.split("\\.", -1);
        if (parts.length != 2) throw unauthorized();
        try {
            String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            byte[] suppliedSignature = Base64.getUrlDecoder().decode(parts[1]);
            if (!MessageDigest.isEqual(sign(payload), suppliedSignature)) throw unauthorized();
            String[] values = payload.split("\\.", -1);
            long userId = Long.parseLong(values[0]);
            long expiresAt = Long.parseLong(values[1]);
            if (Instant.now().getEpochSecond() >= expiresAt) throw unauthorized();
            return users.findById(userId).orElseThrow(() -> unauthorized());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid session token");
        }
    }

    private byte[] sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to sign session token", exception);
        }
    }

    private String encode(String value) { return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8)); }
    private String encode(byte[] value) { return Base64.getUrlEncoder().withoutPadding().encodeToString(value); }
    private ResponseStatusException unauthorized() { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Valid session token required"); }
}
