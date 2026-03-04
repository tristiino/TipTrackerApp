import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { TipService } from '../../services/tip.service';

@Component({
  selector: 'app-tip-entry-form',
  templateUrl: './tip-entry-form.component.html',
  styleUrls: ['./tip-entry-form.component.scss']
})
export class TipEntryFormComponent implements OnInit {
  tipForm: FormGroup;
  submissionMessage: string | null = null;
  isError: boolean = false;
  recentTips: any[] = [];

  constructor(
    private fb: FormBuilder,
    private tipService: TipService
  ) {
    this.tipForm = this.fb.group({
      cashTips:    ['', [Validators.required, Validators.min(0)]],
      creditTips:  ['', [Validators.required, Validators.min(0)]],
      date:        [new Date().toISOString().split('T')[0], [Validators.required]],
      shiftType:   ['', [Validators.required]],
      notes:       [''],
      peopleInPool:['', [Validators.required, Validators.min(1)]],
      startTime:   [''],
      endTime:     [''],

    });
  }
  /**
   * Calculates the hours worked based on the start and end times.
   * @returns The hours worked, or null if the start or end time is not set.
   */
  get hoursWorked(): number | null {
    const start = this.tipForm.get('startTime')?.value;
    const end = this.tipForm.get('endTime')?.value;
    if (!start || !end) return null;
    const [sh, sm] = start.split(':').map(Number);
    const [eh, em] = end.split(':').map(Number);
    let minutes = (eh * 60 + em) - (sh * 60 + sm);
    if (minutes <= 0) return null;
    return Math.round((minutes / 60) * 100) / 100;
  }


  /**
   * Computes the real-time total of cash + credit tips (P1-008).
   * Angular's change detection re-evaluates this getter on every form value change.
   */
  get totalTips(): number {
    const cash   = parseFloat(this.tipForm.get('cashTips')?.value)   || 0;
    const credit = parseFloat(this.tipForm.get('creditTips')?.value) || 0;
    return cash + credit;
  }

  ngOnInit(): void {
    this.loadRecentTips();
  }

  loadRecentTips(): void {
    this.tipService.getRecentTips().subscribe({
      next: (tips) => this.recentTips = tips,
      error: (err) => console.error('Failed to load recent tips', err)
    });
  }

  private readonly defaultStartTimes: Record<string, string> = {
    Morning: '08:00',
    Evening: '15:45',
    Night:   '18:00',
  };

  selectShift(shift: string): void {
    this.tipForm.get('shiftType')?.setValue(shift);
    const saved = localStorage.getItem(`shiftStart_${shift}`);
    this.tipForm.get('startTime')?.setValue(saved || this.defaultStartTimes[shift]);
  }

  onSubmit(): void {
    if (this.tipForm.valid) {
      this.tipService.addTip(this.tipForm.value).subscribe({
        next: (res: any) => {
          this.isError = false;
          this.submissionMessage = 'Tip submitted successfully!';
          const shift = this.tipForm.get('shiftType')?.value;
          const start = this.tipForm.get('startTime')?.value;
          if (shift && start) {
            localStorage.setItem(`shiftStart_${shift}`, start);
          }
          this.tipForm.reset({ date: new Date().toISOString().split('T')[0], startTime: '', endTime: '' });
          this.loadRecentTips();
          setTimeout(() => this.submissionMessage = null, 3000);
        },
        error: (err: any) => {
          this.isError = true;
          this.submissionMessage = 'Submission failed. Please try again.';
          setTimeout(() => this.submissionMessage = null, 3000);
        }
      });
    }
  }

  onCancel(): void {
    this.tipForm.reset({ date: new Date().toISOString().split('T')[0], startTime: '', endTime: '' });
  }
}
