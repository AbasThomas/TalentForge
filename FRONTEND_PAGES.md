# TalentForge Frontend Page Blueprint (Backend Aligned)

This page list was aligned to backend controllers under `src/main/java/com/TalentForge/talentforge/**/controller`.
Each page includes core features that are directly supported by current APIs.

## 1. Public / Auth Pages

1. `/`
- Product overview, role-based CTAs, and links to auth pages.

2. `/login`
- Email/password login form.
- Stores JWT/session and redirects by user role.
- API: `POST /api/v1/auth/login`.

3. `/register`
- User registration with role selection (`CANDIDATE`, `RECRUITER`, `ADMIN`) and profile basics.
- API: `POST /api/v1/auth/register`.

4. `/403`, `/404`, `/500`
- Standard auth/route/error fallback pages.

## 2. Shared Authenticated Pages

1. `/dashboard`
- Role-aware router shell:
- Candidate -> `/candidate/dashboard`
- Recruiter -> `/recruiter/dashboard`
- Admin -> `/admin/dashboard`

2. `/profile`
- Read current user profile (email, full name, role, company, phone, active flag).
- API: `GET /api/v1/users/{id}`.

3. `/chat`
- Shared chat entry page that can redirect to role-specific chat pages.
- Supports conversation and history retrieval.
- APIs: `POST /api/v1/chat`, `GET /api/v1/chat/{userId}`.

## 3. Recruiter Pages

1. `/recruiter/dashboard`
- Summary cards from recruiter jobs/applications.
- Quick links to open jobs, active applications, interviews, and subscription.
- APIs typically consumed: `GET /api/v1/jobs?recruiterId={id}`, `GET /api/v1/applications?jobId={id}`.

2. `/recruiter/jobs`
- List recruiter-owned jobs with status chips (`DRAFT`, `OPEN`, `CLOSED`, `ARCHIVED`).
- Create/edit/delete actions.
- APIs: `GET /api/v1/jobs?recruiterId={id}`, `POST /api/v1/jobs`, `PUT /api/v1/jobs/{id}`, `DELETE /api/v1/jobs/{id}`.

3. `/recruiter/jobs/new`
- Job creation form (title, description, requirements, location, department, salary range, type, experience, closing date).
- Shows backend-generated bias-check result after submit.
- API: `POST /api/v1/jobs`.

4. `/recruiter/jobs/:jobId/edit`
- Edit existing job and re-run bias check on save.
- API: `PUT /api/v1/jobs/{id}`.

5. `/recruiter/jobs/:jobId`
- Job details view with recruiter info, status, closing date, and bias check result.
- Linked applications list for that job.
- APIs: `GET /api/v1/jobs/{id}`, `GET /api/v1/applications?jobId={jobId}`.

6. `/recruiter/applications`
- Application list filtered by selected job(s).
- Shows AI score, AI reason, keywords, status, and timestamps.
- API: `GET /api/v1/applications?jobId={jobId}`.

7. `/recruiter/applications/:applicationId`
- Detailed candidate application view with resume metadata/path, cover letter, and AI scoring block.
- Status pipeline actions (`APPLIED`, `REVIEWING`, `SHORTLISTED`, `INTERVIEWED`, `OFFERED`, `REJECTED`, `HIRED`, `WITHDRAWN`).
- APIs: `GET /api/v1/applications/{id}`, `PATCH /api/v1/applications/{id}/status`.

8. `/recruiter/applications/:applicationId/notes`
- Recruiter notes timeline (newest first) plus create-note form.
- API: `GET /api/v1/notes/application/{applicationId}`, `POST /api/v1/notes`.

9. `/recruiter/applications/:applicationId/interviews`
- Interview list scoped to an application.
- Create interview with schedule/type/link/status/feedback.
- APIs: `GET /api/v1/interviews/application/{applicationId}`, `POST /api/v1/interviews`.

10. `/recruiter/subscription`
- Current plan details (`FREE`, `BASIC`, `PRO`, `ENTERPRISE`), limits, active flag, payment reference.
- Update plan and limits.
- APIs: `GET /api/v1/subscriptions/user/{userId}`, `POST /api/v1/subscriptions`.

11. `/recruiter/chat-assistant`
- AI assistant chat composer + message history.
- APIs: `POST /api/v1/chat`, `GET /api/v1/chat/{userId}`.

12. `/recruiter/candidates`
- Candidate pool list sourced from applicants.
- APIs: `GET /api/v1/applicants`, `GET /api/v1/applicants/{id}`.

## 4. Candidate Pages

1. `/candidate/dashboard`
- Snapshot of submitted applications and interview status by application.
- APIs: `GET /api/v1/applications?applicantId={id}`, `GET /api/v1/interviews/application/{applicationId}`.

2. `/candidate/jobs`
- Browse all jobs with key metadata (department, location, type, experience, status).
- API: `GET /api/v1/jobs`.

3. `/candidate/jobs/:jobId`
- Job details and apply workflow with cover letter + optional resume upload.
- Application submit includes backend resume parsing + AI score generation.
- APIs: `GET /api/v1/jobs/{id}`, `POST /api/v1/applications` (multipart form).

4. `/candidate/applications`
- Candidate application history with status and AI score preview.
- API: `GET /api/v1/applications?applicantId={applicantId}`.

5. `/candidate/applications/:applicationId`
- Single application timeline/details with status and interview/notes references.
- API: `GET /api/v1/applications/{id}`.

6. `/candidate/applications/:applicationId/interviews`
- Candidate-facing interview schedule/details per application.
- API: `GET /api/v1/interviews/application/{applicationId}`.

7. `/candidate/profile`
- Applicant profile management: full name, email, phone, location, LinkedIn, portfolio, summary, skills, years of experience.
- APIs: `POST /api/v1/applicants`, `PUT /api/v1/applicants/{id}`, `GET /api/v1/applicants/{id}`.

8. `/candidate/chat-assistant`
- Candidate AI assistant with chat history.
- APIs: `POST /api/v1/chat`, `GET /api/v1/chat/{userId}`.

## 5. Admin Pages

1. `/admin/dashboard`
- Global totals and activity overview from users/jobs/applicants.
- APIs: `GET /api/v1/users`, `GET /api/v1/jobs`, `GET /api/v1/applicants`.

2. `/admin/users`
- User table with role, company, phone, active state.
- Includes deactivate action.
- APIs: `GET /api/v1/users`, `POST /api/v1/users/{id}/deactivate`.

3. `/admin/users/new`
- Admin-only create user form.
- API: `POST /api/v1/users`.

4. `/admin/users/:userId`
- User detail page with account state and metadata.
- API: `GET /api/v1/users/{id}`.

5. `/admin/jobs`
- Cross-platform job moderation/listing with access to job detail.
- APIs: `GET /api/v1/jobs`, `GET /api/v1/jobs/{id}`.

6. `/admin/applicants`
- Cross-platform applicant directory and profile inspection.
- APIs: `GET /api/v1/applicants`, `GET /api/v1/applicants/{id}`.

7. `/admin/subscriptions`
- Subscription administration with plan/limits/payment reference management.
- APIs: `GET /api/v1/subscriptions/user/{userId}`, `POST /api/v1/subscriptions`.

## 6. Planned But Not Yet Backed By Current Backend

1. `/forgot-password`
- Password reset request flow.

2. `/reset-password`
- Token-based password reset form.

3. `/features`, `/pricing`, `/about`, `/contact`
- Marketing pages (no backend dependency required).
