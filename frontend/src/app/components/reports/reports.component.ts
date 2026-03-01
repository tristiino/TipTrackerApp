import { Component, OnInit } from '@angular/core';
import { ReportService } from 'src/app/services/report.service';
import { AuthService } from 'src/app/services/auth.service';
import { TipService } from 'src/app/services/tip.service';
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

  constructor(
    private reportService: ReportService,
    private authService: AuthService,
    private tipService: TipService
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
  }

  /**
   * Fetches the report data from the backend based on the selected dates
   * for the currently logged-in user.
   */
  loadReport(): void {
    // BUG-07: validate that start date is not after end date before calling the API
    if (this.startDate > this.endDate) {
      this.dateRangeError = 'Start date cannot be after the end date.';
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
   * Allows the user to edit the amount of a tip entry.
   * @param tip The tip entry object to be edited.
   */
  editTip(tip: any): void {
    const newAmount = prompt('Enter new tip amount:', tip.amount);
    if (newAmount) {
      const updatedTip = { ...tip, amount: parseFloat(newAmount) };
      this.tipService.updateTip(tip.id, updatedTip).subscribe({
        next: () => this.loadReport(),
        error: (err) => console.error('Failed to update tip:', err)
      });
    }
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
   * Exports the current list of tip entries to a CSV file.
   */
  exportToCSV(): void {
    if (!this.report || !this.report.tipEntries || this.report.tipEntries.length === 0) {
      return;
    }
    const data = this.report.tipEntries;
    const headers = ['Date', 'Amount', 'Tip Share', 'Shift Type', 'Notes'];
    const rows = data.map((entry: any) =>
      [entry.date, entry.amount, entry.tipShare, entry.shiftType, JSON.stringify(entry.notes)].join(',')
    );
    const csvContent = [headers.join(','), ...rows].join('\n');
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    saveAs(blob, `tip-report-${this.startDate}-to-${this.endDate}.csv`);
  }
}
