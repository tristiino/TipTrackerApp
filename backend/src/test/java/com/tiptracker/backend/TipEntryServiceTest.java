package com.tiptracker.backend;

import com.tiptracker.backend.dto.ReportSummaryDTO;
import com.tiptracker.backend.dto.TipOutRecordDTO;
import com.tiptracker.backend.model.Role;
import com.tiptracker.backend.model.TipEntry;
import com.tiptracker.backend.model.TipOutType;
import com.tiptracker.backend.model.User;
import com.tiptracker.backend.repository.TipEntryRepository;
import com.tiptracker.backend.repository.TipOutRecordRepository;
import com.tiptracker.backend.repository.UserRepository;
import com.tiptracker.backend.service.SettingsService;
import com.tiptracker.backend.service.TipEntryService;
import com.tiptracker.backend.service.TipOutService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for TipEntryService.
 *
 * Phase 2 changes:
 * - Removed assertions based on the old hardcoded 10% tip share.
 * - Added TipOutRecordRepository and TipOutService mocks (new dependencies).
 * - test_getReportSummary_WithData now expects $0 tip-out when no records
 *   are attached (correct behavior for legacy shifts with no role configuration).
 * - New test verifies that when real TipOutRecords are present, the report
 *   correctly deducts them from gross earnings.
 */
@ExtendWith(MockitoExtension.class)
class TipEntryServiceTest {

    @Mock
    private TipEntryRepository tipEntryRepository;

    @Mock
    private TipOutRecordRepository tipOutRecordRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SettingsService settingsService;

    @Mock
    private TipOutService tipOutService;

    @InjectMocks
    private TipEntryService tipEntryService;

    private User testUser;
    private LocalDate startDate;
    private LocalDate endDate;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@test.com");
        testUser.setRole(Role.USER);

        startDate = LocalDate.of(2025, 1, 1);
        endDate   = LocalDate.of(2025, 1, 31);
    }

    /**
     * Test 1: Shifts with no tip-out roles applied.
     *
     * Before Phase 2, this test asserted 10% tip share ($25).
     * Now the correct expectation is $0 tip-out, because no TipOutRecord
     * rows exist for these shifts. Tax (3%) applies directly to gross.
     *
     * totalBeforeTax  = $250.00
     * totalTipOut     = $0.00  (no records)
     * grossEarnings   = $250.00
     * totalTax        = $7.50  (3% of $250)
     * netEarnings     = $242.50
     */
    @Test
    void test_getReportSummary_WithData_NoTipOuts() {
        List<TipEntry> tips = buildTips(100.00, 150.00);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(tipEntryRepository.findByUserIdAndDateBetween(anyLong(), any(), any()))
                .thenReturn(tips);
        // No tip-out records for any shift
        when(tipOutService.getRecordsForEntry(any(TipEntry.class)))
                .thenReturn(List.of());

        ReportSummaryDTO result = tipEntryService.getReportSummary(testUser.getEmail(), startDate, endDate);

        assertEquals(2,      result.getTipEntries().size(),  "Should have 2 entries");
        assertEquals(250.00, result.getTotalBeforeTax(),     "Total before tax should be 250.00");
        assertEquals(0.00,   result.getTotalTipOut(),        "No tip-outs → $0 deducted");
        assertEquals(0.00,   result.getTotalTipShare(),      "Backward compat alias should also be 0");
        assertEquals(250.00, result.getGrossEarnings(),      "Gross = total - tip-outs = 250");
        assertEquals(7.50,   result.getTotalTax(),           "3% of 250 = 7.50");
        assertEquals(242.50, result.getNetEarnings(),        "250 - 7.50 = 242.50");
    }

    /**
     * Test 2: Shifts with real TipOutRecord data applied.
     *
     * Simulates a user who configured a "Busser" role at 5% ($5 on a $100 shift)
     * and a "Bartender" role at 3% ($4.50 on a $150 shift).
     * Total tip-out = $9.50.
     *
     * totalBeforeTax  = $250.00
     * totalTipOut     = $9.50
     * grossEarnings   = $240.50
     * totalTax        = $7.215 → 7.22 (3% of 240.50)
     * netEarnings     = $233.285 → 233.29
     */
    @Test
    void test_getReportSummary_WithTipOutRecords() {
        TipEntry tip1 = new TipEntry(); tip1.setAmount(100.00);
        TipEntry tip2 = new TipEntry(); tip2.setAmount(150.00);
        List<TipEntry> tips = List.of(tip1, tip2);

        TipOutRecordDTO record1 = new TipOutRecordDTO(1L, 1L, "Busser",
                5.00, 5.00, false);   // 5% of $100 = $5.00
        TipOutRecordDTO record2 = new TipOutRecordDTO(2L, 2L, "Bartender",
                4.50, 4.50, false);   // 3% of $150 = $4.50

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(tipEntryRepository.findByUserIdAndDateBetween(anyLong(), any(), any()))
                .thenReturn(tips);
        when(tipOutService.getRecordsForEntry(tip1)).thenReturn(List.of(record1));
        when(tipOutService.getRecordsForEntry(tip2)).thenReturn(List.of(record2));

        ReportSummaryDTO result = tipEntryService.getReportSummary(testUser.getEmail(), startDate, endDate);

        assertEquals(250.00, result.getTotalBeforeTax(),  0.001, "Total before tax");
        assertEquals(9.50,   result.getTotalTipOut(),     0.001, "Total tip-out = 5 + 4.50");
        assertEquals(240.50, result.getGrossEarnings(),   0.001, "Gross = 250 - 9.50");
        assertEquals(7.215,  result.getTotalTax(),        0.001, "3% of 240.50");
        assertEquals(233.285,result.getNetEarnings(),     0.001, "240.50 - 7.215");
    }

    /**
     * Test 3: No shifts in the date range — all values should be zero.
     */
    @Test
    void test_getReportSummary_NoData() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(tipEntryRepository.findByUserIdAndDateBetween(anyLong(), any(), any()))
                .thenReturn(new ArrayList<>());

        ReportSummaryDTO result = tipEntryService.getReportSummary(testUser.getEmail(), startDate, endDate);

        assertEquals(0, result.getTipEntries().size(), "Should have 0 entries");
        assertEquals(0.0, result.getTotalBeforeTax(),  "Total before tax should be 0");
        assertEquals(0.0, result.getTotalTipOut(),      "Tip-out should be 0");
        assertEquals(0.0, result.getNetEarnings(),      "Net earnings should be 0");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private List<TipEntry> buildTips(double... amounts) {
        List<TipEntry> tips = new ArrayList<>();
        for (double amount : amounts) {
            TipEntry t = new TipEntry();
            t.setAmount(amount);
            tips.add(t);
        }
        return tips;
    }
}
