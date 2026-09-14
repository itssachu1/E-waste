package com.janvoice.ai.service;

import com.janvoice.ai.entity.User;

import java.util.Map;

/**
 * Recycler-specific dashboard aggregation. All values are computed
 * from real database rows — nothing fabricated.
 */
public interface RecyclerDashboardService {
    /**
     * @param actor authenticated user (must be RECYCLER or VERIFIED_RECYCLER)
     * @return map with: incoming_lots_count, awaiting_confirmation_count,
     *         confirmed_lots_count, total_volume_kg, pending_payments_count
     */
    Map<String, Object> summary(User actor);
}
