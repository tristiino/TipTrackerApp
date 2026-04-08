package com.tiptracker.backend.repository;

import com.tiptracker.backend.model.TipEntry;
import com.tiptracker.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * Spring Data JPA repository for the TipEntry entity.
 * Handles all database operations for tip entries.
 */
public interface TipEntryRepository extends JpaRepository<TipEntry, Long> {

    /**
     * Finds all tip entries for a specific user within a given date range.
     * Used for reports where full entity data is needed.
     */
    List<TipEntry> findByUserIdAndDateBetween(Long userId, LocalDate start, LocalDate end);

    /**
     * Returns per-day aggregated totals without loading full entities.
     * Each row: [date, totalTips, cashTips, creditTips, hoursWorked].
     * Used by dashboard endpoints for performance with large datasets.
     */
    @Query("SELECT t.date, SUM(t.amount), " +
           "SUM(COALESCE(t.cashTips, 0.0)), SUM(COALESCE(t.creditTips, 0.0)), " +
           "SUM(COALESCE(t.hoursWorked, 0.0)) " +
           "FROM TipEntry t " +
           "WHERE t.user.id = :userId AND t.date BETWEEN :start AND :end " +
           "GROUP BY t.date ORDER BY t.date ASC")
    List<Object[]> findDailyAggregates(
            @Param("userId") Long userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    /** Finds entries for a specific user, date range, and job. */
    List<TipEntry> findByUserIdAndDateBetweenAndJobId(Long userId, LocalDate start, LocalDate end, Long jobId);

    /**
     * Per-day aggregates filtered by job.
     * Used by getDailyEarnings when a jobId filter is active.
     */
    @Query("SELECT t.date, SUM(t.amount), " +
           "SUM(COALESCE(t.cashTips, 0.0)), SUM(COALESCE(t.creditTips, 0.0)), " +
           "SUM(COALESCE(t.hoursWorked, 0.0)) " +
           "FROM TipEntry t " +
           "WHERE t.user.id = :userId AND t.date BETWEEN :start AND :end AND t.job.id = :jobId " +
           "GROUP BY t.date ORDER BY t.date ASC")
    List<Object[]> findDailyAggregatesForJob(
            @Param("userId") Long userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            @Param("jobId") Long jobId);

    /**
     * Finds entries for a specific user, date range, and tag.
     * Used for tag-filtered dashboard and analytics.
     */
    @Query("SELECT t FROM TipEntry t JOIN t.tags tag " +
           "WHERE t.user.id = :userId AND t.date BETWEEN :start AND :end AND tag.id = :tagId")
    List<TipEntry> findByUserIdAndDateBetweenAndTagId(
            @Param("userId") Long userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            @Param("tagId") Long tagId);

    /**
     * Finds the 7 most recent tip entries for a specific user, ordered by date descending.
     */
    List<TipEntry> findTop7ByUserOrderByDateDesc(User user);
}
