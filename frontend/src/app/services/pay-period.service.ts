import { Injectable } from '@angular/core';

/** The computed window for the current pay cycle — consumed by the dashboard and reports. */
export interface PayPeriod {
  startDate: string; // YYYY-MM-DD
  endDate: string;   // YYYY-MM-DD
}

/** What is persisted — an anchor date and a fixed cycle length in days. */
export interface PayPeriodConfig {
  startAnchor: string; // YYYY-MM-DD — the start of the user's first pay period
  lengthDays: number;  // e.g. 7 (weekly), 14 (bi-weekly), 15 (semi-monthly)
}

@Injectable({ providedIn: 'root' })
export class PayPeriodService {
  private readonly STORAGE_KEY = 'payPeriodConfig';

  getConfig(): PayPeriodConfig | null {
    const stored = localStorage.getItem(this.STORAGE_KEY);
    return stored ? JSON.parse(stored) : null;
  }

  setConfig(config: PayPeriodConfig): void {
    localStorage.setItem(this.STORAGE_KEY, JSON.stringify(config));
  }

  clearConfig(): void {
    localStorage.removeItem(this.STORAGE_KEY);
  }

  /**
   * Computes which pay-cycle window contains today based on the stored config.
   * Steps forward from the anchor in increments of lengthDays until reaching
   * the current period — no manual update needed when a cycle rolls over.
   */
  getCurrentPayPeriod(): PayPeriod | null {
    const config = this.getConfig();
    if (!config || !config.startAnchor || config.lengthDays <= 0) return null;

    const anchor = new Date(config.startAnchor + 'T00:00:00');
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const msPerDay = 24 * 60 * 60 * 1000;
    const daysSinceAnchor = Math.floor((today.getTime() - anchor.getTime()) / msPerDay);

    // If today is before the anchor, return the first period
    const periodsElapsed = daysSinceAnchor < 0 ? 0 : Math.floor(daysSinceAnchor / config.lengthDays);
    const currentStart = this.addDays(config.startAnchor, periodsElapsed * config.lengthDays);
    const currentEnd = this.addDays(currentStart, config.lengthDays - 1);

    return { startDate: currentStart, endDate: currentEnd };
  }

  // ---------------------------------------------------------------------------
  // Backward-compatible shims — used by SettingsComponent until Step 7 removes them
  // ---------------------------------------------------------------------------

  /** @deprecated Use getCurrentPayPeriod() */
  getPayPeriod(): PayPeriod | null {
    return this.getCurrentPayPeriod();
  }

  /** @deprecated Use setConfig() */
  setPayPeriod(period: PayPeriod): void {
    const start = new Date(period.startDate + 'T00:00:00');
    const end = new Date(period.endDate + 'T00:00:00');
    const msPerDay = 24 * 60 * 60 * 1000;
    const lengthDays = Math.round((end.getTime() - start.getTime()) / msPerDay) + 1;
    this.setConfig({ startAnchor: period.startDate, lengthDays });
  }

  /** @deprecated Use clearConfig() */
  clearPayPeriod(): void {
    this.clearConfig();
  }

  private addDays(dateStr: string, days: number): string {
    const d = new Date(dateStr + 'T00:00:00');
    d.setDate(d.getDate() + days);
    return d.toISOString().split('T')[0];
  }
}
