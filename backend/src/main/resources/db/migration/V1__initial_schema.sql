-- V1: esquema base multi-tenant de PowerZone
-- Sprint 0 - Foundation (PPP-66)

CREATE TABLE gyms (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(120) NOT NULL,
    slug        VARCHAR(60)  NOT NULL UNIQUE,
    timezone    VARCHAR(60)  NOT NULL DEFAULT 'America/Bogota',
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE roles (
    id     SMALLSERIAL PRIMARY KEY,
    code   VARCHAR(30) NOT NULL UNIQUE,
    name   VARCHAR(60) NOT NULL
);

INSERT INTO roles (code, name) VALUES
    ('MEMBER',      'Miembro'),
    ('TRAINER',     'Entrenador'),
    ('ADMIN',       'Administrador de gimnasio'),
    ('SUPER_ADMIN', 'Super administrador');

CREATE TABLE users (
    id                BIGSERIAL PRIMARY KEY,
    gym_id            BIGINT       NOT NULL REFERENCES gyms (id),
    role_id           SMALLINT     NOT NULL REFERENCES roles (id),
    email             VARCHAR(160) NOT NULL,
    password_hash     VARCHAR(100) NOT NULL,
    first_name        VARCHAR(80)  NOT NULL,
    last_name         VARCHAR(80)  NOT NULL,
    phone             VARCHAR(30),
    birth_date        DATE,
    height_cm         NUMERIC(5, 2),
    weight_kg         NUMERIC(5, 2),
    goal              VARCHAR(30),
    experience_level  VARCHAR(20),
    days_per_week     SMALLINT,
    active            BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_users_gym_email UNIQUE (gym_id, email),
    CONSTRAINT ck_users_goal CHECK (goal IS NULL OR goal IN ('STRENGTH', 'HYPERTROPHY', 'WEIGHT_LOSS', 'ENDURANCE')),
    CONSTRAINT ck_users_level CHECK (experience_level IS NULL OR experience_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
    CONSTRAINT ck_users_days CHECK (days_per_week IS NULL OR days_per_week BETWEEN 2 AND 7)
);

CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_gym ON users (gym_id);

CREATE TABLE refresh_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  VARCHAR(64) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked     BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens (expires_at);

CREATE TABLE password_reset_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  VARCHAR(64) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    used        BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_password_reset_user ON password_reset_tokens (user_id);

-- Gimnasio inicial para el entorno de desarrollo
INSERT INTO gyms (name, slug) VALUES ('PowerZone Centro', 'powerzone-centro');

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();
