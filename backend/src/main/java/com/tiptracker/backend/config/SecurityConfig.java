package com.tiptracker.backend.config;

import com.tiptracker.backend.security.JwtAuthenticationFilter;
import com.tiptracker.backend.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;

/**
 * Centralized configuration for all security aspects of the application.
 * This class enables web security and configures CORS, CSRF, session
 * management,
 * and authorization rules for all HTTP requests.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthFilter;

    /**
     * Defines the main security filter chain that applies to all incoming requests.
     *
     * @param http HttpSecurity object to configure security settings.
     * @return The configured SecurityFilterChain.
     * @throws Exception if an error occurs during configuration.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Configure Cross-Origin Resource Sharing (CORS) to allow requests from the
                // frontend.
                .cors(cors -> cors.configurationSource(request -> {
                    var corsConfig = new org.springframework.web.cors.CorsConfiguration();
                    corsConfig.setAllowedOrigins(List.of(
                            "https://tiptrackerapp.org",
                            "https://www.tiptrackerapp.org",
                            "http://localhost:4200"));
                    corsConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    corsConfig.setAllowedHeaders(List.of("*"));
                    corsConfig.setAllowCredentials(true);
                    return corsConfig;
                }))
                // Disable Cross-Site Request Forgery (CSRF) protection, as we are using
                // stateless JWTs.
                .csrf(csrf -> csrf.disable())
                // Define authorization rules for different endpoints.
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // Allow all preflight OPTIONS requests.
                        .requestMatchers("/api/auth/**").permitAll() // Allow all requests to authentication endpoints.
                        .anyRequest().authenticated() // Require authentication for all other requests.
                )
                // Configure session management to be stateless, as we are not using HTTP
                // sessions.
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Set the custom authentication provider.
                .authenticationProvider(authenticationProvider())
                // Add the custom JWT filter to be executed before the standard
                // username/password filter.
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Provides the custom AuthenticationProvider bean that connects Spring Security
     * to the custom UserDetailsService and PasswordEncoder.
     *
     * @return The configured AuthenticationProvider.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder()); // Use the password encoder bean
        return provider;
    }

    /**
     * Provides the PasswordEncoder bean for the entire application.
     * Uses BCrypt for strong, salted password hashing.
     *
     * @return The PasswordEncoder implementation.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Exposes the AuthenticationManager as a bean, which is required by the
     * AuthenticationService to process login requests.
     *
     * @param config The authentication configuration.
     * @return The configured AuthenticationManager.
     * @throws Exception if an error occurs.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}