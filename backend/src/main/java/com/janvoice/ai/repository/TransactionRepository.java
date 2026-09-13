package com.janvoice.ai.repository;

import com.janvoice.ai.entity.Transaction;
import com.janvoice.ai.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByCollectorOrderByCreatedAtDesc(User collector);
    List<Transaction> findByRecycler_CreatedByOrderByCreatedAtDesc(Long userId);
    boolean existsByLot_IdAndPaymentStatus(Long lotId, Transaction.PaymentStatus status);
    long countByCollectorAndPaymentStatus(User collector, Transaction.PaymentStatus status);

    // Bulk aggregates computed in the database (uses idx_transactions_collector /
    // idx_transactions_payment_status) — never loaded into memory.
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.collector = :collector AND t.paymentStatus = :status")
    BigDecimal sumAmountByCollectorAndStatus(@Param("collector") User collector, @Param("status") Transaction.PaymentStatus status);

    @Query("SELECT COALESCE(SUM(COALESCE(t.lot.finalWeight, t.lot.approximateWeight)), 0) FROM Transaction t WHERE t.collector = :collector AND t.paymentStatus = :status")
    BigDecimal sumWeightByCollectorAndStatus(@Param("collector") User collector, @Param("status") Transaction.PaymentStatus status);

    // Recent real transaction records for a collector, server-side limited.
    // EntityGraph fetches the lot association with the transactions (join fetch),
    // so rendering recent rows never triggers N+1 lazy loads.
    @EntityGraph(attributePaths = {"lot"})
    List<Transaction> findByCollectorOrderByCreatedAtDesc(User collector, Pageable pageable);
}
