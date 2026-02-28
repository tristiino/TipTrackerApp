# Frontend — Angular 14 SPA

## Dev Commands
```bash
npm install       # install dependencies (first time)
npm start         # ng serve — dev server at http://localhost:4200
npm run build     # ng build — production build to dist/frontend
npm test          # ng test — Karma + Jasmine test runner
```

## Architecture

### Directory Layout
```
src/app/
├── components/
│   ├── auth/
│   │   ├── login/            # LoginComponent
│   │   └── register/         # RegisterComponent
│   ├── home/                 # HomeComponent
│   ├── tip-entry/            # TipEntryComponent (display)
│   ├── tip-entry-form/       # TipEntryFormComponent (create/edit)
│   ├── tip-summary/          # TipSummaryComponent
│   ├── reports/              # ReportsComponent
│   └── settings/             # SettingsComponent
├── shared/
│   ├── nav-bar/              # Authenticated navigation bar
│   ├── login-nav-bar/        # Unauthenticated navigation bar
│   └── footer/
├── services/
│   ├── auth.service.ts       # JWT auth, localStorage, isLoggedIn$ BehaviorSubject
│   ├── tip.service.ts        # Tip CRUD API calls
│   ├── report.service.ts     # Report summary API calls
│   ├── user.service.ts       # User profile
│   ├── settings.service.ts   # User preferences (currency, tax rate)
│   ├── theme.service.ts      # Light/dark mode — persisted in localStorage
│   └── language.service.ts   # i18n English/Spanish — BehaviorSubject
├── guards/
│   └── auth.guard.ts         # Redirects unauthenticated users to /login
├── auth.interceptor.ts        # Adds Authorization: Bearer <token> to all HTTP requests
├── app.module.ts
└── app-routing.module.ts
```

## Routing
| Path | Component | Guard |
|---|---|---|
| `/login` | LoginComponent | None |
| `/register` | RegisterComponent | None |
| `/tip-entry-form` | TipEntryFormComponent | AuthGuard |
| `/settings` | SettingsComponent | AuthGuard |
| `/reports` | ReportsComponent | AuthGuard |
| `` (root) | — redirects to `/login` | |

## API Integration
- **Dev** base URL: `http://localhost:8080/api` (defined in `src/environments/environment.ts`)
- **Prod** base URL: `/api` (defined in `src/environments/environment.prod.ts`)
- Always use `environment.apiUrl` — never hardcode URLs in services or components
- JWT Bearer token is attached to every request automatically by `auth.interceptor.ts`
- Token and user object are stored in `localStorage` under keys `token` and `user`

## Key Patterns
- **Reactive Forms** (`FormBuilder`, `FormGroup`, `Validators`) — used in login, register, and tip entry forms
- **BehaviorSubject** — `AuthService.isLoggedIn$` and `LanguageService` emit state reactively to subscribers
- **HttpClient + RxJS Observables** — all service methods return Observables; subscribe in components
- **AuthGuard** calls `AuthService.isAuthenticated()` (checks for a token in localStorage)
- `AppComponent` toggles between `NavBarComponent` and `LoginNavBarComponent` based on auth state
- Theme (`light-theme` / `dark-theme` CSS class) is applied to `<body>` by `ThemeService`
- `file-saver` library is available for exporting reports to file

## Conventions
- Each feature component lives in its own folder under `components/` with `.ts`, `.html`, `.scss`, `.spec.ts`
- Shared UI elements (nav, footer) live in `shared/`
- Components never call `HttpClient` directly — all HTTP calls go through a service
- Styling uses SCSS with CSS custom properties defined in `styles.scss` for theme support
