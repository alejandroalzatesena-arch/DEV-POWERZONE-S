-- V2: modelo de rutinas, catalogo de ejercicios y progreso
-- Sprint 2 - Rutinas (PPP-270)

CREATE TABLE exercises (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(120) NOT NULL,
    muscle_group VARCHAR(40)  NOT NULL,
    equipment    VARCHAR(40)  NOT NULL,
    difficulty   VARCHAR(20)  NOT NULL,
    description  VARCHAR(300),
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_exercises_name UNIQUE (name),
    CONSTRAINT ck_exercises_group CHECK (muscle_group IN (
        'CHEST', 'BACK', 'LEGS', 'SHOULDERS', 'ARMS', 'CORE', 'FULL_BODY', 'CARDIO')),
    CONSTRAINT ck_exercises_difficulty CHECK (difficulty IN (
        'BEGINNER', 'INTERMEDIATE', 'ADVANCED'))
);

CREATE INDEX idx_exercises_group ON exercises (muscle_group);

INSERT INTO exercises (name, muscle_group, equipment, difficulty, description) VALUES
    ('Press de banca',           'CHEST',     'BARBELL',    'INTERMEDIATE', 'Empuje horizontal con barra.'),
    ('Press inclinado con mancuernas', 'CHEST', 'DUMBBELL', 'INTERMEDIATE', 'Enfasis en porcion superior del pectoral.'),
    ('Aperturas en polea',       'CHEST',     'CABLE',      'BEGINNER',     'Aislamiento de pectoral con poleas.'),
    ('Flexiones',                'CHEST',     'BODYWEIGHT', 'BEGINNER',     'Empuje con peso corporal.'),
    ('Dominadas',                'BACK',      'BODYWEIGHT', 'ADVANCED',     'Traccion vertical con peso corporal.'),
    ('Remo con barra',           'BACK',      'BARBELL',    'INTERMEDIATE', 'Traccion horizontal con barra.'),
    ('Jalon al pecho',           'BACK',      'CABLE',      'BEGINNER',     'Traccion vertical en polea alta.'),
    ('Remo en polea baja',       'BACK',      'CABLE',      'BEGINNER',     'Traccion horizontal sentado.'),
    ('Sentadilla',               'LEGS',      'BARBELL',    'INTERMEDIATE', 'Patron principal de pierna.'),
    ('Prensa de piernas',        'LEGS',      'MACHINE',    'BEGINNER',     'Empuje guiado de cuádriceps.'),
    ('Peso muerto rumano',       'LEGS',      'BARBELL',    'ADVANCED',     'Cadena posterior con bisagra de cadera.'),
    ('Zancadas',                 'LEGS',      'DUMBBELL',   'BEGINNER',     'Trabajo unilateral de pierna.'),
    ('Curl femoral',             'LEGS',      'MACHINE',    'BEGINNER',     'Flexion de rodilla en maquina.'),
    ('Elevacion de gemelos',     'LEGS',      'MACHINE',    'BEGINNER',     'Extension de tobillo.'),
    ('Press militar',            'SHOULDERS', 'BARBELL',    'INTERMEDIATE', 'Empuje vertical con barra.'),
    ('Elevaciones laterales',    'SHOULDERS', 'DUMBBELL',   'BEGINNER',     'Aislamiento del deltoides medio.'),
    ('Face pull',                'SHOULDERS', 'CABLE',      'BEGINNER',     'Trabajo de deltoides posterior.'),
    ('Curl de biceps',           'ARMS',      'DUMBBELL',   'BEGINNER',     'Flexion de codo con mancuernas.'),
    ('Curl martillo',            'ARMS',      'DUMBBELL',   'BEGINNER',     'Curl neutro con mancuernas.'),
    ('Extension de triceps',     'ARMS',      'CABLE',      'BEGINNER',     'Extension de codo en polea.'),
    ('Fondos en banco',          'ARMS',      'BODYWEIGHT', 'BEGINNER',     'Extension de codo con peso corporal.'),
    ('Plancha',                  'CORE',      'BODYWEIGHT', 'BEGINNER',     'Isometrico de core frontal.'),
    ('Plancha lateral',          'CORE',      'BODYWEIGHT', 'INTERMEDIATE', 'Isometrico de oblicuos.'),
    ('Elevacion de piernas',     'CORE',      'BODYWEIGHT', 'BEGINNER',     'Flexion de cadera suspendido.'),
    ('Burpees',                  'FULL_BODY', 'BODYWEIGHT', 'INTERMEDIATE', 'Ejercicio global de acondicionamiento.'),
    ('Kettlebell swing',         'FULL_BODY', 'KETTLEBELL', 'INTERMEDIATE', 'Bisagra de cadera explosiva.'),
    ('Remo con mancuerna',       'FULL_BODY', 'DUMBBELL',   'BEGINNER',     'Trabajo global de espalda y core.'),
    ('Cinta continua',           'CARDIO',    'MACHINE',    'BEGINNER',     'Cardio de baja intensidad.'),
    ('Intervalos en bicicleta',  'CARDIO',    'MACHINE',    'BEGINNER',     'Cardio por intervalos.');

CREATE TABLE routines (
    id                BIGSERIAL PRIMARY KEY,
    gym_id            BIGINT       NOT NULL REFERENCES gyms (id),
    user_id           BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name              VARCHAR(120) NOT NULL,
    goal              VARCHAR(30)  NOT NULL,
    experience_level  VARCHAR(20)  NOT NULL,
    days_per_week     SMALLINT     NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    source            VARCHAR(20)  NOT NULL DEFAULT 'GENERATED',
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_routines_goal CHECK (goal IN (
        'STRENGTH', 'HYPERTROPHY', 'WEIGHT_LOSS', 'ENDURANCE')),
    CONSTRAINT ck_routines_level CHECK (experience_level IN (
        'BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
    CONSTRAINT ck_routines_days CHECK (days_per_week BETWEEN 2 AND 7),
    CONSTRAINT ck_routines_status CHECK (status IN ('ACTIVE', 'ARCHIVED'))
);

CREATE INDEX idx_routines_user ON routines (user_id);
CREATE INDEX idx_routines_gym ON routines (gym_id);

CREATE TABLE routine_days (
    id         BIGSERIAL PRIMARY KEY,
    routine_id BIGINT      NOT NULL REFERENCES routines (id) ON DELETE CASCADE,
    day_number SMALLINT    NOT NULL,
    name       VARCHAR(80) NOT NULL,
    focus      VARCHAR(80) NOT NULL,
    CONSTRAINT uq_routine_days UNIQUE (routine_id, day_number)
);

CREATE TABLE routine_exercises (
    id            BIGSERIAL PRIMARY KEY,
    routine_day_id BIGINT      NOT NULL REFERENCES routine_days (id) ON DELETE CASCADE,
    exercise_id   BIGINT       NOT NULL REFERENCES exercises (id),
    position      SMALLINT     NOT NULL,
    sets          SMALLINT     NOT NULL,
    reps          VARCHAR(20)  NOT NULL,
    rest_seconds  SMALLINT     NOT NULL,
    notes         VARCHAR(200),
    CONSTRAINT uq_routine_exercises UNIQUE (routine_day_id, position),
    CONSTRAINT ck_routine_exercises_sets CHECK (sets BETWEEN 1 AND 10),
    CONSTRAINT ck_routine_exercises_rest CHECK (rest_seconds BETWEEN 10 AND 600)
);

CREATE INDEX idx_routine_exercises_day ON routine_exercises (routine_day_id);
CREATE INDEX idx_routine_exercises_exercise ON routine_exercises (exercise_id);

CREATE TABLE exercise_progress (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    exercise_id         BIGINT       NOT NULL REFERENCES exercises (id),
    routine_exercise_id BIGINT       REFERENCES routine_exercises (id) ON DELETE SET NULL,
    performed_on        DATE         NOT NULL DEFAULT CURRENT_DATE,
    weight_kg           NUMERIC(6, 2),
    reps                SMALLINT,
    sets                SMALLINT,
    notes               VARCHAR(200),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_progress_reps CHECK (reps IS NULL OR reps BETWEEN 1 AND 100),
    CONSTRAINT ck_progress_sets CHECK (sets IS NULL OR sets BETWEEN 1 AND 10),
    CONSTRAINT ck_progress_weight CHECK (weight_kg IS NULL OR weight_kg BETWEEN 0 AND 500)
);

CREATE INDEX idx_progress_user ON exercise_progress (user_id, performed_on DESC);
CREATE INDEX idx_progress_exercise ON exercise_progress (exercise_id);

CREATE TRIGGER trg_routines_updated_at
    BEFORE UPDATE ON routines
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();
