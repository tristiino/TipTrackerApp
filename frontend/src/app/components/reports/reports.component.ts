import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ReportService } from 'src/app/services/report.service';
import { AuthService } from 'src/app/services/auth.service';
import { TipService } from 'src/app/services/tip.service';
import { TipOutRoleService } from '../../services/tip-out-role.service';
import { JobService } from '../../services/job.service';
import { Job } from '../../models/job.model';
import { saveAs } from 'file-saver';


@Component({
  selector: 'app-reports',
  templateUrl: './reports.component.html',
  styleUrls: ['./reports.component.scss']
})
export class ReportsComponent implements OnInit {
  report: any = null;
  startDate: string;
  endDate: string;
  isLoading = false;
  dateRangeError: string = '';

  // P2-009: job filter
  jobs: Job[] = [];
  selectedJobId: number | null = null;

  get filteredEntries(): any[] {
    if (!this.report?.tipEntries) return [];
    if (this.selectedJobId === null) return this.report.tipEntries;
    return this.report.tipEntries.filter((e: any) =>
      this.selectedJobId === 0 ? !e.jobId : e.jobId === this.selectedJobId
    );
  }

  constructor(
    private reportService: ReportService,
    private authService: AuthService,
    private tipService: TipService,
    private tipOutRoleService: TipOutRoleService,
    private jobService: JobService,
    private router: Router
  ) {
    // Set a default date range for the last 14 days.
    const today = new Date();
    const lastWeek = new Date();
    lastWeek.setDate(today.getDate() - 14);
    this.endDate = today.toISOString().split('T')[0];
    this.startDate = lastWeek.toISOString().split('T')[0];
  }

  /**
   * On component initialization, load the default report.
   */
  ngOnInit(): void {
    this.loadReport();
    this.jobService.getJobs().subscribe({
      next: (jobs) => this.jobs = jobs,
      error: () => {}
    });
  }

  /**
   * Fetches the report data from the backend based on the selected dates
   * for the currently logged-in user.
   */
  loadReport(): void {
    if (this.startDate && this.endDate && new Date(this.startDate) > new Date(this.endDate)) {
      this.dateRangeError = 'Start date cannot be after the end date.';
      this.report = null;
      return;
    }
    this.dateRangeError = '';

    const user = this.authService.getUser();
    if (!user || !user.id) {
      console.error('User not logged in or user ID is missing.');
      return;
    }
    this.isLoading = true;
    this.report = null;
    this.reportService.getReportSummary(user.id, this.startDate, this.endDate).subscribe({
      next: (data) => {
        this.report = data;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error fetching report:', error);
        this.isLoading = false;
      }
    });
  }

  /**
   * Navigates to the full edit form, passing the entry as router state.
   */
  editTip(tip: any): void {
    this.router.navigate(['/edit', tip.id], { state: { tip } });
  }

  /**
   * Deletes a tip entry after user confirmation.
   * @param id The ID of the tip entry to delete.
   */
  deleteTip(id: number): void {
    if (confirm('Are you sure you want to delete this tip entry?')) {
      this.tipService.deleteTip(id).subscribe({
        next: () => this.loadReport(),
        error: (err) => console.error('Failed to delete tip:', err)
      });
    }
  }

  /**
   * Handles a tip-out record override emitted by <app-tip-out-breakdown>.
   * Calls the API to persist the change, then reloads the report so totals update.
   */
  onRecordOverridden(event: { recordId: number; finalAmount: number }): void {
    this.tipOutRoleService.overrideRecord(event.recordId, event.finalAmount).subscribe({
      next: () => this.loadReport(),
      error: (err) => console.error('Failed to save override:', err)
    });
  }

  /**
   * Exports the current list of tip entries to a CSV file.
   * Phase 2: includes totalTipOut and netTips columns.
   */
  exportToCSV(): void {
    if (!this.report || !this.report.tipEntries || this.report.tipEntries.length === 0) {
      return;
    }
    const data = this.report.tipEntries;
    const headers = ['Date', 'Gross Amount', 'Total Tip-Out', 'Net Tips', 'Shift Type', 'Notes'];
    const rows = data.map((entry: any) =>
      [
        entry.date,
        entry.amount,
        entry.totalTipOut ?? entry.tipShare ?? 0,
        entry.netTips ?? entry.amount,
        entry.shiftType,
        JSON.stringify(entry.notes ?? '')
      ].join(',')
    );
    const csvContent = [headers.join(','), ...rows].join('\n');
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    saveAs(blob, `tip-report-${this.startDate}-to-${this.endDate}.csv`);
  }
}
