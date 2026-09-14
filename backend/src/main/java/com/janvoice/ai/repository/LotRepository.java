package com.janvoice.ai.repository;

import com.janvoice.ai.entity.Lot;
import com.janvoice.ai.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface LotRepository extends JpaRepository<Lot, Long> {
    List<Lot> findByCollectorOrderByCreatedAtDesc(User collector);
    List<Lot> findByCollectorAndStatusOrderByCreatedAtDesc(User collector, Lot.Status status);

    // Lots matched to a recycler profile (recycler handover inbox).
    List<Lot> findByRecycler_CreatedByOrderByCreatedAtDesc(Long createdBy);

    // Recycler-scoped aggregate counts (for recycler dashboard).
    @Query("SELECT COUNT(l) FROM Lot l WHERE l.recycler.id = :recyclerId AND l.status IN :statuses")
    long countByRecyclerIdAndStatusIn(@Param("recyclerId") Long recyclerId, @Param("statuses") Collection<Lot.Status> statuses);

    @Query("SELECT COUNT(l) FROM Lot l WHERE l.recycler.id = :recyclerId AND l.status = :status")
    long countByRecyclerIdAndStatus(@Param("recyclerId") Long recyclerId, @Param("status") Lot.Status status);

    long countByCollector(User collector);

    // Aggregate count over a bounded status set (uses idx_lots_collector / idx_lots_status).
    @Query("SELECT COUNT(l) FROM Lot l WHERE l.collector = :collector AND l.status IN :statuses")
    long countByCollectorAndStatusIn(@Param("collector") User collector, @Param("statuses") Collection<Lot.Status> statuses);

    // Recent real lot records for a collector, server-side limited (no N+1).
    // EntityGraph fetches the material association with the lots (join fetch),
    // so rendering recent rows never triggers N+1 lazy loads.
    @EntityGraph(attributePaths = {"material"})
    List<Lot> findByCollectorOrderByCreatedAtDesc(User collector, Pageable pageable);
}
