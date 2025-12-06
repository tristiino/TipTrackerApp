import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { UserService } from './user.service';

describe('UserService', () => {
  let service: UserService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [UserService]
    });

    service = TestBed.inject(UserService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should register a user', () => {
    const mockUser = { email: 'test@example.com', password: '123456' };
    service.register(mockUser).subscribe(res => {
      expect(res).toEqual({ success: true });
    });

    const req = httpMock.expectOne('http://localhost:8080/api/users/register');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(mockUser);
    req.flush({ success: true });
  });

  it('should login a user', () => {
    const credentials = { email: 'test@example.com', password: '123456' };
    service.login(credentials).subscribe(res => {
      expect(res).toEqual({ token: 'abc123' });
    });

    const req = httpMock.expectOne('http://localhost:8080/api/users/login');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(credentials);
    req.flush({ token: 'abc123' });
  });

  it('should fetch user profile', () => {
    const mockProfile = { id: 1, email: 'test@example.com', name: 'Test User' };
    service.getUserProfile().subscribe(profile => {
      expect(profile).toEqual(mockProfile);
    });

    const req = httpMock.expectOne('http://localhost:8080/api/users/me');
    expect(req.request.method).toBe('GET');
    req.flush(mockProfile);
  });
});
