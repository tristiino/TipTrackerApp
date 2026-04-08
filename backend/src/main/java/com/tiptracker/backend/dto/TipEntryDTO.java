package com.tiptracker.backend.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Data Transfer Object for representing a single tip entry in a report.
 *
 * Phase 2 additions:
 *   tipOutRecords — the per-role deductions applied to this shift
 *   totalTipOut   — sum of all finalAmounts across tipOutRecords
 *   netTips       — amount - totalTipOut (take-home before tax)
 *
 * tipShare is kept for backward compatibility with the CSV export
 * and any frontend code that still reads it. It is populated with
 * the same value as totalTipOut during DTO mapping.
 */
@Data
public class TipEntryDTO {
    private Long id;
    private double amount;
    private Double cashTips;
    private Double creditTips;
    private LocalDate date;
    private String shiftType;
    private String notes;

    /** @deprecated Use totalTipOut. Kept for backward compat with CSV export. */
    private double tipShare;

    private LocalTime startTime;
    private LocalTime endTime;
    private Double hoursWorked;

    // --- Phase 2: Tip-Out Calculator fields ---

    /** The individual tip-out deductions applied to this shift. Empty list if none. */
    private List<TipOutRecordDTO> tipOutRecords = new ArrayList<>();

    /** Sum of finalAmount across all tipOutRecords. 0 if no roles were applied. */
    private double totalTipOut;

    /** Gross tips minus total tip-out. What the server takes home before tax. */
    private double netTips;

    // --- Phase 2 Sprint 2: Multi-Job fields ---

    /** The job this shift was logged against. Null if unassigned. */
    private Long jobId;
    private String jobName;

    // --- Phase 2 Sprint 3: Tags ---

    /** Tags applied to this shift. Empty list if none. P2-014. */
    private List<TagDTO> tags = new ArrayList<>();
}