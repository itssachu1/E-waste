package com.janvoice.ai.repository;

import com.janvoice.ai.entity.AreaBriefing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA Repository interface for 'AreaBriefing' caching database
 * operations.
 */
@Repository
public interface AreaBriefingRepository extends JpaRepository<AreaBriefing, String> {
}
