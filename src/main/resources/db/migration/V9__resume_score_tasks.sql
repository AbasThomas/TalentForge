CREATE TABLE IF NOT EXISTS resume_score_tasks (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    file_name VARCHAR(255),
    file_content_type VARCHAR(120),
    target_role VARCHAR(140),
    score DOUBLE PRECISION,
    reason TEXT,
    matching_keywords TEXT,
    parsed_characters INTEGER,
    source VARCHAR(120),
    used_applicant_profile BOOLEAN,
    processing_logs TEXT,
    error_message TEXT,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_resume_score_tasks_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX IF NOT EXISTS idx_resume_score_tasks_user_created_at
    ON resume_score_tasks(user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_resume_score_tasks_user_status
    ON resume_score_tasks(user_id, status);
