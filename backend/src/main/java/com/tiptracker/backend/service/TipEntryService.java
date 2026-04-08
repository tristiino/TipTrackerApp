package com.tiptracker.backend.service;

import com.tiptracker.backend.dto.*;
import com.tiptracker.backend.model.Job;
import com.tiptracker.backend.model.Tag;
import com.tiptracker.backend.model.TipEntry;
import com.tiptracker.backend.model.TipOutRecord;
import com.tiptracker.backend.model.User;
import com.tiptracker.backend.repository.JobRepository;
import com.tiptracker.backend.repository.TagRepository;
import com.tiptracker.backend.repository.TipEntryRepository;
import com.tiptracker.backend.repository.TipOutRecordRepository;
import com.tiptracker.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service handling all business logic related to Tip Entries.
 * This includes CRUD operations and report/dashboard generation.
 *
 * Phase 2 changes:
 * - TIP_SHARE_RATE (hardcoded 10%) removed. Deductions now come from real
 *   TipOutRecord rows, which users configure via TipOutService.
 * - saveTip / updateTip now accept TipEntryRequest (includes tipOutRoleIds).
 * - computeNet now takes the actual tip-out total instead of a flat rate.
 * - getDailyEarnings, getDashboardSummary, getReportSummary, getRecentTips
 *   all populate the new grossTips / totalTipOut / netTips DTO fields.
 */
@Service
@RequiredArgsConstructor
public class TipEntryService {

    private final TipEntryRepository tipEntryRepository;
    private final TipOutRecordRepository tipOutRecordRepository;
    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final TagRepository tagRepository;
    private final SettingsService settingsService;
    private final TipOutService tipOutService;
    private final TagService tagService;

    private static final double DEFAULT_TAX_RATE = 0.03;

    // -------------------------------------------------------------------------
    // CRUD
    // -------------------------------------------------------------------------

    /**
     * Saves a new tip entry for the authenticated user.
     * After saving, applies any selected tip-out roles and returns a full DTO
     * (including the resolved tip-out records) rather than the raw entity.
     */
    @Transactional
    public TipEntryDTO saveTip(TipEntryRequest request, String userEmail) {
        User user = resolveUser(userEmail);

        TipEntry tip = new TipEntry();
        tip.setUser(user);
        populateTipFromRequest(tip, request);
        TipEntry saved = tipEntryRepository.save(tip);

        // Apply tip-out roles if any were selected (P2-002)
        List<TipOutRecordDTO> records = tipOutService.applyRolesToEntry(saved, request.getTipOutRoleIds());

        return toDTO(saved, records);
    }

    /**
     * Updates an existing tip entry.
     * Clears and re-applies tip-out records so the user's new role selection
     * is always reflected accurately.
     */
    @Transactional
    public TipEntryDTO updateTip(Long id, TipEntryRequest request) {
        TipEntry existing = tipEntryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tip entry not found with id: " + id));

        populateTipFromRequest(existing, request);
        TipEntry saved = tipEntryRepository.save(existing);

        // Re-apply roles: clear old records, then apply the new selection
        tipOutRecordRepository.deleteByTipEntry(saved);
        List<TipOutRecordDTO> records = tipOutService.applyRolesToEntry(saved, request.getTipOutRoleIds());

        return toDTO(saved, records);
    }

    /**
     * Deletes a tip entry (cascades to its TipOutRecord children automatically).
     */
    public void deleteTip(Long id) {
        if (!tipEntryRepository.existsById(id)) {
            throw new RuntimeException("Tip entry not found with id: " + id);
        }
        tipEntryRepository.deleteById(id);
    }

    // -------------------------------------------------------------------------
    // Report summary
    // -------------------------------------------------------------------------

    public ReportSummaryDTO getReportSummary(String userEmail, LocalDate start, LocalDate end) {
        User user = resolveUser(userEmail);
        double taxRate = getUserTaxRate(userEmail);
        return buildReportSummary(user.getId(), start, end, taxRate);
    }

    private ReportSummaryDTO buildReportSummary(Long userId, LocalDate start, LocalDate end, double taxRate) {
        List<TipEntry> tips = tipEntryRepository.findByUserIdAndDateBetween(userId, start, end);

        // Map each shift to its DTO, including tip-out records
        List<TipEntryDTO> tipEntryDTOs = tips.stream().map(tip -> {
            List<TipOutRecordDTO> records = tipOutService.getRecordsForEntry(tip);
            return toDTO(tip, records);
        }).collect(Collectors.toList());

        double totalBeforeTax = tips.stream().mapToDouble(TipEntry::getAmount).sum();
        double totalTipOut    = tipEntryDTOs.stream().mapToDouble(TipEntryDTO::getTotalTipOut).sum();
        double afterTipOut    = totalBeforeTax - totalTipOut;
        double taxDeducted    = afterTipOut * taxRate;
        double netEarnings    = afterTipOut - taxDeducted;

        ReportSummaryDTO summary = new ReportSummaryDTO();
        summary.setTipEntries(tipEntryDTOs);
        summary.setTotalBeforeTax(totalBeforeTax);
        summary.setTotalTipOut(totalTipOut);
        summary.setTotalTipShare(totalTipOut);   // backward compat alias
        summary.setGrossEarnings(afterTipOut);
        summary.setTotalTax(taxDeducted);
        summary.setNetEarnings(netEarnings);
        return summary;
    }

    // -------------------------------------------------------------------------
    // Daily earnings chart
    // -------------------------------------------------------------------------

    /**
     * Returns aggregated tip earnings grouped by day/week/month.
     * Now includes grossTips per period (for the P2-006 dashboard toggle)
     * and computes netEarnings using real tip-out records instead of a flat rate.
     */
    public List<DailyEarningsDTO> getDailyEarnings(String userEmail, Integer days, String groupBy,
                                                    LocalDate startDate, LocalDate endDate, Long jobId) {
        User user = resolveUser(userEmail);
        double taxRate = getUserTaxRate(userEmail);

        LocalDate end   = endDate   != null ? endDate   : LocalDate.now();
        LocalDate start = startDate != null ? startDate : end.minusDays((days != null ? days : 30) - 1);

        // Fetch per-day tip totals from DB (optionally filtered by job)
        List<Object[]> rows = jobId != null
            ? tipEntryRepository.findDailyAggregatesForJob(user.getId(), start, end, jobId)
            : tipEntryRepository.findDailyAggregates(user.getId(), start, end);

        // Fetch per-day tip-out totals from DB (optionally filtered by job)
        List<Object[]> tipOutRows = jobId != null
            ? tipOutRecordRepository.findDailyTipOutAggregatesForJob(user.getId(), start, end, jobId)
            : tipOutRecordRepository.findDailyTipOutAggregates(user.getId(), start, end);
        Map<LocalDate, Double> tipOutByDate = new LinkedHashMap<>();
        for (Object[] row : tipOutRows) {
            tipOutByDate.put((LocalDate) row[0], row[1] != null ? ((Number) row[1]).doubleValue() : 0.0);
        }

        // Build date → DTO map
        Map<LocalDate, DailyEarningsDTO> dailyMap = new LinkedHashMap<>();
        for (Object[] row : rows) {
            LocalDate date  = (LocalDate) row[0];
            double total    = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
            double cash     = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
            double credit   = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;
            double tipOut   = tipOutByDate.getOrDefault(date, 0.0);
            double net      = computeNet(total, tipOut, taxRate);
            dailyMap.put(date, new DailyEarningsDTO(date, total, cash, credit, net, total));
            // grossTips = total (the raw amount before any deductions)
        }

        if ("week".equals(groupBy)) {
            return aggregateByWeek(start, end, dailyMap, tipOutByDate, taxRate);
        } else if ("month".equals(groupBy)) {
            return aggregateByMonth(start, end, dailyMap, tipOutByDate, taxRate);
        } else {
            // Daily: fill every day in range (zeros for days with no tips)
            List<DailyEarningsDTO> result = new ArrayList<>();
            for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                result.add(dailyMap.getOrDefault(d, new DailyEarningsDTO(d, 0, 0, 0, 0, 0)));
            }
            return result;
        }
    }

    // -------------------------------------------------------------------------
    // Dashboard summary
    // -------------------------------------------------------------------------

    public DashboardSummaryDTO getDashboardSummary(String userEmail, Integer days,
                                                    LocalDate startDate, LocalDate endDate, Long jobId) {
        User user = resolveUser(userEmail);

        LocalDate end   = endDate   != null ? endDate   : LocalDate.now();
        LocalDate start = startDate != null ? startDate : end.minusDays((days != null ? days : 30) - 1);

        List<TipEntry> tips = jobId != null
                ? tipEntryRepository.findByUserIdAndDateBetweenAndJobId(user.getId(), start, end, jobId)
                : tipEntryRepository.findByUserIdAndDateBetween(user.getId(), start, end);
        double taxRate    = getUserTaxRate(userEmail);
        double grossTips  = tips.stream().mapToDouble(TipEntry::getAmount).sum();

        // Real tip-out total for the period (replaces hardcoded 10%)
        double totalTipOut;
        if (jobId != null) {
            List<Long> entryIds = tips.stream().map(TipEntry::getId).collect(Collectors.toList());
            totalTipOut = entryIds.isEmpty() ? 0.0
                    : tipOutRecordRepository.sumFinalAmountForEntries(entryIds);
        } else {
            totalTipOut = tipOutRecordRepository.sumFinalAmountByUserAndDateRange(user.getId(), start, end);
        }
        double afterTipOut = grossTips - totalTipOut;
        double netEarnings = afterTipOut - (afterTipOut * taxRate);

        int    shifts       = tips.size();
        double avgPerShift  = shifts > 0 ? grossTips / shifts : 0.0;
        double totalHours   = tips.stream()
                .filter(t -> t.getHoursWorked() != null && t.getHoursWorked() > 0)
                .mapToDouble(TipEntry::getHoursWorked)
                .sum();
        double hourlyWage   = totalHours > 0 ? grossTips / totalHours : 0.0;

        DashboardSummaryDTO dto = new DashboardSummaryDTO();
        dto.setTotalTips(grossTips);
        dto.setGrossTips(grossTips);
        dto.setNetEarnings(netEarnings);
        dto.setShiftsWorked(shifts);
        dto.setAvgTipsPerShift(avgPerShift);
        dto.setTotalHoursWorked(totalHours);
        dto.setEstimatedHourlyWage(hourlyWage);
        dto.setTotalTipOut(totalTipOut);
        return dto;
    }

    // -------------------------------------------------------------------------
    // Recent tips
    // -------------------------------------------------------------------------

    public List<TipEntryDTO> getRecentTips(String userEmail) {
        User user = resolveUser(userEmail);
        return tipEntryRepository.findTop7ByUserOrderByDateDesc(user)
                .stream()
                .map(tip -> toDTO(tip, tipOutService.getRecordsForEntry(tip)))
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Copies all editable fields from a request DTO onto a TipEntry entity.
     * Shared by saveTip and updateTip to avoid duplication.
     */
    private void populateTipFromRequest(TipEntry tip, TipEntryRequest req) {
        tip.setDate(req.getDate());
        tip.setShiftType(req.getShiftType());
        tip.setNotes(req.getNotes());
        tip.setCashTips(req.getCashTips());
        tip.setCreditTips(req.getCreditTips());
        tip.setStartTime(req.getStartTime());
        tip.setEndTime(req.getEndTime());
        tip.setHoursWorked(req.getHoursWorked());

        double cash   = req.getCashTips()   != null ? req.getCashTips()   : 0.0;
        double credit = req.getCreditTips() != null ? req.getCreditTips() : 0.0;

        if (cash < 0 || credit < 0) {
            throw new IllegalArgumentException("Tip amounts cannot be negative.");
        }

        // Compute total from cash + credit when provided; fall back to raw amount
        if (req.getCashTips() != null || req.getCreditTips() != null) {
            tip.setAmount(cash + credit);
        } else {
            tip.setAmount(req.getAmount());
        }

        // Auto-calculate hours if times are provided but hoursWorked wasn't sent
        if (req.getStartTime() != null && req.getEndTime() != null && req.getHoursWorked() == null) {
            long minutes = java.time.Duration.between(req.getStartTime(), req.getEndTime()).toMinutes();
            tip.setHoursWorked(minutes / 60.0);
        }

        // Link to a job profile if provided; clear it if null (unassign)
        if (req.getJobId() != null) {
            Job job = jobRepository.findById(req.getJobId())
                    .orElseThrow(() -> new IllegalArgumentException("Job not found: " + req.getJobId()));
            tip.setJob(job);
        } else {
            tip.setJob(null);
        }

        // P2-014: Resolve and apply tags (scoped to the shift's owner)
        Long userId = tip.getUser().getId();
        List<Tag> resolvedTags = tagService.resolveTagsForUser(req.getTagIds(), userId);
        tip.getTags().clear();
        tip.getTags().addAll(resolvedTags);
    }

    /**
     * Converts a TipEntry + its pre-fetched tip-out records into a TipEntryDTO.
     * Central mapping used by all service methods that return shift data.
     */
    private TipEntryDTO toDTO(TipEntry tip, List<TipOutRecordDTO> records) {
        double totalTipOut = records.stream().mapToDouble(TipOutRecordDTO::getFinalAmount).sum();

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
        dto.setTipOutRecords(records);
        dto.setTotalTipOut(totalTipOut);
        dto.setNetTips(tip.getAmount() - totalTipOut);
        dto.setTipShare(totalTipOut);   // backward compat alias
        if (tip.getJob() != null) {
            dto.setJobId(tip.getJob().getId());
            dto.setJobName(tip.getJob().getName());
        }

        // P2-014: map tags
        List<TagDTO> tagDTOs = tip.getTags().stream()
                .map(t -> new TagDTO(t.getId(), t.getName()))
                .collect(Collectors.toList());
        dto.setTags(tagDTOs);

        return dto;
    }

    /**
     * Calculates take-home net earnings after tip-outs and tax.
     *
     * Old (Phase 1): computeNet(total, taxRate) used hardcoded 10% share
     * New (Phase 2): actual tipOutTotal passed in; no hardcoded rate
     */
    private double computeNet(double grossTips, double tipOutTotal, double taxRate) {
        double afterTipOut = grossTips - tipOutTotal;
        return afterTipOut - (afterTipOut * taxRate);
    }

    private List<DailyEarningsDTO> aggregateByWeek(LocalDate start, LocalDate end,
                                                    Map<LocalDate, DailyEarningsDTO> dailyMap,
                                                    Map<LocalDate, Double> tipOutByDate,
                                                    double taxRate) {
        LocalDate weekStart = start.with(DayOfWeek.MONDAY);
        List<DailyEarningsDTO> result = new ArrayList<>();
        for (LocalDate ws = weekStart; !ws.isAfter(end); ws = ws.plusWeeks(1)) {
            double total = 0, cash = 0, credit = 0, tipOut = 0;
            for (LocalDate d = ws; !d.isAfter(ws.plusDays(6)) && !d.isAfter(end); d = d.plusDays(1)) {
                DailyEarningsDTO day = dailyMap.get(d);
                if (day != null) {
                    total  += day.getTotalTips();
                    cash   += day.getCashTips();
                    credit += day.getCreditTips();
                }
                tipOut += tipOutByDate.getOrDefault(d, 0.0);
            }
            result.add(new DailyEarningsDTO(ws, total, cash, credit, computeNet(total, tipOut, taxRate), total));
        }
        return result;
    }

    private List<DailyEarningsDTO> aggregateByMonth(LocalDate start, LocalDate end,
                                                     Map<LocalDate, DailyEarningsDTO> dailyMap,
                                                     Map<LocalDate, Double> tipOutByDate,
                                                     double taxRate) {
        LocalDate monthStart = start.withDayOfMonth(1);
        List<DailyEarningsDTO> result = new ArrayList<>();
        for (LocalDate ms = monthStart; !ms.isAfter(end); ms = ms.plusMonths(1)) {
            LocalDate me = ms.plusMonths(1).minusDays(1);
            double total = 0, cash = 0, credit = 0, tipOut = 0;
            for (LocalDate d = ms; !d.isAfter(me) && !d.isAfter(end); d = d.plusDays(1)) {
                DailyEarningsDTO day = dailyMap.get(d);
                if (day != null) {
                    total  += day.getTotalTips();
                    cash   += day.getCashTips();
                    credit += day.getCreditTips();
                }
                tipOut += tipOutByDate.getOrDefault(d, 0.0);
            }
            result.add(new DailyEarningsDTO(ms, total, cash, credit, computeNet(total, tipOut, taxRate), total));
        }
        return result;
    }

    private User resolveUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    private double getUserTaxRate(String userEmail) {
        try {
            UserSettingsDTO settings = settingsService.getSettings(userEmail);
            return settings.getTaxRate();
        } catch (Exception e) {
            return DEFAULT_TAX_RATE;
        }
    }
}
