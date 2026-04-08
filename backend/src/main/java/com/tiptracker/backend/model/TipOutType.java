package com.tiptracker.backend.model;

/**
 * Defines how a tip-out role calculates its deduction amount.
 *
 * PERCENTAGE: deduction = grossTips * (amount / 100)
 *             e.g. amount=5.0 means "5% of gross tips"
 *
 * FIXED_AMOUNT: deduction = amount, regardless of gross tips
 *               e.g. amount=10.0 means "always $10.00"
 */
public enum TipOutType {
    PERCENTAGE,
    FIXED_AMOUNT
}
