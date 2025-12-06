import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { SettingsService } from './settings.service';

describe('SettingsService', () => {
  let service: SettingsService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [SettingsService]
    });

    service = TestBed.inject(SettingsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should get user settings', () => {
    const mockSettings = { userId: 1, currency: 'USD', taxRate: 0.1 };

    service.getSettings(1).subscribe(settings => {
      expect(settings).toEqual(mockSettings);
    });

    const req = httpMock.expectOne('http://localhost:8080/api/settings/1');
    expect(req.request.method).toBe('GET');
    req.flush(mockSettings);
  });

  it('should update user settings', () => {
    const mockSettings = { userId: 1, currency: 'EUR', taxRate: 0.08 };

    service.updateSettings(1, mockSettings).subscribe(res => {
      expect(res).toEqual({ success: true });
    });

    const req = httpMock.expectOne('http://localhost:8080/api/settings/1');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(mockSettings);
    req.flush({ success: true });
  });
});
