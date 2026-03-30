import { Injectable } from '@angular/core';

export interface PayPeriod {
  startDate: string; // YYYY-MM-DD
  endDate: string;   // YYYY-MM-DD
}

@Injectable({ providedIn: 'root' })
export class PayPeriodService {
  private readonly STORAGE_KEY = 'payPeriod';

  getPayPeriod(): PayPeriod | null {
    const stored = localStorage.getItem(this.STORAGE_KEY);
    return stored ? JSON.parse(stored) : null;
  }

  setPayPeriod(period: PayPeriod): void {
    localStorage.setItem(this.STORAGE_KEY, JSON.stringify(period));
  }

  clearPayPeriod(): void {
    localStorage.removeItem(this.STORAGE_KEY);
  }
}
