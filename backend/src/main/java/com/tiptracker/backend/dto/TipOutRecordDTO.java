package com.tiptracker.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for TipOutRecord.
 * Represents a single tip-out deduction on a specific shift.
 *
 * Sent to the client so it can:
 *  - Display per-role breakdown (roleName, finalAmount)
 *  - Show override indicator (isOverridden)
 *  - Allow user to submit an override (via PATCH endpoint using id)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TipOutRecordDTO {

    private Long id;
    private Long roleId;           // which template was applied (nullable)
    private String roleName;       // snapshot label for display
    private double computedAmount; // what the formula produced
    private double finalAmount;    // what actually gets deducted
    private boolean isOverridden;  // true if user changed finalAmount
}
