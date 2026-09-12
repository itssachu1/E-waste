package com.janvoice.ai.repository;

import com.janvoice.ai.entity.Complaint;
import com.janvoice.ai.entity.Upvote;
import com.janvoice.ai.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * Spring Data JPA Repository interface for 'Upvote' entity database operations.
 */
@Repository
public interface UpvoteRepository extends JpaRepository<Upvote, Long> {

    // Retrieve upvote details by matching voter and targeted complaint
    Optional<Upvote> findByUserAndComplaint(User user, Complaint complaint);

    // Validate if user has already upvoted the target complaint
    boolean existsByUserAndComplaint(User user, Complaint complaint);
}
