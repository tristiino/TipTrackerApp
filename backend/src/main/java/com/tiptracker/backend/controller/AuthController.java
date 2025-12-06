package com.tiptracker.backend.controller;

import com.tiptracker.backend.dto.LoginRequest;
import com.tiptracker.backend.dto.RegisterRequest;
import com.tiptracker.backend.payload.AuthenticationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.tiptracker.backend.service.AuthenticationService;

/**
 * Controller for handling user authentication processes, including user login and registration.
 * All endpoints under /api/auth are publicly accessible as defined in SecurityConfig.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;

    /**
     * Handles user login requests.
     *
     * @param loginRequest DTO containing the user's email and password.
     * @return A ResponseEntity containing the JWT and user information on success, or an error on failure.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@RequestBody LoginRequest loginRequest) {
        log.info("Attempting login for user: {}", loginRequest.getEmail());
        AuthenticationResponse authResponse = authenticationService.authenticate(loginRequest);
        log.info("Login successful for user: {}", loginRequest.getEmail());
        return ResponseEntity.ok(authResponse);
    }

    /**
     * Handles new user registration requests.
     * The password is raw here and will be encoded by the AuthenticationService.
     *
     * @param registerRequest DTO containing username, email, and raw password.
     * @return A ResponseEntity containing the created user's information on success, or an error on failure.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest registerRequest) {
        log.info("Registering new user: {}", registerRequest.getEmail());
        try {
            AuthenticationResponse authResponse = authenticationService.register(registerRequest);
            log.info("Registration successful for user: {}", registerRequest.getEmail());
            return ResponseEntity.ok(authResponse);
        } catch (Exception e) {
            log.error("Registration failed for user: {} - Reason: {}", registerRequest.getEmail(), e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}