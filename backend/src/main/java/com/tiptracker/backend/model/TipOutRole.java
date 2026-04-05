package com.tiptracker.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A reusable tip-out role template owned by a user.
 *
 * Examples:
 *   name="Busser",     splitType=PERCENTAGE,   amount=5.0  → 5% of gross
 *   name="Bartender",  splitType=PERCENTAGE,   amount=3.0  → 3% of gross
 *   name="Host",       splitType=FIXED_AMOUNT, amount=10.0 → $10 flat
 *
 * These are templates only. The actual per-shift deduction amounts
 * are stored in TipOutRecord, not here. That way, changing a role
 * template does not retroactively alter historical shifts.
 */
@Data
@NoArgsConstructor
@Entity
@Table(
    name = "tip_out_role",
    indexes = @Index(name = "idx_tip_out_role_user", columnList = "user_id")
)
public class TipOutRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "split_type", nullable = false)
    private TipOutType splitType;

    /**
     * The value of the split.
     * For PERCENTAGE: 5.0 means 5%
     * For FIXED_AMOUNT: 10.0 means $10.00
     */
    @Column(nullable = false)
    private double amount;

    /**
     * Which tip pool this role pulls from: CASH, CREDIT, or BOTH (default).
     * Determines the base amount for percentage calculations.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    private TipOutSource source = TipOutSource.BOTH;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
