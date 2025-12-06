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
   * On component initialization, gets the current user's data from the AuthService.
   */
  ngOnInit(): void {
    this.currentUser = this.authService.getUser();
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
