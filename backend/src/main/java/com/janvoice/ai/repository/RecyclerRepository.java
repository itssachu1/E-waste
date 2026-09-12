package com.janvoice.ai.repository;

import com.janvoice.ai.entity.Recycler;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RecyclerRepository extends JpaRepository<Recycler, Long> {
    List<Recycler> findByActiveTrueAndAuthorizationStatusNotOrderByBusinessNameAsc(Recycler.AuthorizationStatus status);
    List<Recycler> findByCityIgnoreCaseAndActiveTrueAndAuthorizationStatusNotOrderByBusinessNameAsc(String city, Recycler.AuthorizationStatus status);
    List<Recycler> findByAcceptedMaterials_IdAndActiveTrueAndAuthorizationStatusNotOrderByBusinessNameAsc(Long materialId, Recycler.AuthorizationStatus status);
}
