-- V3: pagos, membresias y comprobantes
-- Sprint 3 - Pagos (PPP-22 / PPP-23)

CREATE TABLE membership_plans (
    id            BIGSERIAL PRIMARY KEY,
    gym_id        BIGINT       NOT NULL REFERENCES gyms (id) ON DELETE CASCADE,
    code          VARCHAR(30)  NOT NULL,
    name          VARCHAR(80)  NOT NULL,
    description   VARCHAR(240),
    price_cents   INTEGER      NOT NULL,
    currency      CHAR(3)      NOT NULL DEFAULT 'COP',
    duration_days SMALLINT     NOT NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_plans_gym_code UNIQUE (gym_id, code),
    CONSTRAINT ck_plans_price CHECK (price_cents > 0),
    CONSTRAINT ck_plans_duration CHECK (duration_days BETWEEN 1 AND 730)
);

CREATE INDEX idx_plans_gym_active ON membership_plans (gym_id) WHERE active;

CREATE TABLE payment_methods (
    code      VARCHAR(30) PRIMARY KEY,
    label     VARCHAR(60) NOT NULL,
    kind      VARCHAR(20) NOT NULL,
    immediate BOOLEAN     NOT NULL DEFAULT TRUE,
    active    BOOLEAN     NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_payment_methods_kind CHECK (kind IN ('CARD', 'WALLET', 'TRANSFER'))
);

CREATE TABLE gym_payment_methods (
    gym_id      BIGINT      NOT NULL REFERENCES gyms (id) ON DELETE CASCADE,
    method_code VARCHAR(30) NOT NULL REFERENCES payment_methods (code) ON DELETE CASCADE,
    enabled     BOOLEAN     NOT NULL DEFAULT TRUE,
    PRIMARY KEY (gym_id, method_code)
);

CREATE TABLE payments (
    id                 BIGSERIAL PRIMARY KEY,
    gym_id             BIGINT       NOT NULL REFERENCES gyms (id),
    user_id            BIGINT       NOT NULL REFERENCES users (id),
    plan_id            BIGINT       NOT NULL REFERENCES membership_plans (id),
    amount_cents       INTEGER      NOT NULL,
    currency           CHAR(3)      NOT NULL,
    method_code        VARCHAR(30)  NOT NULL REFERENCES payment_methods (code),
    status             VARCHAR(20)  NOT NULL,
    gateway            VARCHAR(20)  NOT NULL,
    external_reference VARCHAR(80)  NOT NULL UNIQUE,
    checkout_url       VARCHAR(500),
    failure_code       VARCHAR(40),
    failure_message    VARCHAR(240),
    attempt            SMALLINT     NOT NULL DEFAULT 1,
    expires_at         TIMESTAMPTZ  NOT NULL,
    completed_at       TIMESTAMPTZ,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_payments_status CHECK (
        status IN ('PENDING', 'COMPLETED', 'FAILED', 'EXPIRED', 'REFUNDED')
    ),
    CONSTRAINT ck_payments_amount CHECK (amount_cents > 0),
    CONSTRAINT ck_payments_attempt CHECK (attempt BETWEEN 1 AND 99)
);

CREATE INDEX idx_payments_user_created ON payments (user_id, created_at DESC);
CREATE INDEX idx_payments_gym_created ON payments (gym_id, created_at DESC);
CREATE INDEX idx_payments_status_expires ON payments (status, expires_at);
CREATE INDEX idx_payments_reference ON payments (external_reference);

-- Bitacora de eventos de la pasarela. El identificador del evento es unico:
-- la clave de idempotencia frente a reenvios y duplicados.
CREATE TABLE payment_events (
    id              BIGSERIAL PRIMARY KEY,
    event_id        VARCHAR(120) NOT NULL UNIQUE,
    event_type      VARCHAR(60)  NOT NULL,
    payment_id      BIGINT       REFERENCES payments (id) ON DELETE SET NULL,
    signature_valid BOOLEAN      NOT NULL,
    outcome         VARCHAR(40)  NOT NULL,
    received_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    processed_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_payment_events_payment ON payment_events (payment_id);

CREATE TABLE memberships (
    id         BIGSERIAL PRIMARY KEY,
    gym_id     BIGINT      NOT NULL REFERENCES gyms (id) ON DELETE CASCADE,
    user_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    plan_id    BIGINT      NOT NULL REFERENCES membership_plans (id),
    starts_at  DATE        NOT NULL,
    ends_at    DATE        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_memberships_gym_user UNIQUE (gym_id, user_id),
    CONSTRAINT ck_memberships_range CHECK (ends_at > starts_at)
);

CREATE INDEX idx_memberships_ends ON memberships (ends_at);

CREATE SEQUENCE payment_receipt_number_seq START WITH 1000 INCREMENT BY 1;

CREATE TABLE payment_receipts (
    payment_id BIGINT      PRIMARY KEY REFERENCES payments (id) ON DELETE RESTRICT,
    number     BIGINT      NOT NULL UNIQUE DEFAULT nextval('payment_receipt_number_seq'),
    issued_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TRIGGER trg_memberships_updated_at
    BEFORE UPDATE ON memberships
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_payments_updated_at
    BEFORE UPDATE ON payments
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

-- Planes del gimnasio semilla
INSERT INTO membership_plans (gym_id, code, name, description, price_cents, currency, duration_days)
SELECT id, 'MENSUAL', 'Plan mensual', 'Acceso ilimitado al gimnasio durante 30 dias',
       8990000, 'COP', 30
  FROM gyms WHERE slug = 'powerzone-centro';

INSERT INTO membership_plans (gym_id, code, name, description, price_cents, currency, duration_days)
SELECT id, 'TRIMESTRAL', 'Plan trimestral', 'Tres meses de acceso con seguimiento de rutina',
       23990000, 'COP', 90
  FROM gyms WHERE slug = 'powerzone-centro';

INSERT INTO membership_plans (gym_id, code, name, description, price_cents, currency, duration_days)
SELECT id, 'ANUAL', 'Plan anual', 'Doce meses de acceso con el mejor precio por mes',
       79990000, 'COP', 365
  FROM gyms WHERE slug = 'powerzone-centro';

INSERT INTO payment_methods (code, label, kind, immediate) VALUES
    ('CARD_CREDIT', 'Tarjeta de credito', 'CARD', TRUE),
    ('CARD_DEBIT',  'Tarjeta de debito',  'CARD', TRUE),
    ('PSE',         'PSE - transferencia bancaria', 'TRANSFER', FALSE),
    ('NEQUI',       'Nequi', 'WALLET', TRUE);

INSERT INTO gym_payment_methods (gym_id, method_code)
SELECT g.id, m.code
  FROM gyms g
 CROSS JOIN payment_methods m
 WHERE g.slug = 'powerzone-centro';
