import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TipService, TipShareRequest, TipShareResponse } from './tip.service';

describe('TipService', () => {
  let service: TipService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [TipService],
    });
    service = TestBed.inject(TipService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should calculate tip share correctly', () => {
    const mockRequest: TipShareRequest = {
      tipAmount: 100,
      numberOfPeople: 4,
      taxRate: 0.1,
    };

    const mockResponse: TipShareResponse = {
      tipAmount: 100,
      numberOfPeople: 4,
      taxRate: 0.1,
      tipAfterTax: 90,
      amountPerPerson: 22.5,
    };

    service.calculateTipShare(mockRequest).subscribe((res) => {
      expect(res.tipAfterTax).toEqual(90);
      expect(res.amountPerPerson).toEqual(22.5);
    });

    const req = httpMock.expectOne('http://localhost:8080/api/tipshare/calculate');
    expect(req.request.method).toBe('POST');
    req.flush(mockResponse);
  });

  it('should save a tip entry', () => {
    const newTip = {
      amount: 100,
      date: '2025-06-30',
      shiftType: 'Evening',
      notes: 'Busy night',
      user: { id: 1 }
    };

    const savedTip = { ...newTip, id: 123 };

    service.saveTipEntry(newTip).subscribe((res) => {
      expect(res).toEqual(savedTip);
    });

    const req = httpMock.expectOne('http://localhost:8080/api/tipentry');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(newTip);
    req.flush(savedTip);
  });

  it('should fetch all tip entries', () => {
    const mockEntries = [
      { id: 1, amount: 50, date: '2025-06-28', shiftType: 'Morning' },
      { id: 2, amount: 80, date: '2025-06-29', shiftType: 'Evening' }
    ];

    service.getTipEntries().subscribe((res) => {
      expect(res.length).toBe(2);
      expect(res).toEqual(mockEntries);
    });

    const req = httpMock.expectOne('http://localhost:8080/api/tipentry');
    expect(req.request.method).toBe('GET');
    req.flush(mockEntries);
  });

  it('should update a tip entry', () => {
    const updatedTip = {
      id: 1,
      amount: 120,
      date: '2025-06-30',
      shiftType: 'Evening',
      notes: 'Updated note'
    };

    service.updateTipEntry(updatedTip.id, updatedTip).subscribe((res) => {
      expect(res).toEqual(updatedTip);
    });

    const req = httpMock.expectOne(`http://localhost:8080/api/tipentry/${updatedTip.id}`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(updatedTip);
    req.flush(updatedTip);
  });

  it('should delete a tip entry', () => {
    const tipId = 42;

    service.deleteTipEntry(tipId).subscribe((res) => {
      expect(res).toBeNull(); // Updated to match actual response
    });

    const req = httpMock.expectOne(`http://localhost:8080/api/tipentry/${tipId}`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null); // This is what your backend would return
  });

});
