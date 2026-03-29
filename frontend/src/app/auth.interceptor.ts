import { Injectable } from '@angular/core';
import {
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpInterceptor
} from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {

  /**
   * The core intercept method that processes each request.
   * @param request The outgoing HTTP request.
   * @param next The next interceptor in the chain.
   * @returns An Observable of the HTTP event.
   */
  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    // Retrieve the JWT from local storage.
    const token = sessionStorage.getItem('token');

    // If a token exists, clone the request and add the Authorization header.
    if (token) {
      const cloned = request.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
      // Pass the cloned request to the next handler.
      return next.handle(cloned);
    }

    // If no token exists, pass the original request along without modification.
    return next.handle(request);
  }
}
