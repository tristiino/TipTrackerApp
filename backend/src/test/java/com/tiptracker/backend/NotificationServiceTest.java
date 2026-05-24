package com.tiptracker.backend;

import com.tiptracker.backend.model.DeviceToken;
import com.tiptracker.backend.model.Role;
import com.tiptracker.backend.model.User;
import com.tiptracker.backend.model.UserSettings;
import com.tiptracker.backend.repository.DeviceTokenRepository;
import com.tiptracker.backend.repository.TipEntryRepository;
import com.tiptracker.backend.repository.UserRepository;
import com.tiptracker.backend.service.ApnsService;
import com.tiptracker.backend.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private DeviceTokenRepository deviceTokenRepository;
    @Mock private TipEntryRepository tipEntryRepository;
    @Mock private ApnsService apnsService;

    @InjectMocks
    private NotificationService notificationService;

    private User user;
    private UserSettings settings;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("dev@test.com");
        user.setUsername("devuser");
        user.setRole(Role.USER);

        settings = new UserSettings();
        settings.setUser(user);
        settings.setNotificationsEnabled(true);
        settings.setReminderDayOfWeek(1);
        settings.setReminderTime(LocalTime.of(18, 0));
    }

    // --- registerToken ---

    @Test
    void registerToken_newToken_savesNewRow() {
        when(userRepository.findByEmail("dev@test.com")).thenReturn(Optional.of(user));
        when(deviceTokenRepository.findByToken("token-abc")).thenReturn(Optional.empty());

        notificationService.registerToken("dev@test.com", "token-abc");

        ArgumentCaptor<DeviceToken> captor = ArgumentCaptor.forClass(DeviceToken.class);
        verify(deviceTokenRepository).save(captor.capture());
        assertEquals("token-abc", captor.getValue().getToken());
        assertEquals(user, captor.getValue().getUser());
    }

    @Test
    void registerToken_existingToken_updatesUser() {
        DeviceToken existing = new DeviceToken();
        existing.setToken("token-abc");
        existing.setUser(new User()); // different user

        when(userRepository.findByEmail("dev@test.com")).thenReturn(Optional.of(user));
        when(deviceTokenRepository.findByToken("token-abc")).thenReturn(Optional.of(existing));

        notificationService.registerToken("dev@test.com", "token-abc");

        verify(deviceTokenRepository).save(existing);
        assertEquals(user, existing.getUser());
    }

    // --- removeToken ---

    @Test
    void removeToken_deletesToken() {
        notificationService.removeToken("token-abc");
        verify(deviceTokenRepository).deleteByToken("token-abc");
    }

    @Test
    void removeToken_blankToken_doesNothing() {
        notificationService.removeToken("");
        verify(deviceTokenRepository, never()).deleteByToken(any());
    }

    // --- sendReminderToUser: message content ---

    @Test
    void sendReminderToUser_withTips_sendsPersonalizedMessage() {
        DeviceToken dt = new DeviceToken();
        dt.setToken("device-token-1");
        dt.setUser(user);

        when(deviceTokenRepository.findByUserId(1L)).thenReturn(List.of(dt));
        when(tipEntryRepository.sumTipsForUserBetween(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new BigDecimal("123.45"));

        notificationService.sendReminderToUser(settings);

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(apnsService).sendNotification(eq("device-token-1"), eq("Weekly Tips Reminder"), bodyCaptor.capture());
        assertTrue(bodyCaptor.getValue().contains("$123.45"));
        assertTrue(bodyCaptor.getValue().contains("so far this week"));
    }

    @Test
    void sendReminderToUser_noTips_sendsZeroTipsMessage() {
        DeviceToken dt = new DeviceToken();
        dt.setToken("device-token-1");
        dt.setUser(user);

        when(deviceTokenRepository.findByUserId(1L)).thenReturn(List.of(dt));
        when(tipEntryRepository.sumTipsForUserBetween(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(BigDecimal.ZERO);

        notificationService.sendReminderToUser(settings);

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(apnsService).sendNotification(eq("device-token-1"), eq("Weekly Tips Reminder"), bodyCaptor.capture());
        assertTrue(bodyCaptor.getValue().contains("No tips logged"));
    }

    @Test
    void sendReminderToUser_nullTotal_treatedAsZero() {
        DeviceToken dt = new DeviceToken();
        dt.setToken("device-token-1");
        dt.setUser(user);

        when(deviceTokenRepository.findByUserId(1L)).thenReturn(List.of(dt));
        when(tipEntryRepository.sumTipsForUserBetween(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(null);

        notificationService.sendReminderToUser(settings);

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(apnsService).sendNotification(any(), any(), bodyCaptor.capture());
        assertTrue(bodyCaptor.getValue().contains("No tips logged"));
    }

    @Test
    void sendReminderToUser_sendsToAllDevices() {
        DeviceToken dt1 = new DeviceToken(); dt1.setToken("token-1"); dt1.setUser(user);
        DeviceToken dt2 = new DeviceToken(); dt2.setToken("token-2"); dt2.setUser(user);

        when(deviceTokenRepository.findByUserId(1L)).thenReturn(List.of(dt1, dt2));
        when(tipEntryRepository.sumTipsForUserBetween(any(), any(), any())).thenReturn(BigDecimal.ZERO);

        notificationService.sendReminderToUser(settings);

        verify(apnsService).sendNotification(eq("token-1"), any(), any());
        verify(apnsService).sendNotification(eq("token-2"), any(), any());
    }
}
