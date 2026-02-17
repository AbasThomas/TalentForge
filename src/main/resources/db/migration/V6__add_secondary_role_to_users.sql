ALTER TABLE users
    ADD COLUMN IF NOT EXISTS secondary_role VARCHAR(50);

UPDATE users
SET secondary_role = NULL
WHERE secondary_role = role;
