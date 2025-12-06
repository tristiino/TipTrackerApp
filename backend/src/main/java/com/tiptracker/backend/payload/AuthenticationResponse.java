package com.tiptracker.backend.payload;

import com.tiptracker.backend.dto.UserDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the successful response sent to the client after authentication (login or registration).
 * It bundles the JWT for future authenticated requests and essential user information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticationResponse {
    /**
     * The JSON Web Token (JWT) to be used by the client for authenticating subsequent requests.
     */
    private String token;

    /**
     * A DTO containing non-sensitive information about the authenticated user.
     */
    private UserDto user;
}