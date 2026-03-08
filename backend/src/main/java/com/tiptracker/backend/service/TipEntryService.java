package com.tiptracker.backend.service;

import com.tiptracker.backend.dto.DailyEarningsDTO;
import com.tiptracker.backend.dto.DashboardSummaryDTO;
import com.tiptracker.backend.dto.ReportSummaryDTO;
import com.tiptracker.backend.dto.TipEntryDTO;
import com.tiptracker.backend.model.TipEntry;
import com.tiptracker.backend.model.User;
import com.tiptracker.backend.repository.TipEntryRepository;
import com.tiptracker.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    // Validate cash/credit fields and compute total amount
    double cash = tip.getCashTips() != null ? tip.getCashTips() : 0.0;
    double credit = tip.getCreditTips() != null ? tip.getCreditTips() : 0.0;

    if (cash < 0 || credit < 0) {
        throw new IllegalArgumentException("Tip amounts cannot be negative.");
    }

    // Calculate total amount
    if (tip.getCashTips() != null || tip.getCreditTips() != null) {
        tip.setAmount(cash + credit);
    }

    // Calculate hours worked
    if (tip.getStartTime() != null && tip.getEndTime() != null) {
        long minutes = java.time.Duration.between(tip.getStartTime(), tip.getEndTime()).toMinutes();
        tip.setHoursWorked(minutes / 60.0);
    }

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
        existingTip.setCashTips(tipDetails.getCashTips());
        existingTip.setCreditTips(tipDetails.getCreditTips());
        existingTip.setStartTime(tipDetails.getStartTime());
        existingTip.setEndTime(tipDetails.getEndTime());
        existingTip.setHoursWorked(tipDetails.getHoursWorked());


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
            dto.setCashTips(tip.getCashTips());
            dto.setCreditTips(tip.getCreditTips());
            dto.setStartTime(tip.getStartTime());
            dto.setEndTime(tip.getEndTime());
            dto.setHoursWorked(tip.getHoursWorked());
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
     * Returns aggregated tip earnings for the last N days, grouped by day/week/month.
     * Uses a DB-level aggregate query for performance with large datasets.
     * @param userEmail The email of the authenticated user.
     * @param days      The number of days to look back (e.g. 30).
     * @param groupBy   Aggregation period: "day", "week", or "month".
     * @return A list of DailyEarningsDTO sorted ascending by period start date.
     */
    public List<DailyEarningsDTO> getDailyEarnings(String userEmail, int days, String groupBy) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + userEmail));

        LocalDate end   = LocalDate.now();
        LocalDate start = end.minusDays(days - 1);

        // Fetch per-day aggregates from DB (no full entity load)
        List<Object[]> rows = tipEntryRepository.findDailyAggregates(user.getId(), start, end);

        // Build date → DTO map from aggregate rows
        Map<LocalDate, DailyEarningsDTO> dailyMap = new LinkedHashMap<>();
        for (Object[] row : rows) {
            LocalDate date  = (LocalDate) row[0];
            double total    = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
            double cash     = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
            double credit   = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;
            double net      = computeNet(total);
            dailyMap.put(date, new DailyEarningsDTO(date, total, cash, credit, net));
        }

        if ("week".equals(groupBy)) {
            return aggregateByWeek(start, end, dailyMap);
        } else if ("month".equals(groupBy)) {
            return aggregateByMonth(start, end, dailyMap);
        } else {
            // Daily: fill every day (zeros for days with no tips)
            List<DailyEarningsDTO> result = new ArrayList<>();
            for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                result.add(dailyMap.getOrDefault(d, new DailyEarningsDTO(d, 0, 0, 0, 0)));
            }
            return result;
        }
    }

    private double computeNet(double total) {
        double gross = total - (total * TIP_SHARE_RATE);
        return gross - (gross * TAX_RATE);
    }

    private List<DailyEarningsDTO> aggregateByWeek(LocalDate start, LocalDate end,
                                                    Map<LocalDate, DailyEarningsDTO> dailyMap) {
        LocalDate weekStart = start.with(DayOfWeek.MONDAY);
        List<DailyEarningsDTO> result = new ArrayList<>();
        for (LocalDate ws = weekStart; !ws.isAfter(end); ws = ws.plusWeeks(1)) {
            double total = 0, cash = 0, credit = 0;
            for (LocalDate d = ws; !d.isAfter(ws.plusDays(6)) && !d.isAfter(end); d = d.plusDays(1)) {
                DailyEarningsDTO day = dailyMap.get(d);
                if (day != null) { total += day.getTotalTips(); cash += day.getCashTips(); credit += day.getCreditTips(); }
            }
            result.add(new DailyEarningsDTO(ws, total, cash, credit, computeNet(total)));
        }
        return result;
    }

    private List<DailyEarningsDTO> aggregateByMonth(LocalDate start, LocalDate end,
                                                     Map<LocalDate, DailyEarningsDTO> dailyMap) {
        LocalDate monthStart = start.withDayOfMonth(1);
        List<DailyEarningsDTO> result = new ArrayList<>();
        for (LocalDate ms = monthStart; !ms.isAfter(end); ms = ms.plusMonths(1)) {
            LocalDate me = ms.plusMonths(1).minusDays(1);
            double total = 0, cash = 0, credit = 0;
            for (LocalDate d = ms; !d.isAfter(me) && !d.isAfter(end); d = d.plusDays(1)) {
                DailyEarningsDTO day = dailyMap.get(d);
                if (day != null) { total += day.getTotalTips(); cash += day.getCashTips(); credit += day.getCreditTips(); }
            }
            result.add(new DailyEarningsDTO(ms, total, cash, credit, computeNet(total)));
        }
        return result;
    }

    /**
     * Returns aggregated summary stats for the dashboard over a rolling N-day window.
     * @param userEmail The email of the authenticated user.
     * @param days      The number of days to look back.
     * @return A DashboardSummaryDTO with totals, shift count, and hourly wage estimate.
     */
    public DashboardSummaryDTO getDashboardSummary(String userEmail, int days) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + userEmail));

        LocalDate end   = LocalDate.now();
        LocalDate start = end.minusDays(days - 1);
        List<TipEntry> tips = tipEntryRepository.findByUserIdAndDateBetween(user.getId(), start, end);

        double totalTips    = tips.stream().mapToDouble(TipEntry::getAmount).sum();
        double tipShare     = totalTips * TIP_SHARE_RATE;
        double gross        = totalTips - tipShare;
        double netEarnings  = gross - (gross * TAX_RATE);
        int    shifts       = tips.size();
        double avgPerShift  = shifts > 0 ? totalTips / shifts : 0.0;

        double totalHours   = tips.stream()
                .filter(t -> t.getHoursWorked() != null && t.getHoursWorked() > 0)
                .mapToDouble(TipEntry::getHoursWorked)
                .sum();
        double hourlyWage   = totalHours > 0 ? totalTips / totalHours : 0.0;

        return new DashboardSummaryDTO(totalTips, netEarnings, shifts, avgPerShift, totalHours, hourlyWage);
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
            dto.setCashTips(tip.getCashTips());
            dto.setCreditTips(tip.getCreditTips());
            dto.setStartTime(tip.getStartTime());
            dto.setEndTime(tip.getEndTime());
            dto.setHoursWorked(tip.getHoursWorked());
            dto.setTipShare(tip.getAmount() * TIP_SHARE_RATE);
            return dto;
        }).collect(Collectors.toList());
    }
}
