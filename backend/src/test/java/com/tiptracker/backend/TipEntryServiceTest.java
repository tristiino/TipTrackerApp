package com.tiptracker.backend;

import com.tiptracker.backend.dto.ReportSummaryDTO;
import com.tiptracker.backend.model.Role;
import com.tiptracker.backend.model.TipEntry;
import com.tiptracker.backend.model.User;
import com.tiptracker.backend.repository.TipEntryRepository;
import com.tiptracker.backend.repository.UserRepository;
import com.tiptracker.backend.service.TipEntryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the TipEntryService.
 *
 */
@ExtendWith(MockitoExtension.class)
class TipEntryServiceTest {


    @Mock
    private TipEntryRepository tipEntryRepository;


    @Mock
    private UserRepository userRepository;


    @InjectMocks
    private TipEntryService tipEntryService;

    private User testUser;
    private LocalDate startDate;
    private LocalDate endDate;

    @BeforeEach
    void setUp() {
        // Create a common user for all tests
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@test.com");
        testUser.setRole(Role.USER);

        startDate = LocalDate.of(2025, 1, 1);
        endDate = LocalDate.of(2025, 1, 31);
    }

    /**
     * Test Case 1: Verifies that getReportSummary calculates totals correctly
     * when tip entries are present.
     */
    @Test
    void test_getReportSummary_WithData() {
        // 1. Create a sample list of tip entries.
        List<TipEntry> tips = new ArrayList<>();
        TipEntry tip1 = new TipEntry();
        tip1.setAmount(100.00);
        TipEntry tip2 = new TipEntry();
        tip2.setAmount(150.00);
        tips.add(tip1);
        tips.add(tip2);

        // 2. Tell the mock repository what to return when its method is called.
        when(tipEntryRepository.findByUserIdAndDateBetween(anyLong(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(tips);


        // 3. Call the actual service method we want to test.
        ReportSummaryDTO result = tipEntryService.getReportSummary(testUser.getId(), startDate, endDate);


        // 4. Check if the calculations are correct. 10% tip share, 3% tax rate.
        assertEquals(2, result.getTipEntries().size(), "Should have 2 tip entries");
        assertEquals(250.00, result.getTotalBeforeTax(), "Total Before Tax should be 250.00");
        assertEquals(25.00, result.getTotalTipShare(), "Tip Share should be 25.00"); // 10% of 250
        assertEquals(225.00, result.getGrossEarnings(), "Gross Earnings should be 225.00"); // 250 - 25
        assertEquals(6.75, result.getTotalTax(), "Tax Deducted should be 6.75"); // 3% of 225
        assertEquals(218.25, result.getNetEarnings(), "Net Earnings should be 218.25"); // 225 - 6.75
    }

    /**
     * Test Case 2: Verifies that getReportSummary returns a zeroed-out report
     * when no tip entries are found.
     */
    @Test
    void test_getReportSummary_NoData() {

        // 1. Tell the mock repository to return an empty list.
        when(tipEntryRepository.findByUserIdAndDateBetween(anyLong(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new ArrayList<>());


        // 2. Call the service method.
        ReportSummaryDTO result = tipEntryService.getReportSummary(testUser.getId(), startDate, endDate);


        // 3. Check if all values are zero.
        assertEquals(0, result.getTipEntries().size(), "Should have 0 tip entries");
        assertEquals(0.0, result.getTotalBeforeTax(), "Total Before Tax should be 0.0");
        assertEquals(0.0, result.getNetEarnings(), "Net Earnings should be 0.0");
    }
}