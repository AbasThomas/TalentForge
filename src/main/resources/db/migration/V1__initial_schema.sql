CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    company VARCHAR(255),
    phone VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS jobs (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    requirements TEXT,
    location VARCHAR(255),
    department VARCHAR(255),
    salary_range VARCHAR(255),
    job_type VARCHAR(50),
    experience_level VARCHAR(50),
    status VARCHAR(50) NOT NULL,
    recruiter_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closing_date TIMESTAMP,
    bias_check_result TEXT,
    CONSTRAINT fk_jobs_recruiter FOREIGN KEY (recruiter_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS applicants (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(100),
    location VARCHAR(255),
    linkedin_url VARCHAR(512),
    portfolio_url VARCHAR(512),
    summary TEXT,
    skills TEXT,
    years_of_experience INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS applications (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL,
    applicant_id BIGINT NOT NULL,
    status VARCHAR(50),
    resume_file_name VARCHAR(255),
    resume_file_path VARCHAR(1024),
    resume_file_type VARCHAR(100),
    resume_text TEXT,
    ai_score DOUBLE PRECISION,
    ai_score_reason TEXT,
    matching_keywords TEXT,
    cover_letter TEXT,
    applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP,
    interviewed_at TIMESTAMP,
    CONSTRAINT fk_applications_job FOREIGN KEY (job_id) REFERENCES jobs (id),
    CONSTRAINT fk_applications_applicant FOREIGN KEY (applicant_id) REFERENCES applicants (id),
    CONSTRAINT uk_application_job_applicant UNIQUE (job_id, applicant_id)
);

CREATE TABLE IF NOT EXISTS notes (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT NOT NULL,
    recruiter_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notes_application FOREIGN KEY (application_id) REFERENCES applications (id),
    CONSTRAINT fk_notes_recruiter FOREIGN KEY (recruiter_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS subscriptions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    plan_type VARCHAR(50) NOT NULL,
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    job_post_limit INTEGER,
    applicant_limit INTEGER,
    payment_reference VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_subscriptions_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS interviews (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT NOT NULL,
    scheduled_at TIMESTAMP,
    interview_type VARCHAR(50),
    meeting_link VARCHAR(1024),
    status VARCHAR(50),
    feedback TEXT,
    CONSTRAINT fk_interviews_application FOREIGN KEY (application_id) REFERENCES applications (id)
);

CREATE TABLE IF NOT EXISTS chat_messages (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    sender_type VARCHAR(50) NOT NULL,
    message TEXT NOT NULL,
    sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_jobs_recruiter_id ON jobs(recruiter_id);
CREATE INDEX IF NOT EXISTS idx_applications_job_id ON applications(job_id);
CREATE INDEX IF NOT EXISTS idx_applications_applicant_id ON applications(applicant_id);
CREATE INDEX IF NOT EXISTS idx_notes_application_id ON notes(application_id);
CREATE INDEX IF NOT EXISTS idx_chat_messages_user_id ON chat_messages(user_id);
