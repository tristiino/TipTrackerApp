package com.tiptracker.backend.repository;

import com.tiptracker.backend.model.TipEntry;
import com.tiptracker.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Spring Data JPA repository for the TipEntry entity.
 * Handles all database operations for tip entries.
 */
public interface TipEntryRepository extends JpaRepository<TipEntry, Long> {

    /**
     * Finds all tip entries for a specific user within a given date range.
     * @param userId The ID of the user.
     * @param start The start date of the period.
     * @param end The end date of the period.
     * @return A list of TipEntry entities matching the criteria.
     */
    List<TipEntry> findByUserIdAndDateBetween(Long userId, LocalDate start, LocalDate end);

    /**
     * Finds the 7 most recent tip entries for a specific user, ordered by date descending.
     * @param user The User entity to find tips for.
     * @return A list of the 7 most recent TipEntry entities.
     */
    List<TipEntry> findTop7ByUserOrderByDateDesc(User user);
}