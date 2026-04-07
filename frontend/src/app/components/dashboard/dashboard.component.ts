import { Component, OnInit, ViewChild } from '@angular/core';
import { ChartConfiguration, ChartData, ChartOptions, ChartType } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';
import { forkJoin } from 'rxjs';
import { TipService } from '../../services/tip.service';
import { PayPeriodService, PayPeriod } from '../../services/pay-period.service';
import { JobService } from '../../services/job.service';
import { Job } from '../../models/job.model';

interface Metric {
  key: string;
  label: string;
  datasetIndex: number;
  color: string;
  bgColor: string;
  dataKey: string;
}

interface TimeRange {
  label: string;
  days: number;
}

type GroupBy = 'payperiod' | 'day' | 'week' | 'month';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {

  @ViewChild(BaseChartDirective) chart?: BaseChartDirective;

  chartType: ChartType = 'line';
  activeMetric = 'total';
  selectedDays = 30;
  groupBy: GroupBy = 'payperiod';
  isLoading = true;

  payPeriod: PayPeriod | null = null;
  selectedPeriodOffset = 0;
  availablePeriods: { label: string; offset: number; period: PayPeriod }[] = [];

  jobs: Job[] = [];
  selectedJobId: number | null = null;

  periodTotal = 0;
  activeDays  = 0;
  totalCash   = 0;
  totalCredit = 0;
  summary: any = null;

  private rawData: any[] = [];

  groupByOptions: { key: GroupBy; label: string }[] = [
    { key: 'payperiod', label: 'Pay Period' },
    { key: 'day',       label: 'Daily'      },
    { key: 'week',      label: 'Weekly'     },
    { key: 'month',     label: 'Monthly'    },
  ];

  private timeRangeMap: Record<string, TimeRange[]> = {
    day:   [{ label: '7 Days',  days: 7   }, { label: '2 Weeks', days: 14  }, { label: '30 Days', days: 30  }],
    week:  [{ label: '4 Weeks', days: 28  }, { label: '8 Weeks', days: 56  }, { label: '12 Weeks', days: 84 }],
    month: [{ label: '3 Mo',    days: 90  }, { label: '6 Mo',    days: 180 }, { label: '12 Mo',   days: 365 }],
  };

  get timeRanges(): TimeRange[] {
    if (this.groupBy === 'payperiod') return [];
    return this.timeRangeMap[this.groupBy];
  }

  get periodLabel(): string {
    if (this.groupBy === 'payperiod') {
      if (!this.payPeriod) return 'No pay period set';
      return `${this.formatDisplayDate(this.payPeriod.startDate)} – ${this.formatDisplayDate(this.payPeriod.endDate)}`;
    }
    return this.timeRanges.find(r => r.days === this.selectedDays)?.label ?? '';
  }

  metrics: Metric[] = [
    { key: 'total',  label: 'Total Tips',   datasetIndex: 0, color: '#0d6efd', bgColor: 'rgba(13, 110, 253, 0.12)',  dataKey: 'totalTips'   },
    { key: 'cash',   label: 'Cash',         datasetIndex: 1, color: '#198754', bgColor: 'rgba(25, 135, 84, 0.12)',   dataKey: 'cashTips'    },
    { key: 'credit', label: 'Credit',       datasetIndex: 2, color: '#ffc107', bgColor: 'rgba(255, 193, 7, 0.12)',   dataKey: 'creditTips'  },
    { key: 'net',    label: 'Net Earnings', datasetIndex: 3, color: '#6f42c1', bgColor: 'rgba(111, 66, 193, 0.12)',  dataKey: 'netEarnings' },
    // Phase 2 (P2-006): Gross tips before tip-outs — enables the gross vs. net comparison
    { key: 'gross',  label: 'Gross Tips',   datasetIndex: 4, color: '#20c997', bgColor: 'rgba(32, 201, 151, 0.12)', dataKey: 'grossTips'   },
  ];

  get activeMetricDef(): Metric {
    return this.metrics.find(m => m.key === this.activeMetric) ?? this.metrics[0];
  }

  chartData: ChartData<'line'> = {
    labels: [],
    datasets: [
      { label: 'Total Tips',   data: [], borderColor: '#0d6efd', backgroundColor: 'rgba(13, 110, 253, 0.12)',  fill: true, tension: 0.4, borderWidth: 2.5, pointRadius: 0, pointHoverRadius: 5, pointHoverBackgroundColor: '#0d6efd', pointHoverBorderColor: '#fff', pointHoverBorderWidth: 2 },
      { label: 'Cash',         data: [], borderColor: '#198754', backgroundColor: 'rgba(25, 135, 84, 0.12)',   fill: true, tension: 0.4, borderWidth: 2.5, pointRadius: 0, pointHoverRadius: 5, pointHoverBackgroundColor: '#198754', pointHoverBorderColor: '#fff', pointHoverBorderWidth: 2, hidden: true },
      { label: 'Credit',       data: [], borderColor: '#ffc107', backgroundColor: 'rgba(255, 193, 7, 0.12)',   fill: true, tension: 0.4, borderWidth: 2.5, pointRadius: 0, pointHoverRadius: 5, pointHoverBackgroundColor: '#ffc107', pointHoverBorderColor: '#fff', pointHoverBorderWidth: 2, hidden: true },
      { label: 'Net Earnings', data: [], borderColor: '#6f42c1', backgroundColor: 'rgba(111, 66, 193, 0.12)', fill: true, tension: 0.4, borderWidth: 2.5, pointRadius: 0, pointHoverRadius: 5, pointHoverBackgroundColor: '#6f42c1', pointHoverBorderColor: '#fff', pointHoverBorderWidth: 2, hidden: true },
      // Phase 2 (P2-006): Gross tips dataset — hidden by default, toggled via metric buttons
      { label: 'Gross Tips',   data: [], borderColor: '#20c997', backgroundColor: 'rgba(32, 201, 151, 0.12)', fill: true, tension: 0.4, borderWidth: 2.5, pointRadius: 0, pointHoverRadius: 5, pointHoverBackgroundColor: '#20c997', pointHoverBorderColor: '#fff', pointHoverBorderWidth: 2, hidden: true },
    ],
  };

  chartOptions: ChartConfiguration['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    interaction: { mode: 'index', intersect: false },
    plugins: {
      legend: { display: false },
      tooltip: {
        backgroundColor: 'rgba(15, 23, 42, 0.95)',
        titleColor: '#94a3b8',
        bodyColor: '#f1f5f9',
        borderColor: 'rgba(255,255,255,0.1)',
        borderWidth: 1,
        padding: 12,
        callbacks: {
          label: (ctx) => ` ${ctx.dataset.label}: $${(ctx.parsed.y ?? 0).toFixed(2)}`,
        },
      },
    },
    scales: {
      x: {
        grid: { display: false },
        border: { display: false },
        ticks: { maxTicksLimit: 8, color: '#64748b', font: { size: 11 } },
      },
      y: {
        beginAtZero: true,
        grid: { color: 'rgba(255,255,255,0.06)' },
        border: { display: false },
        ticks: { color: '#64748b', font: { size: 11 }, callback: (v) => `$${v}` },
      },
    },
  };

  doughnutData: ChartData<'doughnut'> = {
    labels: ['Cash', 'Credit'],
    datasets: [{
      data: [0, 0],
      backgroundColor: ['#198754', '#0d6efd'],
      hoverBackgroundColor: ['#157347', '#0b5ed7'],
      borderWidth: 0,
      hoverOffset: 4,
    }],
  };

  doughnutOptions: ChartOptions<'doughnut'> = {
    responsive: true,
    maintainAspectRatio: false,
    cutout: '68%',
    plugins: {
      legend: { display: false },
      tooltip: {
        backgroundColor: 'rgba(15, 23, 42, 0.95)',
        titleColor: '#94a3b8',
        bodyColor: '#f1f5f9',
        borderColor: 'rgba(255,255,255,0.1)',
        borderWidth: 1,
        padding: 12,
        callbacks: {
          label: (ctx) => {
            const total = (ctx.dataset.data as number[]).reduce((a, b) => a + b, 0);
            const pct = total > 0 ? ((ctx.parsed / total) * 100).toFixed(1) : '0.0';
            return ` ${ctx.label}: $${(ctx.parsed ?? 0).toFixed(2)} (${pct}%)`;
          },
        },
      },
    },
  };

  constructor(
    private tipService: TipService,
    private payPeriodService: PayPeriodService,
    private jobService: JobService,
  ) {}

  ngOnInit(): void {
    this.jobService.getJobs().subscribe(jobs => { this.jobs = jobs; });
    this.buildAvailablePeriods();
    this.payPeriod = this.availablePeriods.find(p => p.offset === 0)?.period ?? null;
    if (this.payPeriod) {
      this.loadPayPeriodData();
    } else {
      this.isLoading = false;
    }
  }

  setGroupBy(key: GroupBy): void {
    this.groupBy = key;
    if (key === 'payperiod') {
      this.buildAvailablePeriods();
      this.selectedPeriodOffset = 0;
      this.payPeriod = this.availablePeriods.find(p => p.offset === 0)?.period ?? null;
      if (this.payPeriod) {
        this.loadPayPeriodData();
      } else {
        this.isLoading = false;
      }
    } else {
      this.loadData(this.timeRangeMap[key][0].days);
    }
  }

  selectPeriod(offset: number): void {
    this.selectedPeriodOffset = offset;
    const found = this.availablePeriods.find(p => p.offset === offset);
    if (found) {
      this.payPeriod = found.period;
      this.loadPayPeriodData();
    }
  }

  private buildAvailablePeriods(): void {
    const labels = ['Current', 'Previous', '2 Periods Ago'];
    this.availablePeriods = [];
    for (let i = 0; i >= -2; i--) {
      const period = this.payPeriodService.getPayPeriodByOffset(i);
      if (period) {
        this.availablePeriods.push({ label: labels[-i], offset: i, period });
      }
    }
  }

  selectJob(jobId: number | null): void {
    this.selectedJobId = jobId;
    if (this.groupBy === 'payperiod') {
      this.loadPayPeriodData();
    } else {
      this.loadData(this.selectedDays);
    }
  }

  loadData(days: number): void {
    this.selectedDays = days;
    this.isLoading = true;
    const jobId = this.selectedJobId ?? undefined;

    forkJoin({
      earnings: this.tipService.getDailyEarnings(days, this.groupBy, jobId),
      summary:  this.tipService.getDashboardSummary(days, jobId),
    }).subscribe({
      next: ({ earnings, summary }) => this.applyData(earnings, summary),
      error: (err) => {
        console.error('Failed to load dashboard data', err);
        this.isLoading = false;
      },
    });
  }

  loadPayPeriodData(): void {
    if (!this.payPeriod) return;
    this.isLoading = true;
    const { startDate, endDate } = this.payPeriod;
    const jobId = this.selectedJobId ?? undefined;

    forkJoin({
      earnings: this.tipService.getDailyEarningsByDateRange(startDate, endDate, 'day', jobId),
      summary:  this.tipService.getDashboardSummaryByDateRange(startDate, endDate, jobId),
    }).subscribe({
      next: ({ earnings, summary }) => this.applyData(earnings, summary),
      error: (err) => {
        console.error('Failed to load pay period data', err);
        this.isLoading = false;
      },
    });
  }

  private applyData(earnings: any[], summary: any): void {
    this.rawData = earnings;
    this.summary = summary;

    this.chartData = {
      ...this.chartData,
      labels: earnings.map((d: any) => this.formatDate(d.date)),
      datasets: [
        { ...this.chartData.datasets[0], data: earnings.map((d: any) => d.totalTips)   },
        { ...this.chartData.datasets[1], data: earnings.map((d: any) => d.cashTips)    },
        { ...this.chartData.datasets[2], data: earnings.map((d: any) => d.creditTips)  },
        { ...this.chartData.datasets[3], data: earnings.map((d: any) => d.netEarnings) },
        // Phase 2 (P2-006): gross tips before tip-out deductions
        { ...this.chartData.datasets[4], data: earnings.map((d: any) => d.grossTips ?? d.totalTips) },
      ],
    };

    setTimeout(() => {
      this.metrics.forEach((m, i) => {
        const meta = this.chart?.chart?.getDatasetMeta(i);
        if (meta) meta.hidden = m.key !== this.activeMetric;
      });
      this.chart?.chart?.update();
    });

    this.updateStats();
    this.updateBreakdown();
    this.isLoading = false;
  }

  setMetric(key: string): void {
    this.activeMetric = key;
    this.metrics.forEach((m, i) => {
      const meta = this.chart?.chart?.getDatasetMeta(i);
      if (meta) meta.hidden = m.key !== key;
    });
    this.chart?.chart?.update();
    this.updateStats();
  }

  private updateBreakdown(): void {
    this.totalCash   = this.rawData.reduce((s, d) => s + (d.cashTips   ?? 0), 0);
    this.totalCredit = this.rawData.reduce((s, d) => s + (d.creditTips ?? 0), 0);
    this.doughnutData = {
      ...this.doughnutData,
      datasets: [{ ...this.doughnutData.datasets[0], data: [this.totalCash, this.totalCredit] }],
    };
  }

  private updateStats(): void {
    const m = this.activeMetricDef;
    const values: number[] = this.rawData.map(d => d[m.dataKey] ?? 0);
    this.periodTotal = values.reduce((sum, v) => sum + v, 0);
    this.activeDays  = values.filter(v => v > 0).length;
  }

  private formatDate(dateStr: string): string {
    const d = new Date(dateStr + 'T00:00:00');
    if (this.groupBy === 'month') {
      return d.toLocaleDateString('en-US', { month: 'short', year: '2-digit' });
    }
    return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
  }

  private formatDisplayDate(dateStr: string): string {
    const d = new Date(dateStr + 'T00:00:00');
    return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: '2-digit' });
  }
}
