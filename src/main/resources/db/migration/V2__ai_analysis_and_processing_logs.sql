ALTER TABLE applicants
    ADD COLUMN IF NOT EXISTS ai_score DOUBLE PRECISION;

ALTER TABLE applicants
    ADD COLUMN IF NOT EXISTS ai_analysis JSONB;

ALTER TABLE applications
    ADD COLUMN IF NOT EXISTS processing_logs JSONB NOT NULL DEFAULT '[]'::jsonb;
