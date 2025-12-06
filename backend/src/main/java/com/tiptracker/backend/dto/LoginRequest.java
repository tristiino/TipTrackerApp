package com.tiptracker.backend.dto;

import lombok.Data;

/**
 * Data Transfer Object for receiving user login credentials from the client.
 */
@Data
public class LoginRequest {
    private String email;
    private String password;
}