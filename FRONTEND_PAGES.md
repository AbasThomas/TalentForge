# TalentForge Frontend Page Blueprint

This document lists all frontend pages the TalentForge web app should have.

## 1. Public Pages (No Login Required)

1. `/` - Landing Page
- Product pitch, key features, pricing highlights, CTA buttons (`Get Started`, `Login`)

2. `/features`
- Full feature breakdown (AI scoring, bias detection, social export, dashboards)

3. `/pricing`
- Plan cards (Free, Starter, Pro, Enterprise), monthly/annual toggle

4. `/about`
- Company story, mission, privacy-first messaging

5. `/contact`
- Contact form + support email/social links

6. `/login`
- Email/password login

7. `/register`
- Recruiter/Candidate/Admin registration flow (or role selection)

8. `/forgot-password`
- Request password reset link

9. `/reset-password`
- Set new password with token

## 2. Shared Authenticated Pages

1. `/dashboard`
- Role-aware redirect shell:
- Candidate -> candidate dashboard
- Recruiter -> recruiter dashboard
- Admin -> admin dashboard

2. `/profile`
- Profile details, password change, phone/company update

3. `/notifications`
- Alerts (application updates, interview schedules, system notices)

4. `/settings`
- Preferences, timezone, email notifications, AI settings (if role allows)

## 3. Recruiter Pages

1. `/recruiter/dashboard`
- KPI cards, open jobs, application funnel, quick actions

2. `/recruiter/jobs`
- List/search/filter recruiter jobs

3. `/recruiter/jobs/new`
- Create job page (calls bias-check before/while saving)

4. `/recruiter/jobs/:jobId/edit`
- Edit existing job

5. `/recruiter/jobs/:jobId`
- Job details, linked applications, status controls

6. `/recruiter/applications`
- All applications table with filters (status, AI score, date, job)

7. `/recruiter/applications/:applicationId`
- Application detail: resume, AI score, notes, status pipeline

8. `/recruiter/interviews`
- Upcoming/completed interviews list

9. `/recruiter/interviews/new`
- Schedule interview

10. `/recruiter/notes`
- Optional consolidated notes page

11. `/recruiter/subscription`
- Current plan, limits, billing status, upgrade CTA

12. `/recruiter/chat-assistant`
- In-app AI assistant/chat history

13. `/recruiter/analytics`
- Hiring metrics, score distribution, time-to-hire charts

## 4. Candidate Pages

1. `/candidate/dashboard`
- Active applications, statuses, interview reminders

2. `/candidate/jobs`
- Browse/search available jobs

3. `/candidate/jobs/:jobId`
- Job details and apply flow

4. `/candidate/applications`
- Candidate’s submitted applications

5. `/candidate/applications/:applicationId`
- Application status timeline and feedback (when available)

6. `/candidate/resume`
- Resume upload/manage (if supported as profile asset)

7. `/candidate/chat-assistant`
- Candidate support chatbot

## 5. Admin Pages

1. `/admin/dashboard`
- Global platform KPIs (users, jobs, active plans, usage)

2. `/admin/users`
- Manage all users (search, activate/deactivate, roles)

3. `/admin/users/:userId`
- User detail and account actions

4. `/admin/jobs`
- View/moderate all jobs

5. `/admin/applications`
- Global applications oversight

6. `/admin/subscriptions`
- Plan assignments, billing references, limits

7. `/admin/analytics`
- Business metrics + system usage reports

8. `/admin/system-settings`
- Global app settings, feature toggles, AI model defaults

## 6. Utility / System Pages

1. `/403`
- Forbidden page

2. `/404`
- Not found page

3. `/500`
- Server error page

4. `/maintenance`
- Optional maintenance mode page

## 7. Suggested Navigation Structure

1. Public Navbar
- Home, Features, Pricing, About, Contact, Login, Sign Up

2. Recruiter Sidebar
- Dashboard, Jobs, Applications, Interviews, Analytics, Subscription, Chat Assistant, Settings

3. Candidate Sidebar
- Dashboard, Jobs, Applications, Resume, Chat Assistant, Settings

4. Admin Sidebar
- Dashboard, Users, Jobs, Applications, Subscriptions, Analytics, System Settings

## 8. MVP Priority (Build First)

1. Auth (`/login`, `/register`)
2. Recruiter core (`/recruiter/jobs`, `/recruiter/jobs/new`, `/recruiter/applications`, `/recruiter/applications/:id`)
3. Candidate core (`/candidate/jobs`, `/candidate/applications`)
4. Shared (`/profile`, `/settings`)
5. Admin lite (`/admin/users`, `/admin/subscriptions`)

