package com.janvoice.ai.entity;

import jakarta.persistence.*;

/**
 * JPA Entity mapping the 'upvotes' database table.
 * Standard Java implementation (No Lombok annotation wrappers).
 */
@Entity
@Table(name = "upvotes", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "user_id", "complaint_id" })
})
public class Upvote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complaint_id", nullable = false)
    private Complaint complaint;

    // Default Constructor
    public Upvote() {
    }

    // Full Constructor
    public Upvote(Long id, User user, Complaint complaint) {
        this.id = id;
        this.user = user;
        this.complaint = complaint;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Complaint getComplaint() {
        return complaint;
    }

    public void setComplaint(Complaint complaint) {
        this.complaint = complaint;
    }

    // Builder
    public static UpvoteBuilder builder() {
        return new UpvoteBuilder();
    }

    public static class UpvoteBuilder {
        private Long id;
        private User user;
        private Complaint complaint;

        public UpvoteBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public UpvoteBuilder user(User user) {
            this.user = user;
            return this;
        }

        public UpvoteBuilder complaint(Complaint complaint) {
            this.complaint = complaint;
            return this;
        }

        public Upvote build() {
            return new Upvote(id, user, complaint);
        }
    }
}
