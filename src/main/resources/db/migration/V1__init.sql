CREATE TABLE payment_orders (
    payment_order_id       UUID PRIMARY KEY,
    enrollment_id          VARCHAR(64) NOT NULL UNIQUE,
    team_id                VARCHAR(64) NOT NULL,
    tournament_id           VARCHAR(64) NOT NULL,
    amount                  NUMERIC(12,2) NOT NULL,
    status                  VARCHAR(30) NOT NULL,
    mp_payment_id           VARCHAR(64),
    idempotency_key         VARCHAR(64) NOT NULL UNIQUE,
    external_resource_url   TEXT,
    payer_email             VARCHAR(120),
    payer_id_type           VARCHAR(10),
    payer_id_number         VARCHAR(30),
    expires_at              TIMESTAMP NOT NULL,
    created_at              TIMESTAMP NOT NULL DEFAULT now(),
    updated_at              TIMESTAMP NOT NULL DEFAULT now(),
    version                 INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_payment_orders_expiration
    ON payment_orders (status, expires_at);

CREATE INDEX idx_payment_orders_mp_payment_id
    ON payment_orders (mp_payment_id);

CREATE TABLE payment_method_limits (
    payment_method_id      VARCHAR(30) PRIMARY KEY,
    min_allowed_amount     NUMERIC(12,2) NOT NULL,
    max_allowed_amount     NUMERIC(12,2) NOT NULL,
    status                  VARCHAR(20) NOT NULL,
    last_synced_at          TIMESTAMP NOT NULL
);
