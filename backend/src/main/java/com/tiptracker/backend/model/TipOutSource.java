package com.tiptracker.backend.model;

/**
 * Defines which tip pool a tip-out role draws from.
 *
 * CASH   — percentage/fixed amount applies only to the cash tips portion
 * CREDIT — percentage/fixed amount applies only to the credit tips portion
 * BOTH   — applies to the combined total (default, previous behaviour)
 */
public enum TipOutSource {
    CASH,
    CREDIT,
    BOTH
}
