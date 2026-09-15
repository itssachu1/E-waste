package com.janvoice.ai.service.impl;

import com.janvoice.ai.entity.Lot;
import com.janvoice.ai.entity.Transaction;
import com.janvoice.ai.entity.User;
import com.janvoice.ai.repository.LotRepository;
import com.janvoice.ai.repository.TransactionRepository;
import com.janvoice.ai.service.DashboardService;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardServiceImpl implements DashboardService {
    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_LIMIT = 50;

    /**
     * In-progress lifecycle states: between CREATED and HANDED_OVER inclusive.
     * Terminal/closed states (RECYCLER_CONFIRMED, PAID, CANCELLED) and the
     * pre-registration DRAFT state are excluded.
     */
    private static final List<Lot.Status> ACTIVE_STATUSES = Arrays.asList(
            Lot.Status.CREATED, Lot.Status.QUOTE_REQUESTED, Lot.Status.QUOTE_RECEIVED,
            Lot.Status.ACCEPTED, Lot.Status.READY_FOR_HANDOVER, Lot.Status.HANDED_OVER);

    /** A handover is completed when the lot is confirmed by the recycler or paid. */
    private static final List<Lot.Status> COMPLETED_STATUSES = Arrays.asList(
            Lot.Status.RECYCLER_CONFIRMED, Lot.Status.PAID);

    private final TransactionRepository transactions;
    private final LotRepository lots;

    public DashboardServiceImpl(TransactionRepository transactions, LotRepository lots) {
        this.transactions = transactions;
        this.lots = lots;
    }

    @Override @Transactional
    public Map<String, Object> summary(User actor) {
        requireCollector(actor);

        // All aggregates run in the database (bounded, indexed queries).
        BigDecimal totalEarnings = transactions.sumAmountByCollectorAndStatus(actor, Transaction.PaymentStatus.PAID);
        BigDecimal pendingAmount = transactions.sumAmountByCollectorAndStatus(actor, Transaction.PaymentStatus.PENDING);
        BigDecimal totalKg = transactions.sumWeightByCollectorAndStatus(actor, Transaction.PaymentStatus.PAID);
        long paidTransactions = transactions.countByCollectorAndPaymentStatus(actor, Transaction.PaymentStatus.PAID);
        long totalLots = lots.countByCollector(actor);
        long activeLots = lots.countByCollectorAndStatusIn(actor, ACTIVE_STATUSES);
        long completedHandovers = lots.countByCollectorAndStatusIn(actor, COMPLETED_STATUSES);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total_earnings", totalEarnings);
        result.put("pending_amount", pendingAmount);
        result.put("total_kg_collected", totalKg);
        result.put("paid_transactions_count", paidTransactions);
        result.put("total_lots_count", totalLots);
        result.put("active_lots_count", activeLots);
        result.put("completed_handovers_count", completedHandovers);
        return result;
    }

    @Override @Transactional
    public List<Map<String, Object>> recentLots(User actor, int limit) {
        requireCollector(actor);
        return lots.findByCollectorOrderByCreatedAtDesc(actor, PageRequest.of(0, normalizeLimit(limit)))
                .stream().map(this::lotMap).collect(Collectors.toList());
    }

    @Override @Transactional
    public List<Map<String, Object>> recentTransactions(User actor, int limit) {
        requireCollector(actor);
        return transactions.findByCollectorOrderByCreatedAtDesc(actor, PageRequest.of(0, normalizeLimit(limit)))
                .stream().map(this::transactionMap).collect(Collectors.toList());
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) return DEFAULT_LIMIT;
        return Math.min(limit, MAX_LIMIT);
    }

    private Map<String, Object> lotMap(Lot lot) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", lot.getId());
        map.put("lot_reference", lot.getLotReference());
        map.put("material_name", lot.getMaterial().getCommonName());
        map.put("approximate_weight", lot.getApproximateWeight());
        map.put("final_weight", lot.getFinalWeight());
        map.put("estimated_value", lot.getEstimatedValue());
        map.put("final_sale_amount", lot.getFinalSaleAmount());
        map.put("status", lot.getStatus());
        map.put("created_at", lot.getCreatedAt());
        return map;
    }

    private Map<String, Object> transactionMap(Transaction transaction) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", transaction.getId());
        map.put("lot_reference", transaction.getLot().getLotReference());
        map.put("amount", transaction.getAmount());
        map.put("payment_mode", transaction.getPaymentMode());
        map.put("payment_reference", transaction.getPaymentReference());
        map.put("payment_status", transaction.getPaymentStatus());
        map.put("transaction_time", transaction.getTransactionTime());
        map.put("created_at", transaction.getCreatedAt());
        return map;
    }

    /**
     * Collector dashboards are open to collector accounts (COLLECTOR/CITIZEN)
     * and to verified recycler accounts. Every aggregate above is scoped to the
     * authenticated actor, so a recycler receives its own (usually empty)
     * collector totals instead of a hard 403. Recycler-specific figures are
     * served by /api/dashboard/recycler-summary.
     */
    private void requireCollector(User actor) {
        if (actor == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Valid session token required");
        if (!isCollector(actor) && !isRecycler(actor)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Collector or recycler role required");
        }
    }

    private boolean isCollector(User actor) {
        return "COLLECTOR".equalsIgnoreCase(actor.getRole()) || "CITIZEN".equalsIgnoreCase(actor.getRole());
    }

    private boolean isRecycler(User actor) {
        return "RECYCLER".equalsIgnoreCase(actor.getRole()) || "VERIFIED_RECYCLER".equalsIgnoreCase(actor.getRole());
    }
}