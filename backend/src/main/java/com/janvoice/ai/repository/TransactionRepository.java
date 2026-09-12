package com.janvoice.ai.repository;

import com.janvoice.ai.entity.Transaction;
import com.janvoice.ai.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByCollectorOrderByCreatedAtDesc(User collector);
    List<Transaction> findByRecycler_CreatedByOrderByCreatedAtDesc(Long userId);
    boolean existsByLot_IdAndPaymentStatus(Long lotId, Transaction.PaymentStatus status);
}
