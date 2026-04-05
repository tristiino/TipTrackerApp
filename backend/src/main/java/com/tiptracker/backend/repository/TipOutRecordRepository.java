package com.tiptracker.backend.repository;

import com.tiptracker.backend.model.TipEntry;
import com.tiptracker.backend.model.TipOutRecord;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TipOutRecordRepository extends JpaRepository<TipOutRecord, Long> {

    /** All tip-out records for a single shift. Used when building TipEntryDTO. */
    List<TipOutRecord> findByTipEntry(TipEntry tipEntry);

    /**
     * Deletes all tip-out records for a shift.
     * Called when a shift is updated so we can re-apply the new role selection.
     */
    @Modifying
    void deleteByTipEntry(TipEntry tipEntry);

    /**
     * Sums all finalAmounts for a user's shifts within a date range.
     * Used by getDashboardSummary and getReportSummary to compute
     * period-level totals without loading every individual record.
     */
    @Query("SELECT COALESCE(SUM(r.finalAmount), 0) FROM TipOutRecord r " +
           "WHERE r.tipEntry.user.id = :userId " +
           "AND r.tipEntry.date BETWEEN :start AND :end")
    double sumFinalAmountByUserAndDateRange(
            @Param("userId") Long userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    /**
     * Aggregates tip-out totals per day within a date range.
     * Paired with the existing daily earnings query in TipEntryService
     * so getDailyEarnings can show accurate per-day net figures.
     *
     * Returns List<Object[]> where [0]=LocalDate, [1]=Double (sum of finalAmounts)
     */
    @Query("SELECT r.tipEntry.date, COALESCE(SUM(r.finalAmount), 0) " +
           "FROM TipOutRecord r " +
           "WHERE r.tipEntry.user.id = :userId " +
           "AND r.tipEntry.date BETWEEN :start AND :end " +
           "GROUP BY r.tipEntry.date " +
           "ORDER BY r.tipEntry.date ASC")
    List<Object[]> findDailyTipOutAggregates(
            @Param("userId") Long userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);
}
