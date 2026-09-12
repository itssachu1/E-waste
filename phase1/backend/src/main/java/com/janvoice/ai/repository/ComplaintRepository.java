package com.janvoice.ai.repository;

import com.janvoice.ai.entity.Complaint;
import com.janvoice.ai.entity.ComplaintStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA Repository interface for 'Complaint' entity database
 * operations.
 * Holds retrieval logic for main parent complaints and analytics.
 */
@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    // Fetch all parent complaints (master issues) in an area
    List<Complaint> findByWardAreaAndParentComplaintIsNullOrderByPriorityScoreDesc(String wardArea);

    // Fetch all parent complaints sorted globally or by area for sorting queues
    List<Complaint> findByParentComplaintIsNullOrderByPriorityScoreDesc();

    // Check status counts in an area for dashboard
    long countByWardAreaAndStatus(String wardArea, ComplaintStatus status);

    // Fetch child duplicates connected to a master complaint
    List<Complaint> findByParentComplaintId(Long parentId);

    // Group complaints count by Category in a ward area (returns Object array:
    // [category_name, count])
    @Query("SELECT c.category, COUNT(c) FROM Complaint c WHERE c.wardArea = :wardArea AND c.parentComplaint IS NULL GROUP BY c.category")
    List<Object[]> countByCategoryInWardArea(@Param("wardArea") String wardArea);

    // Find master complaints of a specific category in an area for similarity
    // comparisons
    List<Complaint> findByWardAreaAndCategoryAndParentComplaintIsNull(String wardArea, String category);

    // Search complaints in an area
    @Query("SELECT c FROM Complaint c WHERE c.wardArea = :wardArea AND c.parentComplaint IS NULL AND " +
            "(c.originalText LIKE %:query% OR c.translatedText LIKE %:query% OR c.category LIKE %:query%)")
    List<Complaint> searchComplaints(@Param("wardArea") String wardArea, @Param("query") String query);
}
