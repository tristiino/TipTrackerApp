package com.tiptracker.backend.controller;

import com.tiptracker.backend.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/**
 * Account lifecycle endpoints.
 * DELETE /api/account is required by Apple App Store Guideline 5.1.1 —
 * any app that allows account creation must provide in-app account deletion.
 */
@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    /**
     * Permanently deletes the authenticated user's account and all their data.
     * No request body required — user identity comes from the JWT.
     *
     * @return 204 No Content on success.
     */
    @DeleteMapping
    public ResponseEntity<Void> deleteAccount(Principal principal) {
        accountService.deleteAccount(principal.getName());
        return ResponseEntity.noContent().build();
    }
}
