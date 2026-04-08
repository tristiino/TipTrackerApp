import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { TipOutRole, TipOutSource, TipOutType } from '../../models/tip-out-role.model';
import { TipOutRoleService } from '../../services/tip-out-role.service';
import { JobService } from '../../services/job.service';
import { Job } from '../../models/job.model';

/**
 * Manages the user's tip-out role templates (P2-001).
 *
 * Allows creating, editing, and deleting named roles like:
 *   "Busser — 5%" or "Host — $10 flat"
 *
 * Includes real-time validation:
 * - Warns if adding a PERCENTAGE role would push total percentage over 100%
 * - Shows total percentage currently allocated across all PERCENTAGE roles
 */
@Component({
  selector: 'app-tip-out-role-manager',
  templateUrl: './tip-out-role-manager.component.html',
  styleUrls: ['./tip-out-role-manager.component.scss']
})
export class TipOutRoleManagerComponent implements OnInit {

  roles: TipOutRole[] = [];
  jobs: Job[] = [];
  isFormVisible = false;
  editingRoleId: number | null = null;
  isLoading = false;
  errorMessage = '';
  successMessage = '';

  form: FormGroup;

  constructor(
    private tipOutRoleService: TipOutRoleService,
    private jobService: JobService,
    private fb: FormBuilder
  ) {
    this.form = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(50)]],
      splitType: ['PERCENTAGE', Validators.required],
      amount: [null, [Validators.required, Validators.min(0.01)]],
      source: ['BOTH', Validators.required],
      jobId: [null]
    });
  }

  ngOnInit(): void {
    this.loadRoles();
    this.jobService.getJobs().subscribe({ next: (jobs) => this.jobs = jobs, error: () => {} });
  }

  loadRoles(): void {
    this.tipOutRoleService.getRoles().subscribe({
      next: (roles) => this.roles = roles,
      error: () => this.errorMessage = 'Failed to load roles.'
    });
  }

  // --- Form helpers ---

  get splitType(): TipOutType {
    return this.form.get('splitType')!.value;
  }

  get source(): TipOutSource {
    return this.form.get('source')!.value;
  }

  get isPercentage(): boolean {
    return this.splitType === 'PERCENTAGE';
  }

  /** Sum of all saved PERCENTAGE roles in the same job scope (excluding the one being edited, if any). */
  get existingPercentageTotal(): number {
    const currentJobId = this.form.get('jobId')!.value ?? null;
    return this.roles
      .filter(r => r.splitType === 'PERCENTAGE' && r.id !== this.editingRoleId)
      .filter(r => (r.jobId ?? null) === (currentJobId === '' ? null : currentJobId))
      .reduce((sum, r) => sum + r.amount, 0);
  }

  /** Live total shown in the UI as the user types a new percentage amount. */
  get projectedPercentageTotal(): number {
    if (!this.isPercentage) return this.existingPercentageTotal;
    const current = this.form.get('amount')!.value || 0;
    return this.existingPercentageTotal + current;
  }

  /** True when the current form input would push percentage total over 100%. */
  get wouldExceed100(): boolean {
    return this.isPercentage && this.projectedPercentageTotal > 100;
  }

  // --- CRUD actions ---

  showAddForm(): void {
    this.editingRoleId = null;
    this.form.reset({ splitType: 'PERCENTAGE', source: 'BOTH', jobId: null });
    this.isFormVisible = true;
    this.errorMessage = '';
  }

  editRole(role: TipOutRole): void {
    this.editingRoleId = role.id!;
    this.form.patchValue({
      name: role.name,
      splitType: role.splitType,
      amount: role.amount,
      source: role.source ?? 'BOTH',
      jobId: role.jobId ?? null
    });
    this.isFormVisible = true;
    this.errorMessage = '';
  }

  saveRole(): void {
    if (this.form.invalid || this.wouldExceed100) return;

    this.isLoading = true;
    this.errorMessage = '';

    const rawJobId = this.form.value.jobId;
    const payload: Omit<TipOutRole, 'id'> = {
      name: this.form.value.name.trim(),
      splitType: this.form.value.splitType,
      amount: this.form.value.amount,
      source: this.form.value.source ?? 'BOTH',
      jobId: rawJobId ? +rawJobId : undefined
    };

    const request$ = this.editingRoleId
      ? this.tipOutRoleService.updateRole(this.editingRoleId, payload)
      : this.tipOutRoleService.createRole(payload);

    request$.subscribe({
      next: () => {
        this.successMessage = this.editingRoleId ? 'Role updated.' : 'Role created.';
        this.isFormVisible = false;
        this.editingRoleId = null;
        this.loadRoles();
        this.isLoading = false;
        setTimeout(() => this.successMessage = '', 3000);
      },
      error: (err) => {
        this.errorMessage = err.error?.error || 'Failed to save role.';
        this.isLoading = false;
      }
    });
  }

  deleteRole(role: TipOutRole): void {
    if (!confirm(`Delete "${role.name}"? This won't affect historical shifts.`)) return;

    this.tipOutRoleService.deleteRole(role.id!).subscribe({
      next: () => {
        this.roles = this.roles.filter(r => r.id !== role.id);
        this.successMessage = `"${role.name}" deleted.`;
        setTimeout(() => this.successMessage = '', 3000);
      },
      error: () => this.errorMessage = 'Failed to delete role.'
    });
  }

  cancelForm(): void {
    this.isFormVisible = false;
    this.editingRoleId = null;
    this.errorMessage = '';
  }

  /** Formats a role's amount for display: "5%" or "$10.00" */
  formatAmount(role: TipOutRole): string {
    return role.splitType === 'PERCENTAGE'
      ? `${role.amount}%`
      : `$${role.amount.toFixed(2)}`;
  }
}
