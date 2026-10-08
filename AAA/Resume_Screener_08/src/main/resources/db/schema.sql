CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS admin_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL DEFAULT 'ADMIN'
);

CREATE TABLE IF NOT EXISTS vacancy_post (
    id BIGSERIAL PRIMARY KEY,
    job_title VARCHAR(255) NOT NULL,
    department VARCHAR(120) NOT NULL DEFAULT 'Engineering',
    job_description TEXT,
    required_skills TEXT,
    preferred_skills TEXT,
    qualifications TEXT,
    education_requirements TEXT,
    experience_requirements TEXT,
    responsibilities TEXT,
    other_requirements TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN'
);

CREATE TABLE IF NOT EXISTS candidate_resume (
    id BIGSERIAL PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    email VARCHAR(255),
    phone VARCHAR(255),
    raw_text TEXT,
    qualifications TEXT,
    skills TEXT,
    experience TEXT,
    uploaded_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS vacancy_candidate (
    id BIGSERIAL PRIMARY KEY,
    vacancy_id BIGINT NOT NULL REFERENCES vacancy_post(id) ON DELETE CASCADE,
    candidate_id BIGINT NOT NULL REFERENCES candidate_resume(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (vacancy_id, candidate_id)
);

CREATE TABLE IF NOT EXISTS candidate_embedding (
    id BIGSERIAL PRIMARY KEY,
    candidate_id BIGINT NOT NULL UNIQUE REFERENCES candidate_resume(id) ON DELETE CASCADE,
    embedding_text TEXT,
    embedding vector(768)
);

CREATE TABLE IF NOT EXISTS vacancy_embedding (
    id BIGSERIAL PRIMARY KEY,
    vacancy_id BIGINT NOT NULL UNIQUE REFERENCES vacancy_post(id) ON DELETE CASCADE,
    embedding_text TEXT,
    embedding vector(768)
);

CREATE TABLE IF NOT EXISTS screening_run (
    id BIGSERIAL PRIMARY KEY,
    vacancy_id BIGINT REFERENCES vacancy_post(id) ON DELETE SET NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    finished_at TIMESTAMP,
    notes TEXT
);

CREATE TABLE IF NOT EXISTS screening_result (
    id BIGSERIAL PRIMARY KEY,
    screening_run_id BIGINT NOT NULL REFERENCES screening_run(id) ON DELETE CASCADE,
    candidate_id BIGINT NOT NULL REFERENCES candidate_resume(id) ON DELETE CASCADE,
    mcdm_score DOUBLE PRECISION NOT NULL DEFAULT 0,
    reranker_score DOUBLE PRECISION NOT NULL DEFAULT 0,
    final_score DOUBLE PRECISION NOT NULL DEFAULT 0,
    reasoning TEXT,
    rank INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS semifinalized_candidate (
    id BIGSERIAL PRIMARY KEY,
    screening_run_id BIGINT NOT NULL REFERENCES screening_run(id) ON DELETE CASCADE,
    candidate_id BIGINT NOT NULL REFERENCES candidate_resume(id) ON DELETE CASCADE,
    score DOUBLE PRECISION NOT NULL DEFAULT 0,
    rank INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS finalized_candidate (
    id BIGSERIAL PRIMARY KEY,
    candidate_id BIGINT NOT NULL REFERENCES candidate_resume(id) ON DELETE CASCADE,
    screening_run_id BIGINT NOT NULL REFERENCES screening_run(id) ON DELETE CASCADE,
    final_score DOUBLE PRECISION NOT NULL DEFAULT 0,
    recommendation TEXT,
    qwen_evaluation TEXT,
    rank INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_candidate_email ON candidate_resume (lower(email));
CREATE INDEX IF NOT EXISTS idx_vacancy_status ON vacancy_post (status);
CREATE INDEX IF NOT EXISTS idx_screening_run_vacancy ON screening_run (vacancy_id);
