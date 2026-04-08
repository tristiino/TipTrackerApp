package com.tiptracker.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Data Transfer Object representing aggregated tip earnings for a single day.
 * Used by the daily earnings chart endpoint on the analytics dashboard.
 *
 * Phase 2 addition: grossTips
 *   Enables the gross vs. net toggle on the dashboard chart (P2-006).
 *   grossTips = raw tips before tip-out deductions for this day.
 *   netEarnings now reflects actual tip-out deductions instead of hardcoded 10%.
 */
@Data
@NoArgsConstructor
public class DailyEarningsDTO {
    private LocalDate date;
    private double totalTips;
    private double cashTips;
    private double creditTips;
    private double netEarnings;

    /** Phase 2: gross tips before tip-out deductions for this day. */
    private double grossTips;

    /** Full constructor including Phase 2 grossTips field. */
    public DailyEarningsDTO(LocalDate date, double totalTips, double cashTips,
                            double creditTips, double netEarnings, double grossTips) {
        this.date = date;
        this.totalTips = totalTips;
        this.cashTips = cashTips;
        this.creditTips = creditTips;
        this.netEarnings = netEarnings;
        this.grossTips = grossTips;
    }
}
