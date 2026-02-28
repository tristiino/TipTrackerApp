# Backend — Spring Boot API

## Build & Run
```bash
./mvnw spring-boot:run        # start dev server (port 8080)
./mvnw test                   # run tests (uses H2 in-memory DB, no MySQL needed)
./mvnw clean install          # full build with dependency download
```

## Required Environment Variables
| Variable | Description |
|---|---|
| `APP_JWT_SECRET` | Base64-encoded 512-bit HMAC-SHA256 key. Generate with `JwtKeyGenerator.java`. |
| `DB_PASSWORD` | MySQL password for `tiptracker_user` |
| `APP_JWT_EXPIRATION` | JWT TTL in ms (optional, default: `86400000` = 24h) |

## Architecture

### Package Layout
```
com.tiptracker.backend
├── controller/        # HTTP layer — AuthController, TipEntryController
├── service/           # Business logic — AuthenticationService, TipEntryService
├── repository/        # Spring Data JPA — UserRepository, TipEntryRepository
├── model/             # JPA entities — User, TipEntry, Role (enum)
├── dto/               # Request/response objects (never expose entities directly)
├── config/            # SecurityConfig (CORS, filter chain, BCrypt bean)
├── security/          # JwtUtil, JwtAuthenticationFilter
├── BackendApplication.java
└── JwtKeyGenerator.java   # Utility: generate JWT secret or BCrypt hash for testing
```

## API Endpoints

### Auth (public — no token required)
| Method | Path | Body | Returns |
|---|---|---|---|
| POST | `/api/auth/login` | `{email, password}` | `{token, user}` |
| POST | `/api/auth/register` | `{username, email, password}` | `{token, user}` |

### Tips (authenticated — `Authorization: Bearer <token>` required)
| Method | Path | Description |
|---|---|---|
| POST | `/api/tips` | Create a tip entry |
| GET | `/api/tips/recent` | Latest 7 tips for the current user |
| GET | `/api/tips/user/{userId}/report` | Financial report for date range (`?start=YYYY-MM-DD&end=YYYY-MM-DD`) |
| PUT | `/api/tips/{id}` | Update a tip entry |
| DELETE | `/api/tips/{id}` | Delete a tip entry |

## Key Business Logic
Located in `service/TipEntryService.java`:
- **Tip Share** (10%): deducted from total (shared with support staff)
- **Gross Earnings**: `totalAmount - tipShare`
- **Tax** (3%): applied to gross earnings
- **Net Earnings**: `grossEarnings - tax`

## Security
- JWT signed with HS256 (HMAC-SHA256), validated per-request by `JwtAuthenticationFilter`
- `/api/auth/**` is public; all other endpoints require a valid JWT
- CORS allowed origins: `localhost:4200`, `tiptrackerapp.org`, `www.tiptrackerapp.org`, `3.19.152.116`
- Passwords hashed with BCrypt (`SecurityConfig` exposes `PasswordEncoder` bean)
- Sessions are stateless (no server-side session state)

## Database
- **Production**: MySQL 8.0, database `tiptracker_db`, `ddl-auto=update`
- **Tests**: H2 in-memory — config in `src/test/resources/application.properties`
- **Tables**:
  - `users` — id, username, email (unique), password (bcrypt), role
  - `tip_entry` — id, amount, date, shift_type, notes, user_id (FK → users)

## Conventions
- DTOs live in `dto/` — never return JPA entities directly from controllers
- `User` implements Spring Security's `UserDetails`; **email is the username** (not the `username` field)
- `@JsonBackReference` on `TipEntry.user` prevents circular serialization
- Lombok is configured — use `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor` freely
- Environment variables are loaded via `dotenv-java`; sensitive values must never be hardcoded
