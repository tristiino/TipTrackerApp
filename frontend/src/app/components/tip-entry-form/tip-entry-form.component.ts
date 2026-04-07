import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { TipService } from '../../services/tip.service';
import { TipOutRoleService } from '../../services/tip-out-role.service';
import { TipOutRole } from '../../models/tip-out-role.model';
import { JobService } from '../../services/job.service';
import { Job } from '../../models/job.model';

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
  submitted = false;

  // --- Phase 2: Tip-Out fields ---
  availableRoles: TipOutRole[] = [];
  selectedRoleIds: number[] = [];

  // --- Phase 2 Sprint 2: Job fields ---
  jobs: Job[] = [];
  selectedJobId: number | null = null;
  private readonly LAST_JOB_KEY = 'lastUsedJobId';

  constructor(
    private fb: FormBuilder,
    private tipService: TipService,
    private tipOutRoleService: TipOutRoleService,
    private jobService: JobService
  ) {
    this.tipForm = this.fb.group({
      cashTips:    ['', [Validators.required, Validators.min(0)]],
      creditTips:  ['', [Validators.required, Validators.min(0)]],
      date:        [new Date().toISOString().split('T')[0], [Validators.required]],
      shiftType:   ['', [Validators.required]],
      notes:       [''],
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

  // --- Phase 2: Real-time tip-out preview (P2-003) ---

  /** Computes the estimated deduction for each selected role given current totalTips. */
  get tipOutPreview(): { role: TipOutRole; amount: number }[] {
    return this.selectedRoleIds
      .map(id => this.availableRoles.find(r => r.id === id))
      .filter((r): r is TipOutRole => !!r)
      .map(role => ({ role, amount: this.getRolePreviewAmount(role) }));
  }

  getRolePreviewAmount(role: TipOutRole): number {
    const cash   = parseFloat(this.tipForm.get('cashTips')?.value)   || 0;
    const credit = parseFloat(this.tipForm.get('creditTips')?.value) || 0;
    const base   = role.source === 'CASH' ? cash
                 : role.source === 'CREDIT' ? credit
                 : cash + credit;
    return role.splitType === 'PERCENTAGE' ? base * role.amount / 100 : role.amount;
  }

  get estimatedTipOut(): number {
    return this.tipOutPreview.reduce((sum, p) => sum + p.amount, 0);
  }

  get estimatedNet(): number {
    return this.totalTips - this.estimatedTipOut;
  }

  /**
   * Returns only roles relevant to the selected job:
   * - Global roles (no jobId) are always shown
   * - Job-specific roles are shown only when their job matches selectedJobId
   */
  get filteredRoles(): TipOutRole[] {
    return this.availableRoles.filter(r => !r.jobId || r.jobId === this.selectedJobId);
  }

  toggleRole(roleId: number): void {
    const idx = this.selectedRoleIds.indexOf(roleId);
    if (idx === -1) {
      this.selectedRoleIds.push(roleId);
    } else {
      this.selectedRoleIds.splice(idx, 1);
    }
  }

  isRoleSelected(roleId: number): boolean {
    return this.selectedRoleIds.includes(roleId);
  }

  formatRoleLabel(role: TipOutRole): string {
    return role.splitType === 'PERCENTAGE'
      ? `${role.name} — ${role.amount}%`
      : `${role.name} — $${role.amount.toFixed(2)} flat`;
  }

  ngOnInit(): void {
    this.loadRecentTips();
    this.tipOutRoleService.getRoles().subscribe({
      next: (roles) => this.availableRoles = roles,
      error: (err) => console.error('Failed to load tip-out roles', err)
    });
    this.jobService.getJobs().subscribe({
      next: (jobs) => {
        this.jobs = jobs;
        const lastId = localStorage.getItem(this.LAST_JOB_KEY);
        if (lastId && jobs.find(j => j.id === +lastId)) {
          this.selectedJobId = +lastId;
        }
      },
      error: () => {}
    });
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
    this.submitted = true;
    this.tipForm.markAllAsTouched();

    if (!this.tipForm.valid) {
      this.isError = true;
      this.submissionMessage = 'Submission unsuccessful. Please fix the errors above.';
      return;
    }

    const payload = {
      ...this.tipForm.value,
      tipOutRoleIds: this.selectedRoleIds,
      jobId: this.selectedJobId ?? undefined
    };

    this.tipService.addTip(payload).subscribe({
      next: () => {
        this.isError = false;
        this.submitted = false;
        this.submissionMessage = 'Tip submitted successfully!';
        const shift = this.tipForm.get('shiftType')?.value;
        const start = this.tipForm.get('startTime')?.value;
        if (shift && start) {
          localStorage.setItem(`shiftStart_${shift}`, start);
        }
        if (this.selectedJobId) localStorage.setItem(this.LAST_JOB_KEY, String(this.selectedJobId));
        this.tipForm.reset({ date: new Date().toISOString().split('T')[0], startTime: '', endTime: '' });
        this.selectedRoleIds = [];
        this.selectedJobId = null;
        this.loadRecentTips();
        setTimeout(() => this.submissionMessage = null, 3000);
      },
      error: () => {
        this.isError = true;
        this.submissionMessage = 'Submission failed. Please try again.';
        setTimeout(() => this.submissionMessage = null, 3000);
      }
    });
  }

  onCancel(): void {
    this.tipForm.reset({ date: new Date().toISOString().split('T')[0], startTime: '', endTime: '' });
  }
}
