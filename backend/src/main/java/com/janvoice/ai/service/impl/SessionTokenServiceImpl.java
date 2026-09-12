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

    public SessionTokenServiceImpl(UserRepository users,
                                   @Value("${session.token.secret}") String secret) {
        this.users = users;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        if (secret.length() < 32) throw new IllegalArgumentException("session.token.secret must be at least 32 characters");
    }

    @Override
    public String issue(User user) {
        long expiresAt = Instant.now().plusSeconds(TOKEN_LIFETIME_SECONDS).getEpochSecond();
        String payload = user.getId() + "." + expiresAt;
        return encode(payload) + "." + encode(sign(payload));
    }

    @Override
    public User authenticate(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) unauthorized();
        String token = authorizationHeader.substring("Bearer ".length()).trim();
        String[] parts = token.split("\\.", -1);
        if (parts.length != 2) unauthorized();
        try {
            String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            byte[] suppliedSignature = Base64.getUrlDecoder().decode(parts[1]);
            if (!MessageDigest.isEqual(sign(payload), suppliedSignature)) unauthorized();
            String[] values = payload.split("\\.", -1);
            long userId = Long.parseLong(values[0]);
            long expiresAt = Long.parseLong(values[1]);
            if (Instant.now().getEpochSecond() >= expiresAt) unauthorized();
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
