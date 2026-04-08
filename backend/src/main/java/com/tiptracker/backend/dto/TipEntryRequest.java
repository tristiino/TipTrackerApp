package com.tiptracker.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Request body for POST /api/tips and PUT /api/tips/{id}.
 *
 * Why a wrapper instead of the raw TipEntry entity?
 * The JPA entity (TipEntry) shouldn't carry API-only fields like tipOutRoleIds.
 * Mixing those concerns makes the entity harder to maintain and can cause
 * unexpected behavior during serialization/deserialization.
 *
 * This DTO carries all the data the client sends when logging or editing a shift.
 */
@Data
@NoArgsConstructor
public class TipEntryRequest {

    // --- Core shift fields ---
    private Double cashTips;
    private Double creditTips;
    private double amount;        // used as fallback if cash/credit are null
    private LocalDate date;
    private String shiftType;
    private String notes;
    private LocalTime startTime;
    private LocalTime endTime;
    private Double hoursWorked;

    /**
     * The IDs of the tip-out role templates the user selected on the form.
     * Empty = no tip-outs applied to this shift.
     */
    private List<Long> tipOutRoleIds = new ArrayList<>();

    /** Optional job profile this shift belongs to. Null = unassigned. */
    private Long jobId;

    /**
     * IDs of tags to apply to this shift. P2-014.
     * Empty = no tags. Tags must belong to the authenticated user.
     */
    private List<Long> tagIds = new ArrayList<>();
}
