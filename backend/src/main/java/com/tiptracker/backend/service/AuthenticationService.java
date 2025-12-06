package com.tiptracker.backend.service;

import com.tiptracker.backend.dto.LoginRequest;
import com.tiptracker.backend.dto.RegisterRequest;
import com.tiptracker.backend.dto.UserDto;
import com.tiptracker.backend.model.Role;
import com.tiptracker.backend.model.User;
import com.tiptracker.backend.payload.AuthenticationResponse;
import com.tiptracker.backend.repository.UserRepository;
import com.tiptracker.backend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Service responsible for handling user authentication, including login and registration.
 * This is the central point for all authentication-related business logic.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder; // Added for registration

    /**
     * Authenticates a user based on login credentials and returns a JWT.
     * @param loginRequest DTO containing email and password.
     * @return AuthenticationResponse containing the JWT and user data.
     */
    public AuthenticationResponse authenticate(LoginRequest loginRequest) {
        log.info("Authenticating user: {}", loginRequest.getEmail());

        // Spring Security handles the password check
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found after authentication"));

        String jwt = jwtUtil.generateToken(user);
        UserDto userDto = new UserDto(user.getId(), user.getEmail(), user.getUsername());

        return new AuthenticationResponse(jwt, userDto);
    }

    /**
     * Registers a new user, encodes their password, saves them to the database,
     * and returns a JWT for immediate login.
     * @param request DTO containing username, email, and raw password.
     * @return AuthenticationResponse containing the JWT and new user's data.
     */
    public AuthenticationResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalStateException("Email already in use");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword())); // Encode password here
        user.setRole(Role.USER); // Default role for new users

        userRepository.save(user);

        String jwt = jwtUtil.generateToken(user);
        UserDto userDto = new UserDto(user.getId(), user.getEmail(), user.getUsername());

        return new AuthenticationResponse(jwt, userDto);
    }
}