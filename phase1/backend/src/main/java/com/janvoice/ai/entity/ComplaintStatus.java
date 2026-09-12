package com.janvoice.ai.entity;

/**
 * Enumeration representing the lifecycle states of a grievance.
 */
public enum ComplaintStatus {
    PENDING, // Default when registered
    IN_PROGRESS, // Actively handled by municipal departments
    RESOLVED // Fixed/Closed by MP review team
}
