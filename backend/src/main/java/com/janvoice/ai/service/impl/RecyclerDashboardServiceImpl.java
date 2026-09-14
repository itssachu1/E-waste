package com.janvoice.ai.service.impl;

import com.janvoice.ai.entity.Lot;
import com.janvoice.ai.entity.Transaction;
import com.janvoice.ai.entity.User;
import com.janvoice.ai.repository.LotRepository;
import com.janvoice.ai.repository.RecyclerRepository;
import com.janvoice.ai.repository.TransactionRepository;
import com.janvoice.ai.service.RecyclerDashboardService;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Recycler dashboard: stats about lots assigned to this recycler,
 * confirmations pending, volume received, and pending payments.
 * All aggregates run in the database via COUNT/SUM.
 */
@Service
public class RecyclerDashboardServiceImpl implements RecyclerDashboardService {

    private static final List<Lot.Status> INCOMING_STATUSES = Arrays.asList(
            Lot.Status.QUOTE_REQUESTED, Lot.Status.QUOTE_RECEIVED,
            Lot.Status.ACCEPTED, Lot.Status.READY_FOR_HANDOVER);

    private final LotRepository lots;
    private final TransactionRepository transactions;
    private final RecyclerRepository recyclers;

    public RecyclerDashboardServiceImpl(LotRepository lots, TransactionRepository transactions, RecyclerRepository recyclers) {
        this.lots = lots;
        this.transactions = transactions;
        this.recyclers = recyclers;
    }

    @Override
    @Transactional
    public Map<String, Object> summary(User actor) {
        requireRecycler(actor);
        Long recyclerUserId = actor.getId();

        // Resolve the recycler profile owned by this user to scope queries by recycler_id.
        Long recyclerId = recyclers.findAll().stream()
                .filter(r -> r.getCreatedBy() != null && r.getCreatedBy().equals(recyclerUserId))
                .map(com.janvoice.ai.entity.Recycler::getId)
                .findFirst().orElse(null);

        if (recyclerId == null) {
            // No recycler profile yet — return zeros, not an error.
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("incoming_lots_count", 0);
            empty.put("awaiting_confirmation_count", 0);
            empty.put("confirmed_lots_count", 0);
            empty.put("total_volume_kg", BigDecimal.ZERO);
            empty.put("pending_payments_count", 0);
            empty.put("has_profile", false);
            return empty;
        }

        long incoming = lots.countByRecyclerIdAndStatusIn(recyclerId, INCOMING_STATUSES);
        long awaiting = lots.countByRecyclerIdAndStatus(recyclerId, Lot.Status.HANDED_OVER);
        long confirmed = lots.countByRecyclerIdAndStatusIn(recyclerId,
                Arrays.asList(Lot.Status.RECYCLER_CONFIRMED, Lot.Status.PAID));
        BigDecimal volume = transactions.sumWeightByRecyclerId(recyclerId);
        long pendingPayments = transactions.countByRecyclerIdAndStatus(recyclerId, Transaction.PaymentStatus.PENDING);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("incoming_lots_count", incoming);
        result.put("awaiting_confirmation_count", awaiting);
        result.put("confirmed_lots_count", confirmed);
        result.put("total_volume_kg", volume == null ? BigDecimal.ZERO : volume);
        result.put("pending_payments_count", pendingPayments);
        result.put("has_profile", true);
        return result;
    }

    private void requireRecycler(User actor) {
        if (actor == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Valid session token required");
        // Allow explicit recycler roles OR any user who owns a recycler profile.
        if ("RECYCLER".equalsIgnoreCase(actor.getRole()) || "VERIFIED_RECYCLER".equalsIgnoreCase(actor.getRole())) return;
        boolean ownsProfile = recyclers.findAll().stream().anyMatch(r -> r.getCreatedBy() != null && r.getCreatedBy().equals(actor.getId()));
        if (!ownsProfile) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Recycler profile required");
    }
}
