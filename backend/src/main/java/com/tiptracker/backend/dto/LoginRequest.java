package com.tiptracker.backend.dto;

import lombok.Data;

/**
 * Data Transfer Object for receiving user login credentials from the client.
 * The usernameOrEmail field accepts either a username or an email address.
 */
@Data
public class LoginRequest {
    private String usernameOrEmail;
    private String password;
}