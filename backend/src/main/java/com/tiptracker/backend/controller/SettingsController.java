package com.tiptracker.backend.controller;

import com.tiptracker.backend.dto.UserSettingsDTO;
import com.tiptracker.backend.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

/**
 * Exposes GET and PUT endpoints for user settings.
 * The authenticated user is always resolved from the JWT principal —
 * never accepted as a request parameter — to prevent IDOR.
 */
@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;

    /**
     * Returns the current user's settings.
     * Creates a default row on first access.
     * @param principal The authenticated user from the JWT.
     * @return The user's settings DTO.
     */
    @GetMapping
    public ResponseEntity<UserSettingsDTO> getSettings(Principal principal) {
        return ResponseEntity.ok(settingsService.getSettings(principal.getName()));
    }

    /**
     * Replaces the current user's settings with the values in the request body.
     * @param dto       The new settings values.
     * @param principal The authenticated user from the JWT.
     * @return The saved settings DTO.
     */
    @PutMapping
    public ResponseEntity<UserSettingsDTO> updateSettings(
            @RequestBody UserSettingsDTO dto,
            Principal principal) {
        return ResponseEntity.ok(settingsService.updateSettings(principal.getName(), dto));
    }
}
