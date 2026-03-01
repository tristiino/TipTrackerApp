package com.tiptracker.backend.service;

import com.tiptracker.backend.dto.ReportSummaryDTO;
import com.tiptracker.backend.dto.TipEntryDTO;
import com.tiptracker.backend.model.TipEntry;
import com.tiptracker.backend.model.User;
import com.tiptracker.backend.repository.TipEntryRepository;
import com.tiptracker.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service handling all business logic related to Tip Entries.
 * This includes CRUD operations and report generation.
 */
@Service
@RequiredArgsConstructor
public class TipEntryService {

    private final TipEntryRepository tipEntryRepository;
    private final UserRepository userRepository;

    private static final double TAX_RATE = 0.03;
    private static final double TIP_SHARE_RATE = 0.10;

    /**
     * Saves a new tip entry and associates it with the currently authenticated user.
     * @param tip The TipEntry object to save.
     * @param userEmail The email of the currently logged-in user.
     * @return The saved TipEntry entity.
     */
    public TipEntry saveTip(TipEntry tip, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + userEmail));
        tip.setUser(user);
        return tipEntryRepository.save(tip);
    }

    /**
     * Updates an existing tip entry by its ID.
     * @param id The ID of the tip to update.
     * @param tipDetails An object containing the new data for the tip.
     * @return The updated TipEntry entity.
     */
    public TipEntry updateTip(Long id, TipEntry tipDetails) {
        TipEntry existingTip = tipEntryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tip entry not found with id: " + id));

        existingTip.setAmount(tipDetails.getAmount());
        existingTip.setDate(tipDetails.getDate());
        existingTip.setShiftType(tipDetails.getShiftType());
        existingTip.setNotes(tipDetails.getNotes());

        return tipEntryRepository.save(existingTip);
    }

    /**
     * Deletes a tip entry by its ID.
     * @param id The ID of the tip to delete.
     */
    public void deleteTip(Long id) {
        if (!tipEntryRepository.existsById(id)) {
            throw new RuntimeException("Tip entry not found with id: " + id);
        }
        tipEntryRepository.deleteById(id);
    }

    /**
     * Generates a financial summary report for the authenticated user (identified by email)
     * within a given date range. The user ID is resolved server-side from the email so that
     * no user-supplied identifier can be used to access another user's data.
     * @param userEmail The email of the authenticated user (derived from the JWT principal).
     * @param start The start date of the report period.
     * @param end The end date of the report period.
     * @return A DTO containing the calculated summary and a list of tip entries.
     */
    public ReportSummaryDTO getReportSummary(String userEmail, LocalDate start, LocalDate end) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + userEmail));
        return getReportSummary(user.getId(), start, end);
    }

    /**
     * Generates a financial summary report for a given user and date range.
     * @param userId The ID of the user for whom the report is generated.
     * @param start The start date of the report period.
     * @param end The end date of the report period.
     * @return A DTO containing the calculated summary and a list of tip entries.
     */
    public ReportSummaryDTO getReportSummary(Long userId, LocalDate start, LocalDate end) {
        List<TipEntry> tips = tipEntryRepository.findByUserIdAndDateBetween(userId, start, end);

        List<TipEntryDTO> tipEntryDTOs = tips.stream().map(tip -> {
            TipEntryDTO dto = new TipEntryDTO();
            dto.setId(tip.getId());
            dto.setAmount(tip.getAmount());
            dto.setDate(tip.getDate());
            dto.setNotes(tip.getNotes());
            dto.setShiftType(tip.getShiftType());
            dto.setTipShare(tip.getAmount() * TIP_SHARE_RATE);
            return dto;
        }).collect(Collectors.toList());

        double totalBeforeTax = tips.stream().mapToDouble(TipEntry::getAmount).sum();
        double tipShare = totalBeforeTax * TIP_SHARE_RATE;
        double grossEarnings = totalBeforeTax - tipShare;
        double taxDeducted = grossEarnings * TAX_RATE;
        double netEarnings = grossEarnings - taxDeducted;

        ReportSummaryDTO summary = new ReportSummaryDTO();
        summary.setTipEntries(tipEntryDTOs);
        summary.setTotalBeforeTax(totalBeforeTax);
        summary.setTotalTipShare(tipShare);
        summary.setGrossEarnings(grossEarnings);
        summary.setTotalTax(taxDeducted);
        summary.setNetEarnings(netEarnings);

        return summary;
    }

    /**
     * Fetches the 7 most recent tip entries for a given user.
     * @param userEmail The email of the user whose recent tips are to be fetched.
     * @return A list of DTOs representing the recent tips.
     */
    public List<TipEntryDTO> getRecentTips(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userEmail));

        List<TipEntry> recentTips = tipEntryRepository.findTop7ByUserOrderByDateDesc(user);

        return recentTips.stream().map(tip -> {
            TipEntryDTO dto = new TipEntryDTO();
            dto.setId(tip.getId());
            dto.setAmount(tip.getAmount());
            dto.setDate(tip.getDate());
            dto.setNotes(tip.getNotes());
            dto.setShiftType(tip.getShiftType());
            dto.setTipShare(tip.getAmount() * TIP_SHARE_RATE);
            return dto;
        }).collect(Collectors.toList());
    }
}
