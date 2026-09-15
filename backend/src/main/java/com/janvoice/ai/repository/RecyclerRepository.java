package com.janvoice.ai.repository;

import com.janvoice.ai.entity.Recycler;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RecyclerRepository extends JpaRepository<Recycler, Long> {
    List<Recycler> findByActiveTrueAndAuthorizationStatusNotOrderByBusinessNameAsc(Recycler.AuthorizationStatus status);
    List<Recycler> findByCityIgnoreCaseAndActiveTrueAndAuthorizationStatusNotOrderByBusinessNameAsc(String city, Recycler.AuthorizationStatus status);
    List<Recycler> findByAcceptedMaterials_IdAndActiveTrueAndAuthorizationStatusNotOrderByBusinessNameAsc(Long materialId, Recycler.AuthorizationStatus status);
    // Profile owned by a user account (used to scope recycler-facing figures).
    Optional<Recycler> findFirstByCreatedBy(Long createdBy);
}
