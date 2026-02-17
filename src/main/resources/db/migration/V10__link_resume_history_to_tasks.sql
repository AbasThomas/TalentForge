ALTER TABLE resume_score_history
    ADD COLUMN IF NOT EXISTS resume_score_task_id BIGINT;

ALTER TABLE resume_score_history
    ADD CONSTRAINT fk_resume_score_history_task
        FOREIGN KEY (resume_score_task_id) REFERENCES resume_score_tasks (id);

CREATE INDEX IF NOT EXISTS idx_resume_score_history_task_id
    ON resume_score_history(resume_score_task_id);
