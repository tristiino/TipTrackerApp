package com.tiptracker.backend.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Represents a single Tip Entry entity in the database,
 * corresponding to one recorded tip by a user.
 */
@Data
@NoArgsConstructor
@Entity
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