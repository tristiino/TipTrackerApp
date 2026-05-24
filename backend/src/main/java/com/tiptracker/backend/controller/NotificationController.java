package com.tiptracker.backend.controller;

import com.tiptracker.backend.dto.DeviceTokenRequest;
import com.tiptracker.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

/**
 * Endpoints for managing APNs device tokens.
 * Called by the TipSlip iOS app after receiving/losing its APNs token from Apple.
 * User identity is always resolved from the JWT — never from request parameters.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Registers (or refreshes) an APNs device token for the authenticated user.
     * Safe to call multiple times with the same token — idempotent upsert.
     */
    @PostMapping("/device-token")
    public ResponseEntity<Void> registerToken(
            @RequestBody DeviceTokenRequest request,
            Principal principal) {
        if (request.getToken() == null || request.getToken().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        notificationService.registerToken(principal.getName(), request.getToken());
        return ResponseEntity.ok().build();
    }

    /**
     * Removes an APNs device token. Called on sign-out or account deletion.
     * Returns 204 even if the token was not found (no-op).
     */
    @DeleteMapping("/device-token")
    public ResponseEntity<Void> removeToken(
            @RequestBody DeviceTokenRequest request,
            Principal principal) {
        notificationService.removeToken(request.getToken());
        return ResponseEntity.noContent().build();
    }
}
