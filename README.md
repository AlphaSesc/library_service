# Library Service

The **Library Service** manages all library-related operations for the University: book catalog, student borrowing/returns, loan tracking, and admin oversight.

---

## Overview

This service is responsible for:

- **Library user management** — student accounts created via inter-service call from Student Portal during enrollment
- **PIN-based authentication** — separate from the main portal login (students use a 6-digit PIN)
- **Book catalog** — browse, add, and manage books (admin-only for management)
- **Borrowing operations** — students borrow books with a 14-day default loan period
- **Return operations** — when a book is returned late, a fine invoice is automatically generated via the **Finance Service**
- **Loan history tracking** — students can view their current and past borrowings
- **Admin monitoring** — overview of all current loans, overdue loans, and per-student loan summaries

---

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 4.0.5 |
| Security | Spring Security + JWT (jjwt 0.13.0) |
| Database | MySQL 8.x |
| ORM | Spring Data JPA / Hibernate |
| Templating | Thymeleaf (for view layer) |
| Build Tool | Maven |
| External Communication | RestTemplate (Finance client) |

---

## Architecture

```
┌──────────────────────────────────────────────────┐
│               Library Service                    │
│             (Port 8082 — main API)               │
└──────────────────────────────────────────────────┘
                        │
                        ▼
              ┌──────────────────┐
              │  Finance Service │
              │    (Port 8080)   │
              └──────────────────┘
                  (late return fines)
```

When a book is returned late, the Library Service calls the Finance Service to generate a `LIBRARY_FINE` invoice for the student.

---

## API Endpoints

### Public Endpoints (no auth required)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/library/auth/login` | Authenticate with `studentId` + PIN |
| `POST` | `/api/library/register` | Register a student in the library system (called by Student Portal) |

### Authenticated Endpoints (require JWT)

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| `POST` | `/api/library/auth/change-pin` | Any | Change library PIN |
| `GET` | `/api/library/books` | Any | List all active books |
| `POST` | `/api/library/borrow` | STUDENT | Borrow a book by ISBN |
| `POST` | `/api/library/returns` | STUDENT | Return a borrowed book |
| `GET` | `/api/library/account/my-borrowings` | STUDENT | View current borrowings |
| `GET` | `/api/library/account/me` | STUDENT | View full borrowing history |
| `GET` | `/api/admin/library/books` | ADMIN | List all books (admin view) |
| `POST` | `/api/admin/library/books` | ADMIN | Add a new book |
| `GET` | `/api/admin/library/loans/current` | ADMIN | List all currently active loans |
| `GET` | `/api/admin/library/loans/overdue` | ADMIN | List all overdue loans |
| `GET` | `/api/admin/library/students/loan-summary` | ADMIN | Per-student loan counts |

---

## Configuration

Configuration is split between two files in `src/main/resources/`:

### `application.yaml`
```yaml
server:
  port: 8082

spring:
  application:
    name: library_service
  profiles:
    active: dev

finance:
  service:
    base-url: http://localhost:8080
```

### `application-dev.yaml`
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/library_service_db
    username: <your-username>
    password: <your-password>
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update

jwt:
  secret: <your-jwt-secret-key>
  expiration: 86400000  # 24 hours
```

> **Note:** Update `username`, `password`, and `jwt.secret` to match your local environment.

---

## Prerequisites

1. **Java 21** ([Adoptium](https://adoptium.net/))
2. **Maven** (or use included `mvnw` wrapper)
3. **MySQL Server** running on `localhost:3306`
4. **Database created**:
   ```sql
   CREATE DATABASE library_service_db;
   ```
5. **Finance Service** running on port 8080 (required for late-return fine invoicing)

---

## Running the Service

### Using Maven Wrapper
```bash
./mvnw spring-boot:run
```

### Using Maven
```bash
mvn spring-boot:run
```

### From IntelliJ IDEA
- Open the project as a Maven project
- Run `LibraryServiceApplication.java` directly (right-click → **Run**)

The service will start on **http://localhost:8082**.

---

## Project Structure

```
src/main/java/com/example/library_service/
├── client/              # REST client for Finance service
├── config/              # Spring Security configuration
├── controller/
│   ├── student/         # Student-facing endpoints
│   └── admin/           # Admin-only endpoints
├── dto/                 # Request/response DTOs
├── entity/              # JPA entities (LibraryUser, Book, Loan)
├── exception/           # Custom exceptions + global handler
├── repository/          # JPA repositories
├── security/            # JWT filter, CustomLibraryUserDetails
├── service/             # Business logic
└── util/                # Helpers (LoanStatusResolver, etc.)
```

---

## Authentication Flow

The library service uses a separate PIN-based authentication, distinct from the main Student Portal login:

1. When a student enrolls in their first course (via Student Portal), the portal calls `POST /api/library/register` with the student's ID
2. A library account is created with default PIN `000000` and `firstLogin=true`
3. The student logs in via `POST /api/library/auth/login` with their `studentId` + PIN
4. They receive a JWT token used for subsequent requests
5. On first login, they are encouraged to change their PIN via `POST /api/library/auth/change-pin`

---

## Borrowing & Returns

### Borrowing Rules
- Loan period: **14 days** from borrow date
- A student cannot borrow the same book twice without returning it first
- Books with no available copies cannot be borrowed
- Inactive (soft-deleted) books cannot be borrowed
- Inactive students cannot borrow

### Return Rules
- On-time returns: book inventory updated, no fine
- **Late returns**: book inventory updated AND a fine of ** 500** is automatically created in the Finance Service as a `LIBRARY_FINE` invoice

---

## Loan Status

The `LoanStatusResolver` utility dynamically computes loan status:

| Status | Condition |
|--------|-----------|
| `BORROWED` | Active loan, not yet due |
| `OVERDUE` | Active loan, past due date |
| `RETURNED` | Loan has a `returnedAt` timestamp |

---

## Default Roles

- `STUDENT` — can borrow/return books, view their own loans
- `ADMIN` — can add books, view system-wide loan reports

---

## Cross-Service Communication

The Library Service depends on the **Finance Service** for late-return fine invoicing. If the Finance Service is unavailable when a late return occurs, the return itself still completes (book inventory is updated), but the fine invoice creation fails with an `ExternalServiceException`.

---

