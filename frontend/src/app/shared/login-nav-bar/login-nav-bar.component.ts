import { Component, OnInit } from '@angular/core';

/**
 * A simplified navigation bar displayed on the Login and Register pages
 * for unauthenticated users.
 */
@Component({
  selector: 'app-login-nav-bar',
  templateUrl: './login-nav-bar.component.html',
  styleUrls: ['./login-nav-bar.component.scss']
})
export class LoginNavBarComponent implements OnInit {

  constructor() { }

  ngOnInit(): void {
  }

}
