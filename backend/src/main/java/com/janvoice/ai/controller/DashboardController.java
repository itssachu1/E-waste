package com.janvoice.ai.controller;

import com.janvoice.ai.service.DashboardService;
import com.janvoice.ai.service.RecyclerDashboardService;
import com.janvoice.ai.service.SessionTokenService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST endpoints backing the Dashboard. Collector summary values are
 * computed live from database aggregates; recycler summary is served
 * from a separate endpoint scoped to the recycler's lots/transactions.
 */
@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {
    private final DashboardService service;
    private final RecyclerDashboardService recyclerDashboard;
    private final SessionTokenService tokens;

    public DashboardController(DashboardService service, RecyclerDashboardService recyclerDashboard, SessionTokenService tokens) {
        this.service = service;
        this.recyclerDashboard = recyclerDashboard;
        this.tokens = tokens;
    }

    @GetMapping("/summary")
    public Map<String, Object> summary(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return service.summary(tokens.authenticate(authorization));
    }

    /** Recycler-specific dashboard stats: incoming lots, confirmations pending, volume received. */
    @GetMapping("/recycler-summary")
    public Map<String, Object> recyclerSummary(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return recyclerDashboard.summary(tokens.authenticate(authorization));
    }

    @GetMapping("/recent-lots")
    public List<Map<String, Object>> recentLots(
            @RequestParam(defaultValue = "5") int limit,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return service.recentLots(tokens.authenticate(authorization), limit);
    }

    @GetMapping("/recent-transactions")
    public List<Map<String, Object>> recentTransactions(
            @RequestParam(defaultValue = "5") int limit,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return service.recentTransactions(tokens.authenticate(authorization), limit);
    }
}