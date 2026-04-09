import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ReportService } from 'src/app/services/report.service';
import { AuthService } from 'src/app/services/auth.service';
import { TipService } from 'src/app/services/tip.service';
import { localDateString } from 'src/app/utils/date.utils';
import { TipOutRoleService } from '../../services/tip-out-role.service';
import { JobService } from '../../services/job.service';
import { Job } from '../../models/job.model';
import { TagService } from '../../services/tag.service';
import { Tag } from '../../models/tag.model';
import { PayPeriodService } from '../../services/pay-period.service';
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

  // P2-013: expandable row for full note detail
  expandedEntryId: number | null = null;

  // P2-018: calendar view toggle — persisted in localStorage
  private readonly VIEW_MODE_KEY = 'reportsViewMode';
  viewMode: 'table' | 'calendar' = (localStorage.getItem('reportsViewMode') as 'table' | 'calendar') ?? 'calendar';

  setViewMode(mode: 'table' | 'calendar'): void {
    this.viewMode = mode;
    localStorage.setItem(this.VIEW_MODE_KEY, mode);
  }

  get calendarDays(): { date: string; entries: any[] }[] {
    if (!this.report?.tipEntries) return [];

    // Build a map of date → entries from filteredEntries
    const map = new Map<string, any[]>();
    for (const e of this.filteredEntries) {
      const d = e.date;
      if (!map.has(d)) map.set(d, []);
      map.get(d)!.push(e);
    }

    // Walk every day in the range
    const days: { date: string; entries: any[] }[] = [];
    const start = new Date(this.startDate + 'T00:00:00');
    const end   = new Date(this.endDate   + 'T00:00:00');
    for (let d = new Date(start); d <= end; d.setDate(d.getDate() + 1)) {
      const key = localDateString(d);
      days.push({ date: key, entries: map.get(key) ?? [] });
    }
    return days;
  }

  /** Returns the offset (0=Sun … 6=Sat) of the first day so the grid aligns correctly. */
  get calendarStartOffset(): number {
    if (!this.startDate) return 0;
    return new Date(this.startDate + 'T00:00:00').getDay();
  }

  sumNetTips(entries: any[]): number {
    return entries.reduce((s, e) => s + (e.netTips ?? e.amount ?? 0), 0);
  }

  // P2-015: keyword + tag search
  searchKeyword = '';
  filterTagId: number | null = null;
  allTags: Tag[] = [];


  get filteredEntries(): any[] {
    if (!this.report?.tipEntries) return [];
    let entries: any[] = this.report.tipEntries;

    // Job filter (client-side)
    if (this.selectedJobId !== null) {
      entries = entries.filter((e: any) =>
        this.selectedJobId === 0 ? !e.jobId : e.jobId === this.selectedJobId
      );
    }

    // Keyword search — matches notes (case-insensitive)
    const kw = this.searchKeyword.trim().toLowerCase();
    if (kw) {
      entries = entries.filter((e: any) =>
        e.notes && e.notes.toLowerCase().includes(kw)
      );
    }

    // Tag filter
    if (this.filterTagId !== null) {
      entries = entries.filter((e: any) =>
        e.tags?.some((t: any) => t.id === this.filterTagId)
      );
    }

    return entries;
  }

  constructor(
    private reportService: ReportService,
    private authService: AuthService,
    private tipService: TipService,
    private tipOutRoleService: TipOutRoleService,
    private jobService: JobService,
    private tagService: TagService,
    private payPeriodService: PayPeriodService,
    private router: Router
  ) {
    // Use the current pay period if configured, otherwise fall back to last 14 days.
    const payPeriod = this.payPeriodService.getCurrentPayPeriod();
    if (payPeriod) {
      this.startDate = payPeriod.startDate;
      this.endDate = payPeriod.endDate;
    } else {
      const today = new Date();
      const twoWeeksAgo = new Date();
      twoWeeksAgo.setDate(today.getDate() - 14);
      this.endDate = localDateString(today);
      this.startDate = localDateString(twoWeeksAgo);
    }
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
    this.tagService.getTags().subscribe({
      next: (tags) => this.allTags = tags,
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
  toggleExpand(entryId: number, hasNotes: boolean): void {
    if (!hasNotes) return;
    this.expandedEntryId = this.expandedEntryId === entryId ? null : entryId;
  }

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
