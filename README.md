# TalentForge Backend API

Spring Boot backend for TalentForge (ATS), with:
- JWT authentication and role-based authorization
- PostgreSQL + Flyway migrations
- Local AI via Ollama (bias checks, resume scoring, chat assistant)

## Run Locally

1. Start infrastructure:
```bash
docker compose up -d
```

2. Ensure the Ollama model is available:
```bash
docker exec -it talentforge-ollama ollama pull llama3.2:latest
```

3. Configure environment:
- Copy `.env.example` to `.env` (already supported by `spring.config.import`)
- Update values if needed

4. Start API:
```bash
./mvnw spring-boot:run
```

If `8080` is busy, run on `8090`:
```bash
./mvnw "spring-boot:run" "-Dspring-boot.run.arguments=--server.port=8090"
```

Default seeded admin:
- Email: `admin@talentforge.local`
- Password: `Admin@123`

## Authentication

- Header for protected routes:
```http
Authorization: Bearer <JWT_TOKEN>
```

- Public routes:
  - `POST /api/v1/auth/register`
  - `POST /api/v1/auth/login`

## Endpoint Reference

Base path: `/api/v1`

### Auth

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/auth/register` | Public | Register user |
| POST | `/auth/login` | Public | Login and get JWT |

### Users

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/users` | ADMIN | Create user |
| GET | `/users` | Any authenticated | List users |
| GET | `/users/{id}` | Any authenticated | Get user by id |
| POST | `/users/{id}/deactivate` | Any authenticated | Deactivate user |

### Jobs

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/jobs` | RECRUITER, ADMIN | Create job (runs bias check) |
| PUT | `/jobs/{id}` | RECRUITER, ADMIN | Update job |
| GET | `/jobs` | Any authenticated | List jobs (`?recruiterId=` optional) |
| GET | `/jobs/{id}` | Any authenticated | Get job by id |
| DELETE | `/jobs/{id}` | RECRUITER, ADMIN | Delete job |

### Applicants

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/applicants` | Any authenticated | Create applicant |
| POST | `/applicants/resume-score` | Any authenticated | Parse uploaded resume and return Talentforge AI score |
| PUT | `/applicants/{id}` | Any authenticated | Update applicant |
| GET | `/applicants` | Any authenticated | List applicants |
| GET | `/applicants/{id}` | Any authenticated | Get applicant by id |
| DELETE | `/applicants/{id}` | Any authenticated | Delete applicant |

### Applications

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/applications` | Any authenticated | Submit application (multipart, resume parsing + AI score) |
| POST | `/applications/{id}/rescore` | RECRUITER, ADMIN | Re-run Talentforge AI scoring for an existing application |
| PATCH | `/applications/{id}/status` | Any authenticated | Update application status |
| GET | `/applications/{id}` | Any authenticated | Get application by id |
| GET | `/applications` | Any authenticated | Filter by `?jobId=` or `?applicantId=` |

`POST /applications` multipart fields:
- `jobId` (number)
- `applicantId` (number)
- `status` (optional; e.g. `APPLIED`)
- `coverLetter` (optional)
- `resumeFile` (optional file, e.g. PDF/DOCX/TXT)

### Notes

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/notes` | Any authenticated | Create recruiter note |
| GET | `/notes/application/{applicationId}` | Any authenticated | List notes for application |

### Interviews

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/interviews` | RECRUITER, ADMIN | Create interview |
| GET | `/interviews/application/{applicationId}` | Any authenticated | List interviews by application |

### Subscriptions

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/subscriptions` | RECRUITER, ADMIN | Create/update subscription |
| GET | `/subscriptions/user/{userId}` | Any authenticated | Get user subscription |

### Payments (Paystack)

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/payments/options` | Public | Supported currencies, channels, and plan prices |
| POST | `/payments/initialize` | RECRUITER, ADMIN | Initialize checkout and get Paystack authorization URL |
| GET | `/payments/verify/{reference}` | RECRUITER, ADMIN | Verify payment and activate subscription |
| POST | `/payments/webhook` | Public | Paystack webhook receiver (HMAC signature verified) |

### Chat

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/chat` | Any authenticated | Ask AI chat assistant |
| GET | `/chat/{userId}` | Any authenticated | Chat history |

## Quick cURL Examples

Register:
```bash
curl -X POST http://localhost:8090/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"Pass@123","fullName":"Test User","role":"CANDIDATE"}'
```

Login:
```bash
curl -X POST http://localhost:8090/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@talentforge.local","password":"Admin@123"}'
```

Create job:
```bash
curl -X POST http://localhost:8090/api/v1/jobs \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"title":"Backend Engineer","description":"Build APIs","status":"OPEN","recruiterId":2}'
```

## Endpoint Test Automation

Automated API sweep script:
- `scripts/test-endpoints.ps1`

Run:
```powershell
powershell -ExecutionPolicy Bypass -File scripts/test-endpoints.ps1
```

Latest run status (February 14, 2026):
- `TOTAL=34`
- `PASSED=34`
- `FAILED=0`

## Paystack Setup

Add these environment variables:
- `PAYSTACK_SECRET_KEY`
- `PAYSTACK_WEBHOOK_SECRET` (optional; falls back to secret key)
- `PAYSTACK_CALLBACK_URL` (e.g., `http://localhost:3000/recruiter/subscription`)
- `PAYSTACK_CHANNELS` (comma-separated)
- `PAYSTACK_FX_NGN`, `PAYSTACK_FX_GHS`, `PAYSTACK_FX_KES`, `PAYSTACK_FX_ZAR`

Webhook URL for Paystack dashboard:
- `POST /api/v1/payments/webhook`
