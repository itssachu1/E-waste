package com.janvoice.ai.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Data Transfer Object containing credentials when logging in.
 * Standard Java implementation (No Lombok annotation wrappers).
 */
public class LoginRequest {

    @NotBlank(message = "Username cannot be empty")
    private String username;

    @NotBlank(message = "Password cannot be empty")
    private String password;

    // Default Constructor
    public LoginRequest() {
    }

    // Full Constructor
    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
