package com.janvoice.ai.repository;

import com.janvoice.ai.entity.PriceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PriceRecordRepository extends JpaRepository<PriceRecord, Long> {
    List<PriceRecord> findAllByOrderByQuotedAtDesc();
    List<PriceRecord> findByMaterial_IdOrderByQuotedAtDesc(Long materialId);
    List<PriceRecord> findByMaterial_IdAndLocationIgnoreCaseOrderByQuotedAtDesc(Long materialId, String location);
    List<PriceRecord> findByLocationIgnoreCaseOrderByQuotedAtDesc(String location);
}
