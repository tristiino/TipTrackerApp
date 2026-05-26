package com.tiptracker.backend.service;

import com.tiptracker.backend.model.UserSettings;
import com.tiptracker.backend.repository.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Runs every 15 minutes (UTC). On each tick it finds users whose notification
 * time falls within the current window, then delegates to NotificationService
 * to calculate their weekly tip total and send the APNs reminder.
 *
 * Day-of-week mapping: Java DayOfWeek MON=1..SUN=7 is converted to 0=Sun,1=Mon..6=Sat
 * to match the iOS picker and the reminder_day_of_week column convention.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeeklyTipReminderScheduler {

    private final UserSettingsRepository userSettingsRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 * * * * *")
    public void sendWeeklyReminders() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        // Java: MON=1 ... SUN=7  →  we need: SUN=0, MON=1 ... SAT=6
        int todayDow = now.getDayOfWeek().getValue() % 7;

        LocalTime windowStart = now.toLocalTime()
                .truncatedTo(ChronoUnit.MINUTES)
                .minusMinutes(now.getMinute() % 15);
        LocalTime windowEnd = windowStart.plusMinutes(15);

        List<UserSettings> eligible =
                userSettingsRepository.findEligibleForNotification(todayDow, windowStart, windowEnd);

        log.debug("Reminder tick — dow={}, window={}–{}, eligible={}", todayDow, windowStart, windowEnd, eligible.size());

        for (UserSettings settings : eligible) {
            try {
                notificationService.sendReminderToUser(settings);
            } catch (Exception e) {
                log.error("Failed to send reminder for user {}", settings.getUser().getId(), e);
            }
        }
    }
}
