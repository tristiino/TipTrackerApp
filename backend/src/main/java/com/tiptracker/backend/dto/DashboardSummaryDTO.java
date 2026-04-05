package com.tiptracker.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Aggregated summary stats for the analytics dashboard.
 * Covers a rolling N-day window for the authenticated user.
 *
 * Phase 2 addition: grossTips
 *   Previously totalTips and grossTips were the same thing (no tip-outs existed).
 *   Now: grossTips = raw tips received; totalTips stays for backward compat.
 *   netEarnings now reflects actual tip-out deductions instead of hardcoded 10%.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardSummaryDTO {
    private double totalTips;
    private double netEarnings;
    private int    shiftsWorked;
    private double avgTipsPerShift;
    private double totalHoursWorked;
    private double estimatedHourlyWage; // 0 if no hours logged

    /** Phase 2: gross tips before tip-out deductions. Enables gross vs. net toggle (P2-006). */
    private double grossTips;

    /** Phase 2: total tip-out deducted this period. Drives the dashboard breakdown card. */
    private double totalTipOut;
}
