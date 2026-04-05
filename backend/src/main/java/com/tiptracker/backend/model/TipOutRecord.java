package com.tiptracker.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Records the actual tip-out deduction made on a specific shift.
 *
 * There is one TipOutRecord per role applied to a shift.
 * e.g. a shift with Busser (5%) and Bartender (3%) applied
 *      will have two TipOutRecord rows linked to that TipEntry.
 *
 * Key design: computedAmount vs finalAmount
 * -----------------------------------------
 * computedAmount: what the formula produced at time of entry
 *                 (grossTips * role.amount/100, or role.amount for fixed)
 *
 * finalAmount:    the amount actually used for net calculation
 *                 Normally equals computedAmount.
 *                 If the user manually overrides it (P2-004),
 *                 finalAmount diverges and isOverridden is set to true.
 *
 * Why store both? So reports can show "you changed $12.00 to $10.00"
 * and flag it visually — giving full transparency without data loss.
 *
 * roleName is a snapshot of the role's name at time of entry.
 * This means renaming a role won't rewrite your history.
 */
@Data
@NoArgsConstructor
@Entity
@Table(
    name = "tip_out_record",
    indexes = @Index(name = "idx_tip_out_record_entry", columnList = "tip_entry_id")
)
public class TipOutRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tip_entry_id", nullable = false)
    private TipEntry tipEntry;

    /**
     * The template role that was applied. Nullable because a record
     * could theoretically be created manually without a template in
     * future iterations.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tip_out_role_id", nullable = true)
    private TipOutRole role;

    /**
     * Snapshot of the role name at time of entry.
     * Preserved even if the role is later renamed or deleted.
     */
    @Column(name = "role_name", nullable = false)
    private String roleName;

    /**
     * The amount the formula calculated. Never changed after creation.
     */
    @Column(name = "computed_amount", nullable = false)
    private double computedAmount;

    /**
     * The amount used in net tip calculations.
     * Starts equal to computedAmount; diverges if overridden.
     */
    @Column(name = "final_amount", nullable = false)
    private double finalAmount;

    /**
     * True if the user manually changed finalAmount away from computedAmount.
     * Used to visually flag overrides in shift history.
     */
    @Column(name = "is_overridden", nullable = false)
    private boolean isOverridden = false;
}
