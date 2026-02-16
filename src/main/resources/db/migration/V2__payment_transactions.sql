CREATE TABLE IF NOT EXISTS payment_transactions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    plan_type VARCHAR(50) NOT NULL,
    billing_cycle VARCHAR(20) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    amount_minor BIGINT NOT NULL,
    amount_usd_minor BIGINT NOT NULL,
    reference VARCHAR(255) NOT NULL UNIQUE,
    access_code VARCHAR(255),
    authorization_url VARCHAR(1024),
    status VARCHAR(50) NOT NULL,
    gateway_status VARCHAR(100),
    gateway_response TEXT,
    channel VARCHAR(100),
    paid_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payment_transactions_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX IF NOT EXISTS idx_payment_transactions_user_id ON payment_transactions(user_id);
CREATE INDEX IF NOT EXISTS idx_payment_transactions_status ON payment_transactions(status);
