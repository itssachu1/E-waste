package com.janvoice.ai.controller;

import com.janvoice.ai.dto.AuthResponse;
import com.janvoice.ai.dto.LoginRequest;
import com.janvoice.ai.entity.User;
import com.janvoice.ai.repository.UserRepository;
import com.janvoice.ai.service.SessionTokenService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * REST controller exposure for authentication routing (Registration, Login,
 * Demo profiles).
 * CORS is configured centrally by CorsConfig. Per-controller @CrossOrigin
 * overrides were removed: Spring merges them with the global configuration, and
 * a merged "*"/credentials combination makes every preflight fail.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionTokenService sessionTokenService;

    /**
     * User registration handler. Saves collector/recycler profile to database.
     * Public registration allows COLLECTOR (default), CITIZEN (legacy alias of
     * collector) and RECYCLER; any other requested role is downgraded to COLLECTOR.
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(MapResponse("Username '" + user.getUsername() + "' is already taken."));
        }

        // Public registration cannot provision privileged roles (ADMIN/MP).
        // Accept COLLECTOR (default), CITIZEN (legacy collector alias) and
        // RECYCLER so the E-Waste Saathi login screen maps 1:1 to the backend.
        String requested = user.getRole() == null ? "" : user.getRole().trim().toUpperCase();
        if ("RECYCLER".equals(requested) || "CITIZEN".equals(requested) || "COLLECTOR".equals(requested)) {
            user.setRole("CITIZEN".equals(requested) ? "CITIZEN" : requested);
        } else {
            user.setRole("COLLECTOR");
        }

        // Save User
        User savedUser = userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AuthResponse.builder()
                        .userId(savedUser.getId())
                        .username(savedUser.getUsername())
                        .role(savedUser.getRole())
                        .wardArea(savedUser.getWardArea())
                        .message("Registration successful.")
                        .build());
    }

    /**
     * User Login handler.
     * Keeps password comparisons simple for the hackathon MVP prototype.
     */
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@Valid @RequestBody LoginRequest request) {
        Optional<User> userOpt = userRepository.findByUsername(request.getUsername());

        if (userOpt.isEmpty() || !userOpt.get().getPassword().equals(request.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(MapResponse("Invalid username or password credentials."));
        }

        User user = userOpt.get();

        return ResponseEntity.ok(AuthResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .wardArea(user.getWardArea())
                .sessionToken(sessionTokenService.issue(user))
                .message("Login successful.")
                .build());
    }

    // Quick helper to map error messages
    private java.util.Map<String, String> MapResponse(String msg) {
        java.util.Map<String, String> map = new java.util.HashMap<>();
        map.put("error", msg);
        return map;
    }
}
