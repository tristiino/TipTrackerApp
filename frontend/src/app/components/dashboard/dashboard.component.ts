import { Component, OnInit, ViewChild } from '@angular/core';
import { ChartConfiguration, ChartData, ChartType } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';
import { TipService } from '../../services/tip.service';

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
  isLoading = true;
  periodTotal = 0;
  activeDays = 0;

  private rawData: any[] = [];

  timeRanges: TimeRange[] = [
    { label: '7 Days',   days: 7  },
    { label: '2 Weeks',  days: 14 },
    { label: '30 Days',  days: 30 },
  ];

  // Site design-system colors (matching _variables.scss palette)
  metrics: Metric[] = [
    { key: 'total',  label: 'Total Tips',   datasetIndex: 0, color: '#0d6efd', bgColor: 'rgba(13, 110, 253, 0.12)',  dataKey: 'totalTips'   },
    { key: 'cash',   label: 'Cash',         datasetIndex: 1, color: '#198754', bgColor: 'rgba(25, 135, 84, 0.12)',   dataKey: 'cashTips'    },
    { key: 'credit', label: 'Credit',       datasetIndex: 2, color: '#ffc107', bgColor: 'rgba(255, 193, 7, 0.12)',   dataKey: 'creditTips'  },
    { key: 'net',    label: 'Net Earnings', datasetIndex: 3, color: '#6f42c1', bgColor: 'rgba(111, 66, 193, 0.12)',  dataKey: 'netEarnings' },
  ];

  get activeMetricDef(): Metric {
    return this.metrics.find(m => m.key === this.activeMetric) ?? this.metrics[0];
  }

  chartData: ChartData<'line'> = {
    labels: [],
    datasets: [
      {
        label: 'Total Tips',
        data: [],
        borderColor: '#0d6efd',
        backgroundColor: 'rgba(13, 110, 253, 0.12)',
        fill: true,
        tension: 0.4,
        borderWidth: 2.5,
        pointRadius: 0,
        pointHoverRadius: 5,
        pointHoverBackgroundColor: '#0d6efd',
        pointHoverBorderColor: '#fff',
        pointHoverBorderWidth: 2,
      },
      {
        label: 'Cash',
        data: [],
        borderColor: '#198754',
        backgroundColor: 'rgba(25, 135, 84, 0.12)',
        fill: true,
        tension: 0.4,
        borderWidth: 2.5,
        pointRadius: 0,
        pointHoverRadius: 5,
        pointHoverBackgroundColor: '#198754',
        pointHoverBorderColor: '#fff',
        pointHoverBorderWidth: 2,
        hidden: true,
      },
      {
        label: 'Credit',
        data: [],
        borderColor: '#ffc107',
        backgroundColor: 'rgba(255, 193, 7, 0.12)',
        fill: true,
        tension: 0.4,
        borderWidth: 2.5,
        pointRadius: 0,
        pointHoverRadius: 5,
        pointHoverBackgroundColor: '#ffc107',
        pointHoverBorderColor: '#fff',
        pointHoverBorderWidth: 2,
        hidden: true,
      },
      {
        label: 'Net Earnings',
        data: [],
        borderColor: '#6f42c1',
        backgroundColor: 'rgba(111, 66, 193, 0.12)',
        fill: true,
        tension: 0.4,
        borderWidth: 2.5,
        pointRadius: 0,
        pointHoverRadius: 5,
        pointHoverBackgroundColor: '#6f42c1',
        pointHoverBorderColor: '#fff',
        pointHoverBorderWidth: 2,
        hidden: true,
      },
    ],
  };

  chartOptions: ChartConfiguration['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    interaction: {
      mode: 'index',
      intersect: false,
    },
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
        ticks: {
          maxTicksLimit: 8,
          color: '#64748b',
          font: { size: 11 },
        },
      },
      y: {
        beginAtZero: true,
        grid: { color: 'rgba(255,255,255,0.06)' },
        border: { display: false },
        ticks: {
          color: '#64748b',
          font: { size: 11 },
          callback: (value) => `$${value}`,
        },
      },
    },
  };

  constructor(private tipService: TipService) {}

  ngOnInit(): void {
    this.loadData(this.selectedDays);
  }

  loadData(days: number): void {
    this.selectedDays = days;
    this.isLoading = true;
    this.tipService.getDailyEarnings(days).subscribe({
      next: (data) => {
        this.rawData = data;
        this.chartData = {
          ...this.chartData,
          labels: data.map((d: any) => this.formatDate(d.date)),
          datasets: [
            { ...this.chartData.datasets[0], data: data.map((d: any) => d.totalTips) },
            { ...this.chartData.datasets[1], data: data.map((d: any) => d.cashTips) },
            { ...this.chartData.datasets[2], data: data.map((d: any) => d.creditTips) },
            { ...this.chartData.datasets[3], data: data.map((d: any) => d.netEarnings) },
          ],
        };
        // Re-apply hidden state after data reload
        setTimeout(() => {
          this.metrics.forEach((m, i) => {
            const meta = this.chart?.chart?.getDatasetMeta(i);
            if (meta) meta.hidden = m.key !== this.activeMetric;
          });
          this.chart?.chart?.update();
        });
        this.updateStats();
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Failed to load daily earnings', err);
        this.isLoading = false;
      },
    });
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

  private updateStats(): void {
    const m = this.activeMetricDef;
    const values: number[] = this.rawData.map(d => d[m.dataKey] ?? 0);
    this.periodTotal = values.reduce((sum, v) => sum + v, 0);
    this.activeDays = values.filter(v => v > 0).length;
  }

  private formatDate(dateStr: string): string {
    const d = new Date(dateStr + 'T00:00:00');
    return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
  }
}
