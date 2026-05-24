package com.tiptracker.backend;

import com.tiptracker.backend.model.User;
import com.tiptracker.backend.model.UserSettings;
import com.tiptracker.backend.repository.UserSettingsRepository;
import com.tiptracker.backend.service.NotificationService;
import com.tiptracker.backend.service.WeeklyTipReminderScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeeklyTipReminderSchedulerTest {

    @Mock private UserSettingsRepository userSettingsRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private WeeklyTipReminderScheduler scheduler;

    private UserSettings makeSettings(long userId) {
        User user = new User();
        user.setId(userId);
        UserSettings s = new UserSettings();
        s.setUser(user);
        s.setNotificationsEnabled(true);
        return s;
    }

    @Test
    void sendWeeklyReminders_eligibleUsers_areNotified() {
        UserSettings s1 = makeSettings(1L);
        UserSettings s2 = makeSettings(2L);
        when(userSettingsRepository.findEligibleForNotification(anyInt(), any(LocalTime.class), any(LocalTime.class)))
                .thenReturn(List.of(s1, s2));

        scheduler.sendWeeklyReminders();

        verify(notificationService).sendReminderToUser(s1);
        verify(notificationService).sendReminderToUser(s2);
    }

    @Test
    void sendWeeklyReminders_noEligibleUsers_nothingSent() {
        when(userSettingsRepository.findEligibleForNotification(anyInt(), any(LocalTime.class), any(LocalTime.class)))
                .thenReturn(List.of());

        scheduler.sendWeeklyReminders();

        verify(notificationService, never()).sendReminderToUser(any());
    }

    @Test
    void sendWeeklyReminders_oneUserFails_othersStillNotified() {
        UserSettings s1 = makeSettings(1L);
        UserSettings s2 = makeSettings(2L);
        when(userSettingsRepository.findEligibleForNotification(anyInt(), any(LocalTime.class), any(LocalTime.class)))
                .thenReturn(List.of(s1, s2));
        doThrow(new RuntimeException("APNs error")).when(notificationService).sendReminderToUser(s1);

        // should not throw
        scheduler.sendWeeklyReminders();

        verify(notificationService).sendReminderToUser(s2);
    }
}
