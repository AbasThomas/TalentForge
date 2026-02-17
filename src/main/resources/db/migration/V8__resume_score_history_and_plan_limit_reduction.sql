CREATE TABLE IF NOT EXISTS resume_score_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    score DOUBLE PRECISION,
    reason TEXT,
    matching_keywords TEXT,
    parsed_characters INTEGER,
    source VARCHAR(120),
    used_applicant_profile BOOLEAN NOT NULL DEFAULT FALSE,
    file_name VARCHAR(255),
    target_role VARCHAR(140),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_resume_score_history_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX IF NOT EXISTS idx_resume_score_history_user_created_at
    ON resume_score_history(user_id, created_at DESC);

UPDATE subscriptions
SET resume_score_limit = CASE plan_type
    WHEN 'FREE' THEN 20
    WHEN 'BASIC' THEN 60
    WHEN 'PRO' THEN 150
    WHEN 'ENTERPRISE' THEN 400
    ELSE resume_score_limit
END
WHERE plan_type IN ('FREE', 'BASIC', 'PRO', 'ENTERPRISE');
