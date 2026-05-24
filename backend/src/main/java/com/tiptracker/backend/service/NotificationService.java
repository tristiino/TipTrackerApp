package com.tiptracker.backend.service;

import com.tiptracker.backend.model.DeviceToken;
import com.tiptracker.backend.model.User;
import com.tiptracker.backend.model.UserSettings;
import com.tiptracker.backend.repository.DeviceTokenRepository;
import com.tiptracker.backend.repository.TipEntryRepository;
import com.tiptracker.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * Handles device token registration/removal and the per-user reminder send logic.
 * Token ownership follows the most recent authenticated registration — if a token
 * is re-registered by a different user (reinstall scenario), the user_id is updated.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final UserRepository userRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final TipEntryRepository tipEntryRepository;
    private final ApnsService apnsService;

    /**
     * Upserts an APNs device token for the authenticated user.
     * If the token already exists for a different user, it is reassigned.
     */
    @Transactional
    public void registerToken(String email, String token) {
        User user = resolveUser(email);
        deviceTokenRepository.findByToken(token).ifPresentOrElse(
                existing -> {
                    existing.setUser(user);
                    deviceTokenRepository.save(existing);
                },
                () -> {
                    DeviceToken dt = new DeviceToken();
                    dt.setUser(user);
                    dt.setToken(token);
                    deviceTokenRepository.save(dt);
                }
        );
    }

    /**
     * Removes a device token. No-op if the token is not found.
     */
    @Transactional
    public void removeToken(String token) {
        if (token != null && !token.isBlank()) {
            deviceTokenRepository.deleteByToken(token);
        }
    }

    /**
     * Queries the user's weekly tip total, builds a personalized message,
     * and sends a push notification to all of the user's registered devices.
     * Called by WeeklyTipReminderScheduler for each eligible user.
     */
    public void sendReminderToUser(UserSettings settings) {
        User user = settings.getUser();

        LocalDate weekStart = LocalDate.now(ZoneOffset.UTC)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(7);

        BigDecimal weeklyTotal = tipEntryRepository
                .sumTipsForUserBetween(user.getId(), weekStart, weekEnd);
        if (weeklyTotal == null) weeklyTotal = BigDecimal.ZERO;

        String body;
        if (weeklyTotal.compareTo(BigDecimal.ZERO) > 0) {
            body = String.format("You've logged $%.2f so far this week — add any missing shifts!", weeklyTotal);
        } else {
            body = "No tips logged this week — add your first shift!";
        }

        List<DeviceToken> tokens = deviceTokenRepository.findByUserId(user.getId());
        for (DeviceToken dt : tokens) {
            apnsService.sendNotification(dt.getToken(), "Weekly Tips Reminder", body);
        }
    }

    private User resolveUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }
}
