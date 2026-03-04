package com.tiptracker.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Data Transfer Object representing aggregated tip earnings for a single day.
 * Used by the daily earnings chart endpoint on the analytics dashboard.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DailyEarningsDTO {
    private LocalDate date;
    private double totalTips;
    private double cashTips;
    private double creditTips;
    private double netEarnings;
}
