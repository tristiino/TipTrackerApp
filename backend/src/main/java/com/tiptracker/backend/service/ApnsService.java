package com.tiptracker.backend.service;

import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.ApnsClientBuilder;
import com.eatthepath.pushy.apns.PushNotificationResponse;
import com.eatthepath.pushy.apns.auth.ApnsSigningKey;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.eatthepath.pushy.apns.util.concurrent.PushNotificationFuture;
import com.tiptracker.backend.repository.DeviceTokenRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

/**
 * Wraps the Pushy APNs client. Initialized once at startup.
 * If apns.key-path is blank (local dev), sending is skipped with a warning.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApnsService {

    @Value("${apns.key-path:}")
    private String keyPath;

    @Value("${apns.key-id:}")
    private String keyId;

    @Value("${apns.team-id:}")
    private String teamId;

    @Value("${apns.bundle-id:com.yourname.tipslip}")
    private String bundleId;

    @Value("${apns.production:true}")
    private boolean production;

    private final DeviceTokenRepository deviceTokenRepository;

    private ApnsClient apnsClient;

    @PostConstruct
    public void init() {
        if (keyPath == null || keyPath.isBlank()) {
            log.warn("APNs key path not configured — push notifications are disabled. Set APNS_KEY_PATH to enable.");
            return;
        }
        try {
            apnsClient = new ApnsClientBuilder()
                    .setApnsServer(production
                            ? ApnsClientBuilder.PRODUCTION_APNS_HOST
                            : ApnsClientBuilder.DEVELOPMENT_APNS_HOST)
                    .setSigningKey(ApnsSigningKey.loadFromPkcs8File(new File(keyPath), teamId, keyId))
                    .build();
            log.info("APNs client initialized ({})", production ? "production" : "sandbox");
        } catch (Exception e) {
            log.error("Failed to initialize APNs client — push notifications disabled", e);
        }
    }

    /**
     * Sends a push notification to a single device token.
     * Handles APNs rejection by cleaning up stale tokens.
     */
    public void sendNotification(String deviceToken, String title, String body) {
        if (apnsClient == null) {
            log.debug("APNs client not available — skipping notification for token ending ...{}",
                    deviceToken.length() > 8 ? deviceToken.substring(deviceToken.length() - 8) : deviceToken);
            return;
        }
        try {
            String payload = new com.eatthepath.pushy.apns.util.SimpleApnsPayloadBuilder()
                    .setAlertTitle(title)
                    .setAlertBody(body)
                    .setSound("default")
                    .build();

            SimpleApnsPushNotification notification =
                    new SimpleApnsPushNotification(deviceToken, bundleId, payload);

            PushNotificationFuture<SimpleApnsPushNotification, PushNotificationResponse<SimpleApnsPushNotification>> future =
                    apnsClient.sendNotification(notification);

            PushNotificationResponse<SimpleApnsPushNotification> response = future.get();
            if (!response.isAccepted()) {
                String reason = response.getRejectionReason().orElse("unknown");
                handleRejection(deviceToken, reason);
            }
        } catch (Exception e) {
            log.error("Error sending APNs notification to token ending ...{}",
                    deviceToken.length() > 8 ? deviceToken.substring(deviceToken.length() - 8) : deviceToken, e);
        }
    }

    private void handleRejection(String token, String reason) {
        if (List.of("BadDeviceToken", "Unregistered", "DeviceTokenNotForTopic").contains(reason)) {
            deviceTokenRepository.deleteByToken(token);
            log.info("Removed stale APNs token. Reason: {}", reason);
        } else {
            log.warn("APNs rejection: {} for token ending ...{}",
                    reason, token.length() > 8 ? token.substring(token.length() - 8) : token);
        }
    }
}
