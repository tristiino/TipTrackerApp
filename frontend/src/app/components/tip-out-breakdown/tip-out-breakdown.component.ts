import { Component, Input, Output, EventEmitter, OnChanges } from '@angular/core';
import { TipOutRecord } from '../../models/tip-out-role.model';

/**
 * Reusable component that displays the tip-out deductions for a single shift.
 *
 * Used in two contexts:
 *   1. Inside tip-entry-form — shows real-time preview before submit (readonly=false)
 *   2. Inside reports — shows historical breakdown with override support (readonly=false)
 *
 * P2-003: Shows net tips after deductions (grossTips - totalTipOut)
 * P2-004: Allows overriding individual record amounts; overridden rows are flagged
 */
@Component({
  selector: 'app-tip-out-breakdown',
  templateUrl: './tip-out-breakdown.component.html',
  styleUrls: ['./tip-out-breakdown.component.scss']
})
export class TipOutBreakdownComponent implements OnChanges {

  /** The tip-out records to display. Pass [] for shifts with no roles applied. */
  @Input() records: TipOutRecord[] = [];

  /** The gross tip amount for this shift (used to display netTips). */
  @Input() grossTips: number = 0;

  /**
   * When true, hides edit/override controls.
   * Use true for read-only views, false for editable history.
   */
  @Input() readonly: boolean = false;

  /**
   * Emitted when the user saves an override.
   * Parent is responsible for calling the API and refreshing data.
   */
  @Output() recordOverridden = new EventEmitter<{ recordId: number; finalAmount: number }>();

  /** Which record is currently being edited (by id). null = none. */
  editingRecordId: number | null = null;

  /** Holds the in-progress override value while the user is editing. */
  overrideValue: number = 0;

  get totalTipOut(): number {
    return this.records.reduce((sum, r) => sum + r.finalAmount, 0);
  }

  get netTips(): number {
    return this.grossTips - this.totalTipOut;
  }

  get hasRecords(): boolean {
    return this.records.length > 0;
  }

  ngOnChanges(): void {
    // Cancel any in-progress edit if records change (e.g. after reload)
    this.editingRecordId = null;
  }

  startOverride(record: TipOutRecord): void {
    this.editingRecordId = record.id!;
    this.overrideValue = record.finalAmount;
  }

  saveOverride(record: TipOutRecord): void {
    if (this.overrideValue < 0) return;
    this.recordOverridden.emit({ recordId: record.id!, finalAmount: this.overrideValue });
    this.editingRecordId = null;
  }

  cancelOverride(): void {
    this.editingRecordId = null;
  }

  isEditing(record: TipOutRecord): boolean {
    return this.editingRecordId === record.id;
  }
}
