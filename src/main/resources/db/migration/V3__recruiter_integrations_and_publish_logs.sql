CREATE TABLE IF NOT EXISTS recruiter_integrations (
    id BIGSERIAL PRIMARY KEY,
    recruiter_id BIGINT NOT NULL,
    platform VARCHAR(50) NOT NULL,
    account_handle VARCHAR(255),
    profile_url VARCHAR(1024),
    default_message TEXT,
    connected BOOLEAN NOT NULL DEFAULT TRUE,
    connected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_recruiter_integrations_recruiter FOREIGN KEY (recruiter_id) REFERENCES users (id),
    CONSTRAINT uk_recruiter_integrations_recruiter_platform UNIQUE (recruiter_id, platform)
);

CREATE TABLE IF NOT EXISTS integration_publish_logs (
    id BIGSERIAL PRIMARY KEY,
    recruiter_id BIGINT NOT NULL,
    job_id BIGINT NOT NULL,
    platform VARCHAR(50) NOT NULL,
    target_url VARCHAR(2048),
    share_text TEXT,
    status VARCHAR(50) NOT NULL,
    message VARCHAR(512),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_integration_publish_logs_recruiter FOREIGN KEY (recruiter_id) REFERENCES users (id),
    CONSTRAINT fk_integration_publish_logs_job FOREIGN KEY (job_id) REFERENCES jobs (id)
);

CREATE INDEX IF NOT EXISTS idx_recruiter_integrations_recruiter_id ON recruiter_integrations(recruiter_id);
CREATE INDEX IF NOT EXISTS idx_integration_publish_logs_recruiter_id ON integration_publish_logs(recruiter_id);
CREATE INDEX IF NOT EXISTS idx_integration_publish_logs_job_id ON integration_publish_logs(job_id);
