package com.tiptracker.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

/**
 * Data Transfer Object for user settings.
 * Sent to and received from the client — never exposes the User entity directly.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSettingsDTO {
    private String theme;
    private String language;
    private double taxRate;
    private LocalDate payPeriodStartAnchor;
    private int payPeriodLengthDays;
}
