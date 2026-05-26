package com.tiptracker.backend;

import com.tiptracker.backend.model.*;
import com.tiptracker.backend.repository.*;
import com.tiptracker.backend.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for DELETE /api/account (FR-041).
 *
 * Creates a user with data across every relevant table, calls the endpoint,
 * then verifies the user row and all related rows are gone.
 * Runs against H2 in-memory (local profile).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class AccountDeletionTest {

    @Autowired private MockMvc mvc;
    @Autowired private UserRepository userRepository;
    @Autowired private UserSettingsRepository userSettingsRepository;
    @Autowired private TipEntryRepository tipEntryRepository;
    @Autowired private TipOutRoleRepository tipOutRoleRepository;
    @Autowired private TipOutRecordRepository tipOutRecordRepository;
    @Autowired private JobRepository jobRepository;
    @Autowired private TagRepository tagRepository;
    @Autowired private DeviceTokenRepository deviceTokenRepository;
    @Autowired private PasswordResetTokenRepository passwordResetTokenRepository;
    @Autowired private JwtUtil jwtUtil;

    private User user;
    private String authHeader;

    @BeforeEach
    void setUp() {
        // Wipe everything between tests
        deviceTokenRepository.deleteAll();
        tipOutRecordRepository.deleteAll();
        tipEntryRepository.deleteAll();
        tipOutRoleRepository.deleteAll();
        userSettingsRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();
        tagRepository.deleteAll();
        jobRepository.deleteAll();
        userRepository.deleteAll();

        // Create a fresh test user
        user = new User();
        user.setUsername("acctdeluser");
        user.setEmail("acctdel@test.com");
        user.setPassword("$2a$10$dummyhash00000000000000000000000000000000000000000000000");
        user.setRole(Role.USER);
        user = userRepository.save(user);

        authHeader = "Bearer " + jwtUtil.generateToken(user);
    }

    @Test
    void deleteAccount_noData_returns204AndRemovesUser() throws Exception {
        mvc.perform(delete("/api/account")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        assertFalse(userRepository.findByEmail("acctdel@test.com").isPresent(),
                "User row should be gone");
    }

    @Test
    void deleteAccount_withFullData_removesAllRelatedRows() throws Exception {
        // --- Populate data across every table ---

        // UserSettings
        UserSettings settings = new UserSettings();
        settings.setUser(user);
        settings.setTheme("dark");
        settings.setLanguage("english");
        settings.setTaxRate(0.03);
        settings.setPayPeriodLengthDays(14);
        settings.setMorningStart(LocalTime.of(6, 0));
        settings.setEveningStart(LocalTime.of(14, 0));
        settings.setNightStart(LocalTime.of(21, 0));
        settings.setNotificationsEnabled(false);
        settings.setReminderDayOfWeek(0);
        settings.setReminderTime(LocalTime.of(18, 0));
        userSettingsRepository.save(settings);

        // Job
        Job job = new Job();
        job.setName("Main Restaurant");
        job.setUser(user);
        job = jobRepository.save(job);

        // TipOutRole
        TipOutRole role = new TipOutRole();
        role.setName("Busser");
        role.setSplitType(TipOutType.PERCENTAGE);
        role.setAmount(5.0);
        role.setSource(TipOutSource.BOTH);
        role.setUser(user);
        role.setJob(job);
        role = tipOutRoleRepository.save(role);

        // Tag
        Tag tag = new Tag();
        tag.setName("weekend");
        tag.setUserId(user.getId());
        tag = tagRepository.save(tag);

        // TipEntry (with TipOutRecord and Tag)
        TipEntry entry = new TipEntry();
        entry.setDate(LocalDate.now());
        entry.setAmount(100.0);
        entry.setCashTips(40.0);
        entry.setCreditTips(60.0);
        entry.setShiftType("Evening");
        entry.setUser(user);
        entry.setJob(job);
        entry.setTags(Set.of(tag));
        entry = tipEntryRepository.save(entry);

        TipOutRecord record = new TipOutRecord();
        record.setTipEntry(entry);
        record.setRoleName("Busser");
        record.setComputedAmount(5.0);
        record.setFinalAmount(5.0);
        record.setOverridden(false);
        tipOutRecordRepository.save(record);

        // DeviceToken
        DeviceToken dt = new DeviceToken();
        dt.setUser(user);
        dt.setToken("test-apns-token-for-deletion");
        deviceTokenRepository.save(dt);

        // PasswordResetToken
        PasswordResetToken prt = new PasswordResetToken();
        prt.setUser(user);
        prt.setToken("reset-token-xyz");
        prt.setExpiresAt(java.time.LocalDateTime.now().plusHours(1));
        prt.setUsed(false);
        passwordResetTokenRepository.save(prt);

        Long userId = user.getId();

        // --- Call DELETE /api/account ---
        mvc.perform(delete("/api/account")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        // --- Verify every table is clean ---

        assertFalse(userRepository.existsById(userId),
                "users row should be gone");
        assertTrue(userSettingsRepository.findByUser(user).isEmpty(),
                "user_settings row should be gone");
        assertTrue(tipEntryRepository.findByUserIdAndDateBetween(userId, LocalDate.MIN, LocalDate.MAX).isEmpty(),
                "tip_entry rows should be gone");
        assertTrue(tipOutRoleRepository.findByUserOrderByNameAsc(user).isEmpty(),
                "tip_out_role rows should be gone");
        assertTrue(tipOutRecordRepository.findAll().stream()
                        .noneMatch(r -> r.getRoleName().equals("Busser")),
                "tip_out_records rows should be gone");
        assertTrue(jobRepository.findAll().stream()
                        .noneMatch(j -> j.getName().equals("Main Restaurant")),
                "job rows should be gone");
        assertTrue(tagRepository.findByUserId(userId).isEmpty(),
                "shift_tag rows should be gone");
        assertTrue(deviceTokenRepository.findByToken("test-apns-token-for-deletion").isEmpty(),
                "device_tokens row should be gone (DB cascade)");
        assertTrue(passwordResetTokenRepository.findByToken("reset-token-xyz").isEmpty(),
                "password_reset_tokens row should be gone");
    }

    @Test
    void deleteAccount_noAuth_returns403() throws Exception {
        mvc.perform(delete("/api/account")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        // User should still exist
        assertTrue(userRepository.findByEmail("acctdel@test.com").isPresent());
    }
}
