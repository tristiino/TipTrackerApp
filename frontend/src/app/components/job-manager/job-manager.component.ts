import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Job } from '../../models/job.model';
import { JobService } from '../../services/job.service';

@Component({
  selector: 'app-job-manager',
  templateUrl: './job-manager.component.html',
  styleUrls: ['./job-manager.component.scss']
})
export class JobManagerComponent implements OnInit {

  readonly MAX_JOBS = 10;

  jobs: Job[] = [];
  isFormVisible = false;
  editingJobId: number | null = null;
  isLoading = false;
  errorMessage = '';
  successMessage = '';

  form: FormGroup;

  constructor(private jobService: JobService, private fb: FormBuilder) {
    this.form = this.fb.group({
      name:       ['', [Validators.required, Validators.maxLength(60)]],
      location:   [''],
      hourlyWage: [null, [Validators.min(0)]]
    });
  }

  ngOnInit(): void {
    this.loadJobs();
  }

  loadJobs(): void {
    this.jobService.getJobs().subscribe({
      next: (jobs) => this.jobs = jobs,
      error: () => this.errorMessage = 'Failed to load jobs.'
    });
  }

  get atLimit(): boolean {
    return this.jobs.length >= this.MAX_JOBS;
  }

  showAddForm(): void {
    this.editingJobId = null;
    this.form.reset();
    this.isFormVisible = true;
    this.errorMessage = '';
  }

  editJob(job: Job): void {
    this.editingJobId = job.id!;
    this.form.patchValue({
      name:       job.name,
      location:   job.location ?? '',
      hourlyWage: job.hourlyWage ?? null
    });
    this.isFormVisible = true;
    this.errorMessage = '';
  }

  saveJob(): void {
    if (this.form.invalid) return;

    this.isLoading = true;
    this.errorMessage = '';

    const payload: Omit<Job, 'id'> = {
      name:       this.form.value.name.trim(),
      location:   this.form.value.location?.trim() || undefined,
      hourlyWage: this.form.value.hourlyWage ?? undefined
    };

    const request$ = this.editingJobId
      ? this.jobService.updateJob(this.editingJobId, payload)
      : this.jobService.createJob(payload);

    request$.subscribe({
      next: () => {
        this.successMessage = this.editingJobId ? 'Job updated.' : 'Job created.';
        this.isFormVisible = false;
        this.editingJobId = null;
        this.loadJobs();
        this.isLoading = false;
        setTimeout(() => this.successMessage = '', 3000);
      },
      error: (err) => {
        this.errorMessage = err.error?.message || err.error?.error || 'Failed to save job.';
        this.isLoading = false;
      }
    });
  }

  deleteJob(job: Job): void {
    if (!confirm(`Delete "${job.name}"? Existing shifts logged against this job will become unassigned.`)) return;

    this.jobService.deleteJob(job.id!).subscribe({
      next: () => {
        this.jobs = this.jobs.filter(j => j.id !== job.id);
        this.successMessage = `"${job.name}" deleted.`;
        setTimeout(() => this.successMessage = '', 3000);
      },
      error: () => this.errorMessage = 'Failed to delete job.'
    });
  }

  cancelForm(): void {
    this.isFormVisible = false;
    this.editingJobId = null;
    this.errorMessage = '';
  }
}
