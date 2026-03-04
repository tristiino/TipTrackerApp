# TipTrackerApp

## Project Structure
```
TipTracker/
├── backend/          # Java Spring Boot 3.5.3 API
├── frontend/         # Angular 14 SPA
└── CLAUDE.md         # You are here
```

## Test Account (Production)
- **URL:** https://tiptrackerapp.org
- **Username:** testuser_claude
- **Email:** testuser@tiptrackerapp.org
- **Password:** TestPass123!

## Test Account (Local Dev)
- **URL:** http://localhost:4200
- **Backend:** http://localhost:8080
- **Database:** H2 in-memory (resets on restart)
- **Profile:** `local` (`application-local.properties`)
- **Note:** Local DB uses `create-drop` — register via `POST /api/auth/register` each time the backend restarts:
  ```json
  { "username": "testuser_claude", "email": "testuser@tiptrackerapp.org", "password": "TestPass123!" }
  ```
