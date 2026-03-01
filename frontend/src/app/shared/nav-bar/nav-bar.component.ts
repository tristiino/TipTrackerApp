import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';


@Component({
  selector: 'app-nav-bar',
  templateUrl: './nav-bar.component.html',
  styleUrls: ['./nav-bar.component.scss']
})
export class NavBarComponent implements OnInit {
  /** An observable that holds the current login status of the user. */
  public isLoggedIn$ = this.authService.isLoggedIn$;
  /** Holds the currently logged-in user's data to display their name. */
  public currentUser: any = null;

  constructor(private router: Router, private authService: AuthService) {}

  /**
   * On component initialization, subscribe reactively to isLoggedIn$ so that
   * currentUser is refreshed whenever auth state changes (BUG-05).
   * A one-time getUser() call would miss the user object when the navbar
   * initialises before login completes.
   */
  ngOnInit(): void {
    this.authService.isLoggedIn$.subscribe(isLoggedIn => {
      this.currentUser = isLoggedIn ? this.authService.getUser() : null;
    });
  }

  /**
   * Logs the user out by calling the AuthService and navigates to the login page.
   */
  logout(): void {
    this.authService.logout();
    this.currentUser = null;
    this.router.navigate(['/login']).catch(err => console.error('Navigation error:', err));
  }
}
