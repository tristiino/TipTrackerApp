# TipTracker

TipTracker is a robust full-stack application designed to empower service industry professionals by providing a seamless way to track shifts and tips. This project demonstrates a modern, scalable architecture explicitly built to handle real-world data tracking needs with precision and security.

This application serves as a comprehensive example of a production-ready full-stack solution, integrating a secure Java Spring Boot backend with a dynamic Angular frontend.

## 🚀 Key Features

*   **Secure Authentication**: Robust user registration and login implemented with JWT (JSON Web Tokens) and Spring Security.
*   **Shift & Tip Management**: specific tracking for dates, hours worked, and tips earned.
*   **Data Persistence**: Reliable data storage using MySQL with Hibernate ORM.
*   **Responsive UI**: A clean, user-friendly interface built with Angular 14.

---

## 🛠️ Technology Stack

This project leverages industry-standard technologies to ensure performance, maintainability, and scalability.

### Backend (API Layer)
*   **Framework**: Spring Boot 3.5.3
*   **Language**: Java 17+
*   **Security**: Spring Security 6 (JWT Authentication)
*   **Database**: MySQL 8.0
*   **Build Tool**: Maven (Wrapper included)

### Frontend (Client Layer)
*   **Framework**: Angular 14
*   **Language**: TypeScript
*   **Styling**: SCSS, CSS3
*   **Platform**: Node.js

---

## ⚙️ Getting Started

Follow these instructions to get a copy of the project up and running on your local machine for development and testing purposes.

### Prerequisites

Ensure you have the following installed on your system:
*   **Java JDK 17** or higher
*   **Node.js** (v14 or v18 recommended) & **npm**
*   **MySQL Server** (running locally)

### 1. Database Setup

The application requires a MySQL database. You can set this up quickly via the command line.

1.  Open your terminal and log in to MySQL:
    ```bash
    mysql -u root -p
    ```
2.  Create the database:
    ```sql
    CREATE DATABASE IF NOT EXISTS tiptracker_db;
    ```
3.  Exit MySQL:
    ```sql
    EXIT;
    ```

*Note: The application is configured to use `root` as the username. Check `backend/src/main/resources/application.properties` if you need to adjust credentials.*

### 2. Backend Setup

The backend runs on port `8080`.

1.  Navigate to the backend directory:
    ```bash
    cd backend
    ```
2.  Install dependencies and build the project (using the included Maven wrapper):
    ```bash
    ./mvnw clean install
    ```
3.  Start the server:
    ```bash
    ./mvnw spring-boot:run
    ```
    *You should see logs indicating the application has started on port 8080.*

### 3. Frontend Setup

The frontend runs on port `4200`.

1.  Open a **new** terminal window (keep the backend running).
2.  Navigate to the frontend directory:
    ```bash
    cd frontend
    ```
3.  Install dependencies:
    ```bash
    npm install
    ```
4.  Start the development server:
    ```bash
    ng serve
    ```
5.  Open your browser and navigate to `http://localhost:4200`.

---

## 🔍 Application Demo & Verification

### Login / Register
To demonstrate the application flows, you can register a new user or use the following test account (if you've created it):

*   **Email**: `test@test.com`
*   **Password**: `password`

### Verifying Data Persistence

To show that data is accurately being stored in the database, you can query the tables directly.

1.  Log in to MySQL:
    ```bash
    mysql -u root -p
    ```
2.  Select the project database:
    ```sql
    USE tiptracker_db;
    ```
3.  **View Registered Users**:
    ```sql
    SELECT * FROM users;
    ```
    *This will display ID, email, username, and role.*

4.  **View Tip Entries**:
    ```sql
    SELECT * FROM tip_entry;
    ```
    *This will show all tracked shift data.*

---

## 📝 License
This project is intended for educational and portfolio purposes.
