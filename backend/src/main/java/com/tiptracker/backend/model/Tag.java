package com.tiptracker.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a user-defined tag that can be applied to shift entries.
 * Tags are scoped per user — two users can have tags with the same name independently.
 *
 * P2-014: Shift Notes, Tags & Search
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "shift_tag", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"name", "user_id"})
})
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /** Owning user — stored as a plain FK to avoid bidirectional complexity. */
    @Column(name = "user_id", nullable = false)
    private Long userId;
}
