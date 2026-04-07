import { Component, HostListener, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { QuickAddService } from '../../services/quick-add.service';
import { TipService } from '../../services/tip.service';
import { TipOutRoleService } from '../../services/tip-out-role.service';
import { TipOutRole } from '../../models/tip-out-role.model';
import { JobService } from '../../services/job.service';
import { Job } from '../../models/job.model';

@Component({
  selector: 'app-quick-add-modal',
  templateUrl: './quick-add-modal.component.html',
  styleUrls: ['./quick-add-modal.component.scss']
})
export class QuickAddModalComponent implements OnInit {
  form: FormGroup;
  submitting = false;
  success = false;
  errorMsg = '';

  // Phase 2: tip-out role selection
  availableRoles: TipOutRole[] = [];
  selectedRoleIds: number[] = [];

  // Phase 2 Sprint 2: job selection
  jobs: Job[] = [];
  selectedJobId: number | null = null;
  private readonly LAST_JOB_KEY = 'lastUsedJobId';

  private readonly defaultStartTimes: Record<string, string> = {
    Morning: '08:00',
    Evening: '15:45',
    Night:   '18:00',
  };

  constructor(
    private fb: FormBuilder,
    public quickAdd: QuickAddService,
    private tipService: TipService,
    private tipOutRoleService: TipOutRoleService,
    private jobService: JobService
  ) {
    this.form = this.fb.group({
      cashTips:    ['', [Validators.required, Validators.min(0)]],
      creditTips:  ['', [Validators.required, Validators.min(0)]],
      date:        [new Date().toISOString().split('T')[0], Validators.required],
      shiftType:   ['', Validators.required],
      startTime:   [''],
      endTime:     [''],
      notes:       [''],
    });
  }

  ngOnInit(): void {
    this.quickAdd.isOpen$.subscribe(open => {
      if (open) this.resetForm();
    });
    this.tipOutRoleService.getRoles().subscribe({
      next: (roles) => this.availableRoles = roles,
      error: () => {}
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

  toggleRole(roleId: number): void {
    const idx = this.selectedRoleIds.indexOf(roleId);
    if (idx === -1) this.selectedRoleIds.push(roleId);
    else this.selectedRoleIds.splice(idx, 1);
  }

  isRoleSelected(roleId: number): boolean {
    return this.selectedRoleIds.includes(roleId);
  }

  formatRoleLabel(role: TipOutRole): string {
    return role.splitType === 'PERCENTAGE'
      ? `${role.name} — ${role.amount}%`
      : `${role.name} — $${role.amount.toFixed(2)} flat`;
  }

  get totalTips(): number {
    const cash   = parseFloat(this.form.get('cashTips')?.value)   || 0;
    const credit = parseFloat(this.form.get('creditTips')?.value) || 0;
    return cash + credit;
  }

  getRolePreviewAmount(role: TipOutRole): number {
    const cash   = parseFloat(this.form.get('cashTips')?.value)   || 0;
    const credit = parseFloat(this.form.get('creditTips')?.value) || 0;
    const base   = role.source === 'CASH' ? cash
                 : role.source === 'CREDIT' ? credit
                 : cash + credit;
    return role.splitType === 'PERCENTAGE' ? base * role.amount / 100 : role.amount;
  }

  get estimatedTipOut(): number {
    return this.availableRoles
      .filter(r => this.isRoleSelected(r.id!))
      .reduce((sum, r) => sum + this.getRolePreviewAmount(r), 0);
  }

  get estimatedNet(): number {
    return this.totalTips - this.estimatedTipOut;
  }

  get hoursWorked(): number | null {
    const start = this.form.get('startTime')?.value;
    const end   = this.form.get('endTime')?.value;
    if (!start || !end) return null;
    const [sh, sm] = start.split(':').map(Number);
    const [eh, em] = end.split(':').map(Number);
    const minutes = (eh * 60 + em) - (sh * 60 + sm);
    return minutes > 0 ? Math.round((minutes / 60) * 100) / 100 : null;
  }

  selectShift(shift: string): void {
    this.form.get('shiftType')?.setValue(shift);
    const saved = localStorage.getItem(`shiftStart_${shift}`);
    this.form.get('startTime')?.setValue(saved || this.defaultStartTimes[shift]);
  }

  onSubmit(): void {
    if (this.form.invalid || this.submitting) return;
    this.submitting = true;
    this.errorMsg = '';
    const payload = { ...this.form.value, tipOutRoleIds: this.selectedRoleIds, jobId: this.selectedJobId ?? undefined };
    if (this.selectedJobId) localStorage.setItem(this.LAST_JOB_KEY, String(this.selectedJobId));
    this.tipService.addTip(payload).subscribe({
      next: () => {
        const shift = this.form.get('shiftType')?.value;
        const start = this.form.get('startTime')?.value;
        if (shift && start) localStorage.setItem(`shiftStart_${shift}`, start);
        this.submitting = false;
        this.success = true;
        setTimeout(() => this.close(), 1500);
      },
      error: () => {
        this.submitting = false;
        this.errorMsg = 'Submission failed. Please try again.';
      }
    });
  }

  close(): void {
    this.quickAdd.close();
    this.success = false;
    this.errorMsg = '';
  }

  onBackdropClick(e: MouseEvent): void {
    if ((e.target as HTMLElement).classList.contains('quick-add-backdrop')) {
      this.close();
    }
  }

  @HostListener('document:keydown.escape')
  onEsc(): void { this.close(); }

  private resetForm(): void {
    this.form.reset({
      date: new Date().toISOString().split('T')[0],
      startTime: '', endTime: '',
    });
    this.selectedRoleIds = [];
    this.selectedJobId = null;
    this.success = false;
    this.errorMsg = '';
    this.submitting = false;
  }
}
