CREATE TABLE payment_method_limits (
    payment_method_id VARCHAR(255) PRIMARY KEY,
    min_allowed_amount NUMERIC(19, 2),
    max_allowed_amount NUMERIC(19, 2),
    status VARCHAR(50),
    last_synced_at TIMESTAMP
);

CREATE TABLE payment_orders (
    payment_order_id UUID PRIMARY KEY,
    enrollment_id VARCHAR(255) NOT NULL,
    team_id VARCHAR(255),
    tournament_id VARCHAR(255),
    amount NUMERIC(19, 2),
    status VARCHAR(50),
    mp_payment_id VARCHAR(255),
    idempotency_key VARCHAR(255) NOT NULL,
    external_resource_url VARCHAR(1000),
    payer_email VARCHAR(255),
    payer_id_type VARCHAR(50),
    payer_id_number VARCHAR(50),
    expires_at TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    version INTEGER,
    CONSTRAINT uq_payment_orders_enrollment_id UNIQUE (enrollment_id),
    CONSTRAINT uq_payment_orders_idempotency_key UNIQUE (idempotency_key)
);

CREATE INDEX idx_payment_orders_mp_payment_id ON payment_orders (mp_payment_id);
CREATE INDEX idx_payment_orders_expiration ON payment_orders (status, expires_at);
