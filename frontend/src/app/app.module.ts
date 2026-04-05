import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { HTTP_INTERCEPTORS, HttpClientModule } from '@angular/common/http';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { AppRoutingModule } from './app-routing.module';


import { NgChartsModule } from 'ng2-charts';
import { AppComponent } from './app.component';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { HomeComponent } from './components/home/home.component';
import { LoginComponent } from './components/auth/login/login.component';
import { RegisterComponent } from './components/auth/register/register.component';
import { TipEntryFormComponent } from './components/tip-entry-form/tip-entry-form.component';
import { ReportsComponent } from './components/reports/reports.component';
import { SettingsComponent } from './components/settings/settings.component';



import { NavBarComponent } from './shared/nav-bar/nav-bar.component';
import { QuickAddModalComponent } from './components/quick-add-modal/quick-add-modal.component';
import { FooterComponent } from './shared/footer/footer.component';
import { LoginNavBarComponent } from './shared/login-nav-bar/login-nav-bar.component';
import { TipOutRoleManagerComponent } from './components/tip-out-role-manager/tip-out-role-manager.component';
import { TipOutBreakdownComponent } from './components/tip-out-breakdown/tip-out-breakdown.component';


import { AuthInterceptor } from './auth.interceptor';
import { AuthGuard } from './guards/auth.guard';

@NgModule({
  declarations: [
    AppComponent,
    DashboardComponent,
    ReportsComponent,
    SettingsComponent,
    NavBarComponent,
    FooterComponent,
    TipEntryFormComponent,
    LoginComponent,
    RegisterComponent,
    LoginNavBarComponent,
    HomeComponent,
    QuickAddModalComponent,
    TipOutRoleManagerComponent,
    TipOutBreakdownComponent
  ],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    HttpClientModule,
    FormsModule,
    ReactiveFormsModule,
    NgChartsModule,
    AppRoutingModule
  ],
  providers: [

    AuthGuard,
    {
      provide: HTTP_INTERCEPTORS,
      useClass: AuthInterceptor,
      multi: true
    }
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
