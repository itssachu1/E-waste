package com.janvoice.ai.repository;

import com.janvoice.ai.entity.Lot;
import com.janvoice.ai.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LotRepository extends JpaRepository<Lot, Long> {
    List<Lot> findByCollectorOrderByCreatedAtDesc(User collector);
    List<Lot> findByCollectorAndStatusOrderByCreatedAtDesc(User collector, Lot.Status status);
}
