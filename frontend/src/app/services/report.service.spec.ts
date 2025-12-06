import { TestBed } from '@angular/core/testing';
import { ReportService } from './report.service';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

describe('ReportService', () => {
  let service: ReportService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ReportService]
    });

    service = TestBed.inject(ReportService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fetch weekly summary', () => {
    const mockData = { totalTips: 200, totalHours: 30 };
    service.getWeeklySummary().subscribe(data => {
      expect(data).toEqual(mockData);
    });

    const req = httpMock.expectOne('http://localhost:8080/api/reports/weekly');
    expect(req.request.method).toBe('GET');
    req.flush(mockData);
  });

  it('should fetch monthly summary', () => {
    const mockData = { totalTips: 850, totalHours: 120 };
    service.getMonthlySummary().subscribe(data => {
      expect(data).toEqual(mockData);
    });

    const req = httpMock.expectOne('http://localhost:8080/api/reports/monthly');
    expect(req.request.method).toBe('GET');
    req.flush(mockData);
  });

  it('should fetch summary by date range', () => {
    const mockData = { totalTips: 500, totalHours: 60 };
    service.getSummaryByDateRange('2025-06-01', '2025-06-15').subscribe(data => {
      expect(data).toEqual(mockData);
    });

    const req = httpMock.expectOne('http://localhost:8080/api/reports/range?start=2025-06-01&end=2025-06-15');
    expect(req.request.method).toBe('GET');
    req.flush(mockData);
  });
});
