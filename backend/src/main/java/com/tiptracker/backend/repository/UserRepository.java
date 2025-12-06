package com.tiptracker.backend.repository;

import com.tiptracker.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for the User entity.
 * Handles all database operations for users.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by their unique email address.
     * @param email The email address to search for.
     * @return An Optional containing the found User, or an empty Optional if not found.
     */
    Optional<User> findByEmail(String email);
}