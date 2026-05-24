package com.tiptracker.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tiptracker.backend.dto.DeviceTokenRequest;
import com.tiptracker.backend.model.Role;
import com.tiptracker.backend.model.User;
import com.tiptracker.backend.repository.DeviceTokenRepository;
import com.tiptracker.backend.repository.UserRepository;
import com.tiptracker.backend.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for POST/DELETE /api/notifications/device-token.
 * Runs against H2 in-memory DB (local profile). JWT is generated using the real JwtUtil.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class NotificationControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private DeviceTokenRepository deviceTokenRepository;
    @Autowired private JwtUtil jwtUtil;

    private String authHeader;

    @BeforeEach
    void setUp() {
        deviceTokenRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.setUsername("notiftest");
        user.setEmail("notif@test.com");
        user.setPassword("$2a$10$dummyhash00000000000000000000000000000000000000000000000");
        user.setRole(Role.USER);
        userRepository.save(user);

        User loaded = userRepository.findByEmail("notif@test.com").orElseThrow();
        String token = jwtUtil.generateToken(loaded);
        authHeader = "Bearer " + token;
    }

    @Test
    void postDeviceToken_validToken_returns200AndStoresRow() throws Exception {
        DeviceTokenRequest req = new DeviceTokenRequest();
        req.setToken("apns-token-abc123");

        mvc.perform(post("/api/notifications/device-token")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        assertTrue(deviceTokenRepository.findByToken("apns-token-abc123").isPresent());
    }

    @Test
    void postDeviceToken_sameTwice_noDuplicateRow() throws Exception {
        DeviceTokenRequest req = new DeviceTokenRequest();
        req.setToken("apns-token-duplicate");

        mvc.perform(post("/api/notifications/device-token")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        mvc.perform(post("/api/notifications/device-token")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        long count = deviceTokenRepository.findAll().stream()
                .filter(dt -> "apns-token-duplicate".equals(dt.getToken()))
                .count();
        assertEquals(1, count);
    }

    @Test
    void postDeviceToken_blankToken_returns400() throws Exception {
        DeviceTokenRequest req = new DeviceTokenRequest();
        req.setToken("  ");

        mvc.perform(post("/api/notifications/device-token")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postDeviceToken_noAuth_returns403() throws Exception {
        DeviceTokenRequest req = new DeviceTokenRequest();
        req.setToken("some-token");

        mvc.perform(post("/api/notifications/device-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteDeviceToken_existingToken_returns204AndRemovesRow() throws Exception {
        // First register
        DeviceTokenRequest req = new DeviceTokenRequest();
        req.setToken("apns-token-to-delete");

        mvc.perform(post("/api/notifications/device-token")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        // Then delete
        mvc.perform(delete("/api/notifications/device-token")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNoContent());

        assertTrue(deviceTokenRepository.findByToken("apns-token-to-delete").isEmpty());
    }

    @Test
    void deleteDeviceToken_tokenNotFound_returns204NoOp() throws Exception {
        DeviceTokenRequest req = new DeviceTokenRequest();
        req.setToken("nonexistent-token");

        mvc.perform(delete("/api/notifications/device-token")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNoContent());
    }
}
