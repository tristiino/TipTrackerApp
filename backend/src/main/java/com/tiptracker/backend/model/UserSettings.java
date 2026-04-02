package com.tiptracker.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

/**
 * Stores per-user application settings.
 * One row per user, created on first access with sensible defaults.
 * Enables cross-device settings sync — this row is the source of truth.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_settings")
public class UserSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private String theme;

    private String language;

    private double taxRate;

    private LocalDate payPeriodStartAnchor;

    private int payPeriodLengthDays;
}
