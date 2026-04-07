package com.tiptracker.backend.controller;

import com.tiptracker.backend.dto.DailyEarningsDTO;
import com.tiptracker.backend.dto.DashboardSummaryDTO;
import com.tiptracker.backend.dto.ReportSummaryDTO;
import com.tiptracker.backend.dto.TipEntryDTO;
import com.tiptracker.backend.dto.TipEntryRequest;
import com.tiptracker.backend.service.TipEntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

/**
 * Controller for handling all Tip Entry related operations, including
 * CRUD actions and generating reports.
 */
@RestController
@RequestMapping("/api/tips")
@RequiredArgsConstructor
public class TipEntryController {

    private final TipEntryService service;

    /**
     * Creates a new tip entry for the currently authenticated user.
     * Accepts TipEntryRequest (includes tipOutRoleIds) and returns TipEntryDTO
     * (includes the resolved tip-out records and net tips).
     */
    @PostMapping
    public ResponseEntity<TipEntryDTO> addTip(@RequestBody TipEntryRequest request, Principal principal) {
        return ResponseEntity.status(201).body(service.saveTip(request, principal.getName()));
    }

    /**
     * Generates a report summary for the currently authenticated user within a given date range.
     * The {userId} path variable is kept for URL compatibility but is intentionally ignored —
     * the user's identity is always derived from the authenticated JWT principal to prevent IDOR.
     * @param start The start date of the report period.
     * @param end The end date of the report period.
     * @param principal The currently authenticated user, provided by Spring Security.
     * @return A DTO containing the report summary and list of tip entries.
     */
    @GetMapping("/user/{userId}/report")
    public ResponseEntity<ReportSummaryDTO> getReport(
            @PathVariable Long userId,
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            Principal principal
    ) {
        return ResponseEntity.ok(service.getReportSummary(principal.getName(), start, end));
    }

    /**
     * Returns aggregated summary stats (totals, shift count, hourly wage) for the dashboard.
     * Supply either {@code days} (rolling window from today) or {@code startDate}/{@code endDate}
     * (explicit range). When both are absent, defaults to the last 30 days.
     * @param days      Number of days to look back (optional).
     * @param startDate Start of explicit date range (optional, ISO date).
     * @param endDate   End of explicit date range (optional, ISO date).
     * @param principal The currently authenticated user.
     * @return A DashboardSummaryDTO.
     */
    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryDTO> getDashboardSummary(
            @RequestParam(required = false) Integer days,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long jobId,
            Principal principal) {
        return ResponseEntity.ok(service.getDashboardSummary(principal.getName(), days, startDate, endDate, jobId));
    }

    /**
     * Returns daily aggregated tip earnings for the authenticated user.
     * Supply either {@code days} (rolling window from today) or {@code startDate}/{@code endDate}
     * (explicit range). When both are absent, defaults to the last 30 days.
     * @param days      Number of days to look back (optional).
     * @param groupBy   Aggregation period: "day", "week", or "month".
     * @param startDate Start of explicit date range (optional, ISO date).
     * @param endDate   End of explicit date range (optional, ISO date).
     * @param principal The currently authenticated user.
     * @return A list of DailyEarningsDTO sorted ascending by date.
     */
    @GetMapping("/earnings/daily")
    public ResponseEntity<List<DailyEarningsDTO>> getDailyEarnings(
            @RequestParam(required = false) Integer days,
            @RequestParam(defaultValue = "day") String groupBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long jobId,
            Principal principal) {
        return ResponseEntity.ok(service.getDailyEarnings(principal.getName(), days, groupBy, startDate, endDate, jobId));
    }

    /**
     * Fetches the most recent tip entries for the currently authenticated user.
     * @param principal The currently authenticated user.
     * @return A list of the most recent TipEntryDTOs.
     */
    @GetMapping("/recent")
    public List<TipEntryDTO> getRecentTips(Principal principal) {
        return service.getRecentTips(principal.getName());
    }

    /**
     * Updates an existing tip entry.
     * Clears and re-applies tip-out records based on the new tipOutRoleIds selection.
     * Returns TipEntryDTO with the updated tip-out breakdown.
     */
    @PutMapping("/{id}")
    public ResponseEntity<TipEntryDTO> updateTip(@PathVariable Long id,
                                                  @RequestBody TipEntryRequest request) {
        return ResponseEntity.ok(service.updateTip(id, request));
    }

    /**
     * Deletes a specific tip entry by its ID.
     * @param id The ID of the tip entry to delete.
     * @return An HTTP 204 No Content response on successful deletion.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTip(@PathVariable Long id) {
        service.deleteTip(id);
        return ResponseEntity.noContent().build();
    }
}
