import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TipService } from '../../services/tip.service';
import { TipOutRoleService } from '../../services/tip-out-role.service';
import { TipOutRole } from '../../models/tip-out-role.model';
import { JobService } from '../../services/job.service';
import { Job } from '../../models/job.model';
import { TagService } from '../../services/tag.service';
import { Tag } from '../../models/tag.model';

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

  isEditMode = false;
  editId: number | null = null;

  // --- Phase 2: Tip-Out fields ---
  availableRoles: TipOutRole[] = [];
  selectedRoleIds: number[] = [];

  // --- Phase 2 Sprint 2: Job fields ---
  jobs: Job[] = [];
  selectedJobId: number | null = null;
  private readonly LAST_JOB_KEY = 'lastUsedJobId';

  // --- Phase 2 Sprint 3: Tag fields (P2-014) ---
  allTags: Tag[] = [];          // all tags the user has ever created
  selectedTags: Tag[] = [];     // tags applied to this shift
  tagInputValue = '';           // what the user is typing
  tagSuggestions: Tag[] = [];   // filtered autocomplete list
  showSuggestions = false;

  constructor(
    private fb: FormBuilder,
    private tipService: TipService,
    private tipOutRoleService: TipOutRoleService,
    private jobService: JobService,
    private tagService: TagService,
    private route: ActivatedRoute,
    private router: Router
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
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.isEditMode = true;
      this.editId = +idParam;
      const tip = history.state?.tip;
      if (tip) this.prefillFromTip(tip);
    }

    this.loadRecentTips();
    this.tipOutRoleService.getRoles().subscribe({
      next: (roles) => {
        this.availableRoles = roles;
        // Re-apply role pre-selection after roles load (edit mode)
        if (this.isEditMode) {
          const tip = history.state?.tip;
          if (tip?.tipOutRecords) {
            this.selectedRoleIds = tip.tipOutRecords
              .filter((r: any) => r.roleId != null)
              .map((r: any) => r.roleId);
          }
        }
      },
      error: (err) => console.error('Failed to load tip-out roles', err)
    });
    this.jobService.getJobs().subscribe({
      next: (jobs) => {
        this.jobs = jobs;
        if (!this.isEditMode) {
          const lastId = localStorage.getItem(this.LAST_JOB_KEY);
          if (lastId && jobs.find(j => j.id === +lastId)) {
            this.selectedJobId = +lastId;
          }
        }
      },
      error: () => {}
    });

    // P2-014: load user's tags
    this.tagService.getTags().subscribe({
      next: (tags) => {
        this.allTags = tags;
        // Pre-select tags in edit mode
        if (this.isEditMode) {
          const tip = history.state?.tip;
          if (tip?.tags?.length) {
            this.selectedTags = tip.tags.filter((t: Tag) =>
              this.allTags.some(at => at.id === t.id)
            );
          }
        }
      },
      error: () => {}
    });
  }

  private prefillFromTip(tip: any): void {
    this.tipForm.patchValue({
      cashTips:   tip.cashTips   ?? tip.amount ?? 0,
      creditTips: tip.creditTips ?? 0,
      date:       tip.date,
      shiftType:  tip.shiftType  ?? '',
      notes:      tip.notes      ?? '',
      startTime:  tip.startTime  ?? '',
      endTime:    tip.endTime    ?? '',
    });
    this.selectedJobId = tip.jobId ?? null;
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
      jobId: this.selectedJobId ?? undefined,
      tagIds: this.selectedTags.map(t => t.id)
    };

    const request$ = this.isEditMode && this.editId != null
      ? this.tipService.updateTip(this.editId, payload)
      : this.tipService.addTip(payload);

    request$.subscribe({
      next: () => {
        this.isError = false;
        this.submitted = false;
        const shift = this.tipForm.get('shiftType')?.value;
        const start = this.tipForm.get('startTime')?.value;
        if (shift && start) localStorage.setItem(`shiftStart_${shift}`, start);
        if (this.selectedJobId) localStorage.setItem(this.LAST_JOB_KEY, String(this.selectedJobId));

        if (this.isEditMode) {
          this.router.navigate(['/reports']);
        } else {
          this.submissionMessage = 'Tip submitted successfully!';
          this.tipForm.reset({ date: new Date().toISOString().split('T')[0], startTime: '', endTime: '' });
          this.selectedRoleIds = [];
          this.selectedJobId = null;
          this.selectedTags = [];
          this.tagInputValue = '';
          this.loadRecentTips();
          setTimeout(() => this.submissionMessage = null, 3000);
        }
      },
      error: () => {
        this.isError = true;
        this.submissionMessage = this.isEditMode ? 'Update failed. Please try again.' : 'Submission failed. Please try again.';
        setTimeout(() => this.submissionMessage = null, 3000);
      }
    });
  }

  // --- P2-014: Tag input methods ---

  onTagInput(): void {
    const val = this.tagInputValue.trim().toLowerCase();
    if (val) {
      this.tagSuggestions = this.allTags.filter(t =>
        t.name.toLowerCase().includes(val) &&
        !this.selectedTags.some(st => st.id === t.id)
      );
      this.showSuggestions = true;
    } else {
      this.tagSuggestions = [];
      this.showSuggestions = false;
    }
  }

  onTagKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' || event.key === ',') {
      event.preventDefault();
      this.commitTagInput();
    } else if (event.key === 'Escape') {
      this.showSuggestions = false;
    }
  }

  selectSuggestion(tag: Tag): void {
    if (!this.selectedTags.some(t => t.id === tag.id)) {
      this.selectedTags.push(tag);
    }
    this.tagInputValue = '';
    this.tagSuggestions = [];
    this.showSuggestions = false;
  }

  commitTagInput(): void {
    const name = this.tagInputValue.trim();
    if (!name) return;

    // Check if exact match exists in allTags
    const existing = this.allTags.find(t => t.name.toLowerCase() === name.toLowerCase());
    if (existing) {
      this.selectSuggestion(existing);
      return;
    }

    // Create a new tag via the API
    this.tagService.createTag(name).subscribe({
      next: (newTag) => {
        this.allTags.push(newTag);
        this.selectedTags.push(newTag);
        this.tagInputValue = '';
        this.tagSuggestions = [];
        this.showSuggestions = false;
      },
      error: () => {}
    });
  }

  removeTag(tag: Tag): void {
    this.selectedTags = this.selectedTags.filter(t => t.id !== tag.id);
  }

  hideSuggestionsDelayed(): void {
    // Delay so click on suggestion fires first
    setTimeout(() => { this.showSuggestions = false; }, 150);
  }

  onCancel(): void {
    if (this.isEditMode) {
      this.router.navigate(['/reports']);
    } else {
      this.tipForm.reset({ date: new Date().toISOString().split('T')[0], startTime: '', endTime: '' });
    }
  }
}
