package com.janvoice.ai.service.impl;

import com.janvoice.ai.dto.TransactionRequest;
import com.janvoice.ai.dto.TransactionStatusRequest;
import com.janvoice.ai.entity.*;
import com.janvoice.ai.repository.LotRepository;
import com.janvoice.ai.repository.RecyclerRepository;
import com.janvoice.ai.repository.TransactionRepository;
import com.janvoice.ai.service.TransactionService;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TransactionServiceImpl implements TransactionService {
    private final TransactionRepository transactions;
    private final LotRepository lots;
    private final RecyclerRepository recyclers;

    public TransactionServiceImpl(TransactionRepository transactions, LotRepository lots, RecyclerRepository recyclers) { this.transactions = transactions; this.lots = lots; this.recyclers = recyclers; }

    @Override @Transactional
    public Map<String,Object> create(TransactionRequest request, User actor) {
        requireAuthenticated(actor);
        Lot lot = lots.findById(request.getLotId()).orElseThrow(() -> notFound("Lot not found"));
        if (!eligible(lot.getStatus())) throw bad("Transactions require a lot in READY_FOR_HANDOVER or HANDED_OVER status");
        if (!canAccess(lot, actor)) throw forbidden("Only the collector or matched recycler may create this transaction");
        if (transactions.existsByLot_IdAndPaymentStatus(lot.getId(), Transaction.PaymentStatus.PAID)) throw bad("A paid transaction already exists for this lot");
        Transaction transaction = new Transaction();
        transaction.setLot(lot); transaction.setCollector(lot.getCollector()); transaction.setRecycler(lot.getRecycler()); transaction.setAmount(request.getAmount()); transaction.setPaymentMode(request.getPaymentMode()); transaction.setPaymentReference(request.getPaymentReference()); transaction.setNotes(request.getNotes()); transaction.setPaymentStatus(Transaction.PaymentStatus.PENDING); transaction.setCreatedBy(actor.getId());
        return response(transactions.save(transaction));
    }

    @Override @Transactional
    public List<Map<String,Object>> find(User actor, String status, Long lotId) {
        requireAuthenticated(actor);
        List<Transaction> result;
        if ("ADMIN".equalsIgnoreCase(actor.getRole())) result = transactions.findAll();
        else if (isRecycler(actor)) result = transactions.findByRecycler_CreatedByOrderByCreatedAtDesc(actor.getId());
        else result = transactions.findByCollectorOrderByCreatedAtDesc(actor);
        if (status != null) { Transaction.PaymentStatus requested = parseStatus(status); result = result.stream().filter(t -> t.getPaymentStatus() == requested).collect(Collectors.toList()); }
        if (lotId != null) result = result.stream().filter(t -> t.getLot().getId().equals(lotId)).collect(Collectors.toList());
        return result.stream().map(this::response).collect(Collectors.toList());
    }

    @Override @Transactional
    public Map<String,Object> findById(Long id, User actor) {
        requireAuthenticated(actor); Transaction transaction = transactions.findById(id).orElseThrow(() -> notFound("Transaction not found"));
        if (!canAccess(transaction.getLot(), actor)) throw forbidden("You cannot access this transaction");
        return response(transaction);
    }

    @Override @Transactional
    public Map<String,Object> updateStatus(Long id, TransactionStatusRequest request, User actor) {
        requireAuthenticated(actor); Transaction transaction = transactions.findById(id).orElseThrow(() -> notFound("Transaction not found"));
        if (transaction.getPaymentStatus() != Transaction.PaymentStatus.PENDING) throw bad("Only PENDING transactions can change status");
        Transaction.PaymentStatus target = request.getPaymentStatus();
        boolean admin = "ADMIN".equalsIgnoreCase(actor.getRole());
        boolean matchedRecycler = transaction.getRecycler() != null && transaction.getRecycler().getCreatedBy() != null && transaction.getRecycler().getCreatedBy().equals(actor.getId());
        boolean collector = transaction.getCollector().getId().equals(actor.getId());
        if ((target == Transaction.PaymentStatus.PAID || target == Transaction.PaymentStatus.FAILED) && !admin && !matchedRecycler) throw forbidden("Only the matched recycler or admin may set this status");
        if (target == Transaction.PaymentStatus.CANCELLED && !admin && !matchedRecycler && !collector) throw forbidden("Only an involved user may cancel this transaction");
        if (target == Transaction.PaymentStatus.PENDING) throw bad("Transaction is already PENDING");
        if (target == Transaction.PaymentStatus.PAID) {
            if (transactions.existsByLot_IdAndPaymentStatus(transaction.getLot().getId(), Transaction.PaymentStatus.PAID)) throw bad("A paid transaction already exists for this lot");
            Lot lot = transaction.getLot(); lot.setStatus(Lot.Status.PAID); lot.setFinalSaleAmount(transaction.getAmount()); lots.save(lot);
        }
        transaction.setPaymentStatus(target);
        transaction.setTransactionTime(LocalDateTime.now());
        return response(transactions.save(transaction));
    }

    @Override @Transactional
    public Map<String,Object> earnings(User actor) {
        requireAuthenticated(actor);
        // Every authenticated user gets figures scoped to them — no role based 403.
        // The previous "Collector role required" gate rejected real (non-demo)
        // Recycler accounts, which is why recycler logins saw a failing earnings
        // request while the demo profiles (which never call the API) worked fine.
        if (isRecycler(actor)) return recyclerEarnings(actor);
        if (isCollector(actor)) return collectorEarnings(actor);
        return platformEarnings();
    }

    /** Collectors/citizens: what this user has earned from their handovers. */
    private Map<String,Object> collectorEarnings(User actor) {
        // Aggregates run in the database (indexed SUM/COUNT) — only the collector's own rows.
        return earningsResponse(
                transactions.sumAmountByCollectorAndStatus(actor, Transaction.PaymentStatus.PAID),
                transactions.sumAmountByCollectorAndStatus(actor, Transaction.PaymentStatus.PENDING),
                transactions.sumWeightByCollectorAndStatus(actor, Transaction.PaymentStatus.PAID),
                transactions.countByCollectorAndPaymentStatus(actor, Transaction.PaymentStatus.PAID));
    }

    /**
     * Recyclers: the same four figures describe settlement with the platform
     * (amount already paid for received lots, amount still pending, volume
     * received, settled lots), scoped to the recycler profile this user owns.
     */
    private Map<String,Object> recyclerEarnings(User actor) {
        Long recyclerId = recyclers.findFirstByCreatedBy(actor.getId()).map(Recycler::getId).orElse(null);
        if (recyclerId == null) return earningsResponse(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0L);
        return earningsResponse(
                transactions.sumAmountByRecyclerIdAndStatus(recyclerId, Transaction.PaymentStatus.PAID),
                transactions.sumAmountByRecyclerIdAndStatus(recyclerId, Transaction.PaymentStatus.PENDING),
                transactions.sumWeightByRecyclerIdAndStatus(recyclerId, Transaction.PaymentStatus.PAID),
                transactions.countByRecyclerIdAndStatus(recyclerId, Transaction.PaymentStatus.PAID));
    }

    /** Admin console: platform-wide totals. */
    private Map<String,Object> platformEarnings() {
        return earningsResponse(
                transactions.sumAmountByStatus(Transaction.PaymentStatus.PAID),
                transactions.sumAmountByStatus(Transaction.PaymentStatus.PENDING),
                transactions.sumWeightByStatus(Transaction.PaymentStatus.PAID),
                transactions.countByPaymentStatus(Transaction.PaymentStatus.PAID));
    }

    private Map<String,Object> earningsResponse(BigDecimal total, BigDecimal pending, BigDecimal kg, long paid) {
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("total_earnings", total == null ? BigDecimal.ZERO : total);
        result.put("pending_amount", pending == null ? BigDecimal.ZERO : pending);
        result.put("total_kg_collected", kg == null ? BigDecimal.ZERO : kg);
        result.put("completed_handovers_count", paid);
        return result;
    }

    private boolean isCollector(User actor){ return "COLLECTOR".equalsIgnoreCase(actor.getRole()) || "CITIZEN".equalsIgnoreCase(actor.getRole()); }

    private boolean eligible(Lot.Status status){ return status == Lot.Status.READY_FOR_HANDOVER || status == Lot.Status.HANDED_OVER; }
    private boolean isRecycler(User actor){ return "RECYCLER".equalsIgnoreCase(actor.getRole()) || "VERIFIED_RECYCLER".equalsIgnoreCase(actor.getRole()); }
    private boolean canAccess(Lot lot, User actor){ return "ADMIN".equalsIgnoreCase(actor.getRole()) || lot.getCollector().getId().equals(actor.getId()) || (lot.getRecycler() != null && lot.getRecycler().getCreatedBy() != null && lot.getRecycler().getCreatedBy().equals(actor.getId())); }
    private void requireAuthenticated(User actor){ if(actor == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Valid session token required"); }
    private Transaction.PaymentStatus parseStatus(String value){ try{return Transaction.PaymentStatus.valueOf(value.toUpperCase(Locale.ROOT));}catch(IllegalArgumentException e){throw bad("Unknown payment status");} }
    private Map<String,Object> response(Transaction t){Map<String,Object> result=new LinkedHashMap<>();result.put("id",t.getId());result.put("lot_id",t.getLot().getId());result.put("lot_reference",t.getLot().getLotReference());result.put("collector_id",t.getCollector().getId());result.put("recycler_id",t.getRecycler()==null?null:t.getRecycler().getId());result.put("amount",t.getAmount());result.put("payment_mode",t.getPaymentMode());result.put("payment_reference",t.getPaymentReference());result.put("payment_status",t.getPaymentStatus());result.put("notes",t.getNotes());result.put("transaction_time",t.getTransactionTime());result.put("created_by",t.getCreatedBy());result.put("created_at",t.getCreatedAt());result.put("updated_at",t.getUpdatedAt());return result;}
    private ResponseStatusException bad(String m){return new ResponseStatusException(HttpStatus.BAD_REQUEST,m);} private ResponseStatusException forbidden(String m){return new ResponseStatusException(HttpStatus.FORBIDDEN,m);} private ResponseStatusException notFound(String m){return new ResponseStatusException(HttpStatus.NOT_FOUND,m);}
}
