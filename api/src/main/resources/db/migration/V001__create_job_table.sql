CREATE TABLE job (
    job_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    volume DECIMAL(12, 2) NOT NULL,
    estimated_time DECIMAL(7, 2) NOT NULL,
    actual_time DECIMAL(7, 2),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMPTZ,
    canceled_at TIMESTAMPTZ,
    edited_at TIMESTAMPTZ,

    CONSTRAINT chk_status
        CHECK(status IN ('IN_PROGRESS', 'COMPLETED', 'CANCELED')),

    CONSTRAINT chk_status_dates
        CHECK(
            (status = 'IN_PROGRESS' AND finished_at IS NULL AND canceled_at  IS NULL) OR
            (status = 'COMPLETED' AND finished_at IS NOT NULL AND canceled_at IS NULL) OR
            (status = 'CANCELED' AND canceled_at IS NOT NULL)
        )

);

CREATE INDEX idx_job_status ON job (status);