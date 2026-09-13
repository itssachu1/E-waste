package com.janvoice.ai.service;

import com.janvoice.ai.entity.User;

import java.util.List;
import java.util.Map;

/**
 * Collector dashboard aggregation service.
 *
 * Every returned value is computed from real database rows via aggregate
 * queries (SUM/COUNT) in the repositories — nothing is estimated or
 * hardcoded, and nothing is loaded into memory client-side.
 */
public interface DashboardService {
    Map<String, Object> summary(User actor);
    List<Map<String, Object>> recentLots(User actor, int limit);
    List<Map<String, Object>> recentTransactions(User actor, int limit);
}