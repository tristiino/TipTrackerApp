import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../services/auth.service';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';


@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit {

  loginForm: FormGroup;

  errorMessage: string = '';
  successMessage: string = '';

  loginSuccess: boolean | null = null;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.loginForm = this.fb.group({
      usernameOrEmail: ['', Validators.required],
      password: ['', Validators.required]
    });
  }

  /**
   * On init, check for a ?registered=true query param and subscribe
   * to form value changes so the error message clears while the user types.
   */
  ngOnInit(): void {
    // Show success banner when redirected after successful registration
    this.route.queryParams.subscribe(params => {
      if (params['registered'] === 'true') {
        this.successMessage = 'Account created successfully! Please log in.';
      }
    });

    // Clear stale error message as soon as the user starts editing
    this.loginForm.valueChanges.subscribe(() => {
      this.errorMessage = '';
    });
  }

  /**
   * Handles the form submission. If the form is valid, it calls the
   * AuthService to attempt to log the user in.
   */
  login(): void {
    if (this.loginForm.invalid) {
      return;
    }

    const { usernameOrEmail, password } = this.loginForm.value;
    this.authService.login({ usernameOrEmail, password }).subscribe({
      next: (response) => {
        // The backend returns a token and user object on success
        if (response.token && response.user) {
          this.loginSuccess = true;
          this.router.navigate(['/dashboard']);
        } else {
          // Handle an unexpected successful response that lacks a token
          this.loginSuccess = false;
          this.errorMessage = 'Invalid login response from server.';
        }
      },
      error: () => {
        this.loginSuccess = false;
        this.errorMessage = 'Login failed. Please check your credentials and try again.';
      }
    });
  }
}
