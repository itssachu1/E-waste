package com.janvoice.ai.repository;

import com.janvoice.ai.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * Spring Data JPA Repository interface for 'User' entity database operations.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Retrieve user database credentials by username
    Optional<User> findByUsername(String username);

    // Check if usernames match existing accounts
    boolean existsByUsername(String username);
}
