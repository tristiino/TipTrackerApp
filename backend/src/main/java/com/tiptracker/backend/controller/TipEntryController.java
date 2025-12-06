package com.tiptracker.backend.controller;

import com.tiptracker.backend.dto.ReportSummaryDTO;
import com.tiptracker.backend.dto.TipEntryDTO;
import com.tiptracker.backend.model.TipEntry;
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
     * @param tip The TipEntry object from the request body.
     * @param principal The currently authenticated user, provided by Spring Security.
     * @return The saved TipEntry object.
     */
    @PostMapping
    public TipEntry addTip(@RequestBody TipEntry tip, Principal principal) {
        return service.saveTip(tip, principal.getName());
    }

    /**
     * Generates a report summary for a specific user within a given date range.
     * @param userId The ID of the user for whom the report is generated.
     * @param start The start date of the report period.
     * @param end The end date of the report period.
     * @return A DTO containing the report summary and list of tip entries.
     */
    @GetMapping("/user/{userId}/report")
    public ResponseEntity<ReportSummaryDTO> getReport(
            @PathVariable Long userId,
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        return ResponseEntity.ok(service.getReportSummary(userId, start, end));
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
     * @param id The ID of the tip entry to update.
     * @param tipDetails The updated tip entry data from the request body.
     * @return The updated TipEntry object.
     */
    @PutMapping("/{id}")
    public TipEntry updateTip(@PathVariable Long id, @RequestBody TipEntry tipDetails) {
        return service.updateTip(id, tipDetails);
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