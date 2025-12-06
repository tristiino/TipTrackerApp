package com.tiptracker.backend.dto;

import lombok.Data;
import java.util.List;

/**
 * Data Transfer Object for sending a complete, calculated report summary to the client.
 */
@Data
public class ReportSummaryDTO {
    private double totalBeforeTax;
    private double totalTax;
    private double totalTipShare;
    private double grossEarnings;
    private double netEarnings;
    private List<TipEntryDTO> tipEntries;
}