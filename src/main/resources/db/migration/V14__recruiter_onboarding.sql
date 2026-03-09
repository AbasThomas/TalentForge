ALTER TABLE users
    ADD COLUMN IF NOT EXISTS company_website VARCHAR(255),
    ADD COLUMN IF NOT EXISTS recruiter_job_title VARCHAR(120),
    ADD COLUMN IF NOT EXISTS recruiter_team_size VARCHAR(80),
    ADD COLUMN IF NOT EXISTS recruiter_terms_accepted_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS recruiter_data_consent_accepted_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS recruiter_authority_confirmed_at TIMESTAMP;
