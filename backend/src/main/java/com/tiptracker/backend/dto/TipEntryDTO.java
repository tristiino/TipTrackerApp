package com.tiptracker.backend.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Data Transfer Object for representing a single tip entry in a report.
 * Includes a calculated tipShare field not present in the database entity.
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
    private double tipShare;
    private LocalTime startTime;
    private LocalTime endTime;
    private Double hoursWorked;

}