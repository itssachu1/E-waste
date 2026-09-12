package com.janvoice.ai.entity;

import jakarta.persistence.*;

/**
 * JPA Entity mapping the 'users' database table.
 * Written using Standard Java (no Lombok) for maximum JRE compiler
 * compatibility.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role;

    @Column(name = "ward_area", length = 50)
    private String wardArea;

    // Default Constructor (required by JPA)
    public User() {
    }

    // Convenience Constructor
    public User(String username, String password, String role, String wardArea) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.wardArea = wardArea;
    }

    // Full Constructor
    public User(Long id, String username, String password, String role, String wardArea) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
        this.wardArea = wardArea;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    // Simple Builder Pattern
    public static UserBuilder builder() {
        return new UserBuilder();
    }

    public static class UserBuilder {
        private Long id;
        private String username;
        private String password;
        private String role;
        private String wardArea;

        public UserBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public UserBuilder username(String username) {
            this.username = username;
            return this;
        }

        public UserBuilder password(String password) {
            this.password = password;
            return this;
        }

        public UserBuilder role(String role) {
            this.role = role;
            return this;
        }

        public UserBuilder wardArea(String wardArea) {
            this.wardArea = wardArea;
            return this;
        }

        public User build() {
            return new User(id, username, password, role, wardArea);
        }
    }
}
