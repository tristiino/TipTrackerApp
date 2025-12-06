import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree, Router } from '@angular/router';
import { Observable } from 'rxjs';
import { AuthService } from '../services/auth.service';

/**
 * A route guard that prevents unauthenticated users from accessing protected routes.
 */
@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {

  /**
   * @param authService The service used to check the user's authentication status.
   * @param router The Angular router used to redirect unauthenticated users.
   */
  constructor(private authService: AuthService, private router: Router) {}

  /**
   * Determines if a route can be activated.
   * @param route The route that is being activated.
   * @param state The state of the router.
   * @returns True if the user is authenticated, or a UrlTree to redirect to the login page.
   */
  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<boolean | UrlTree> | Promise<boolean | UrlTree> | boolean | UrlTree {
    // Check for a valid token using the AuthService.
    if (this.authService.isAuthenticated()) {
      // If authenticated, allow access.
      return true;
    } else {
      // If not authenticated, redirect the user to the login page.
      return this.router.createUrlTree(['/login']);
    }
  }
}
