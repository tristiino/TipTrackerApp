import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../services/auth.service';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';


@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent {

  loginForm: FormGroup;

  errorMessage: string = '';

  loginSuccess: boolean | null = null;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required]
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

    const { email, password } = this.loginForm.value;
    this.authService.login({ email, password }).subscribe({
      next: (response) => {
        // The backend returns a token and user object on success
        if (response.token && response.user) {
          this.loginSuccess = true;
          this.router.navigate(['/tip-entry-form']);
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
