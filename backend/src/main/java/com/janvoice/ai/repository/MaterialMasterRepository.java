package com.janvoice.ai.repository;

import com.janvoice.ai.entity.MaterialMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MaterialMasterRepository extends JpaRepository<MaterialMaster, Long> {

    List<MaterialMaster> findByActiveTrueOrderByCategoryAscCommonNameAsc();

    List<MaterialMaster> findByActiveTrueAndCategoryIgnoreCaseOrderByCommonNameAsc(String category);

        @Query("select material from MaterialMaster material "
          + "where material.active = true "
          + "and (lower(material.commonName) like lower(concat('%', :search, '%')) "
          + "or lower(material.deviceType) like lower(concat('%', :search, '%')) "
          + "or lower(material.subcategory) like lower(concat('%', :search, '%'))) "
          + "order by material.category asc, material.commonName asc")
    List<MaterialMaster> searchActive(@Param("search") String search);
}
