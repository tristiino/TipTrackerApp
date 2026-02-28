# TipTracker

Full-stack web application for service industry workers to track shifts and tips with financial reporting.

## Tech Stack
- **Backend**: Java 17, Spring Boot 3.5.3, Maven, MySQL 8.0, JWT auth
- **Frontend**: Angular 14.2, TypeScript, SCSS, Karma/Jasmine

## Project Structure
```
/
├── backend/    # Spring Boot REST API (port 8080)
└── frontend/   # Angular SPA (port 4200)
```

## Running the App (Development)

### Prerequisites
- Java 17+, Maven (or use `./mvnw` wrapper)
- Node.js + npm
- MySQL 8.0 running locally with database `tiptracker_db`

### Environment Variables (backend)
Set these before starting the backend:
```bash
export APP_JWT_SECRET=<base64-encoded-512-bit-key>   # required
export DB_PASSWORD=<mysql-password>                   # required
export APP_JWT_EXPIRATION=86400000                    # optional, default 24h in ms
```

Use `JwtKeyGenerator.java` in the backend to generate a valid `APP_JWT_SECRET`.

### Start Backend
```bash
cd backend
./mvnw spring-boot:run
# Runs on http://localhost:8080
```

### Start Frontend
```bash
cd frontend
npm install
npm start
# Runs on http://localhost:4200
```

## Running Tests
```bash
# Backend (JUnit 5 + H2 in-memory DB — no MySQL needed)
cd backend && ./mvnw test

# Frontend (Karma + Jasmine)
cd frontend && npm test
```
