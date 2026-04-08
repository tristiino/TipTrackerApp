package com.tiptracker.backend.dto;

import lombok.Data;
import java.util.List;

/**
 * Data Transfer Object for sending a complete, calculated report summary to the client.
 *
 * Phase 2 addition: totalTipOut
 *   Sum of all TipOutRecord.finalAmount values across the report period.
 *   totalTipShare is kept populated for backward compat with the CSV export.
 */
@Data
public class ReportSummaryDTO {
    private double totalBeforeTax;
    private double totalTax;

    /** @deprecated Use totalTipOut. Kept for backward compat with CSV export. */
    private double totalTipShare;

    private double grossEarnings;
    private double netEarnings;
    private List<TipEntryDTO> tipEntries;

    /** Phase 2: sum of actual tip-out deductions across all shifts in the report. */
    private double totalTipOut;
}