package com.tiptracker.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Aggregated summary stats for the analytics dashboard.
 * Covers a rolling N-day window for the authenticated user.
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
}
