package com.janvoice.ai.dto;

/**
 * Data Transfer Object reflecting login profile.
 * Standard Java implementation (No Lombok annotation wrappers).
 */
public class AuthResponse {
    private Long userId;
    private String username;
    private String role;
    private String wardArea;
    private String message;

    // Default Constructor
    public AuthResponse() {
    }

    // Full Constructor
    public AuthResponse(Long userId, String username, String role, String wardArea, String message) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.wardArea = wardArea;
        this.message = message;
    }

    // Getters and Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getWardArea() {
        return wardArea;
    }

    public void setWardArea(String wardArea) {
        this.wardArea = wardArea;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    // Builder
    public static AuthResponseBuilder builder() {
        return new AuthResponseBuilder();
    }

    public static class AuthResponseBuilder {
        private Long userId;
        private String username;
        private String role;
        private String wardArea;
        private String message;

        public AuthResponseBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public AuthResponseBuilder username(String username) {
            this.username = username;
            return this;
        }

        public AuthResponseBuilder role(String role) {
            this.role = role;
            return this;
        }

        public AuthResponseBuilder wardArea(String wardArea) {
            this.wardArea = wardArea;
            return this;
        }

        public AuthResponseBuilder message(String message) {
            this.message = message;
            return this;
        }

        public AuthResponse build() {
            return new AuthResponse(userId, username, role, wardArea, message);
        }
    }
}
