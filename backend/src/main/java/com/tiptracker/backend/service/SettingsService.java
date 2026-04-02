package com.tiptracker.backend.service;

import com.tiptracker.backend.dto.UserSettingsDTO;
import com.tiptracker.backend.model.User;
import com.tiptracker.backend.model.UserSettings;
import com.tiptracker.backend.repository.UserRepository;
import com.tiptracker.backend.repository.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Service for reading and updating per-user application settings.
 * A settings row is created with sensible defaults the first time a user's
 * settings are accessed, so callers never need to handle a missing row.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettingsService {

    private final UserRepository userRepository;
    private final UserSettingsRepository userSettingsRepository;

    /**
     * Returns the settings for the authenticated user.
     * Creates a default row if one does not yet exist.
     * @param email The email of the authenticated user (from JWT principal).
     * @return The user's current settings as a DTO.
     */
    public UserSettingsDTO getSettings(String email) {
        User user = resolveUser(email);
        UserSettings settings = userSettingsRepository.findByUser(user)
                .orElseGet(() -> createDefaults(user));
        return toDTO(settings);
    }

    /**
     * Applies the values from the DTO to the user's settings row and persists them.
     * Creates the row first if it does not yet exist.
     * @param email The email of the authenticated user (from JWT principal).
     * @param dto   The new settings values sent by the client.
     * @return The saved settings as a DTO.
     */
    public UserSettingsDTO updateSettings(String email, UserSettingsDTO dto) {
        User user = resolveUser(email);
        UserSettings settings = userSettingsRepository.findByUser(user)
                .orElseGet(() -> createDefaults(user));

        settings.setTheme(dto.getTheme());
        settings.setLanguage(dto.getLanguage());
        settings.setTaxRate(dto.getTaxRate());
        settings.setPayPeriodStartAnchor(dto.getPayPeriodStartAnchor());
        settings.setPayPeriodLengthDays(dto.getPayPeriodLengthDays());

        return toDTO(userSettingsRepository.save(settings));
    }

    private UserSettings createDefaults(User user) {
        log.info("Creating default settings for user: {}", user.getEmail());
        UserSettings defaults = new UserSettings();
        defaults.setUser(user);
        defaults.setTheme("light");
        defaults.setLanguage("english");
        defaults.setTaxRate(0.03);
        defaults.setPayPeriodStartAnchor(null);
        defaults.setPayPeriodLengthDays(14);
        return userSettingsRepository.save(defaults);
    }

    private User resolveUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }

    private UserSettingsDTO toDTO(UserSettings settings) {
        return new UserSettingsDTO(
                settings.getTheme(),
                settings.getLanguage(),
                settings.getTaxRate(),
                settings.getPayPeriodStartAnchor(),
                settings.getPayPeriodLengthDays()
        );
    }
}
