package com.tiptracker.backend.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Represents a single Tip Entry entity in the database,
 * corresponding to one recorded tip by a user.
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "tip_entry", indexes = {
    @Index(name = "idx_tip_entry_user_date", columnList = "user_id, date")
})
public class TipEntry {

    /**
     * The unique identifier for the tip entry.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The monetary amount of the tip.
     */
    private double amount;

    /**
     * Cash tip amount.
     */
    @Column(nullable = true)
    private Double cashTips;

    /**
     * Credit card tip amount. Null for entries created before Sprint 2.
     */
    @Column(nullable = true)
    private Double creditTips;

    @Column(nullable = true)
    private LocalTime startTime;

    @Column(nullable = true)
    private LocalTime endTime;

    @Column(nullable = true)
    private Double hoursWorked;
    
    /**
     * The date the tip was received.
     */
    private LocalDate date;

    /**
     * The shift during which the tip was received (e.g., Morning, Evening).
     */
    private String shiftType;

    /**
     * Optional user-provided notes for the tip entry.
     */
    private String notes;

    /**
     * The User who this tip entry belongs to.
     * This represents the "many" side of a one-to-many relationship.
     * JsonBackReference is used to prevent infinite loops during JSON serialization.
     */
    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

}