CREATE SCHEMA IF NOT EXISTS bronze;
CREATE SCHEMA IF NOT EXISTS silver;
CREATE SCHEMA IF NOT EXISTS gold;
CREATE SCHEMA IF NOT EXISTS ops;
CREATE SCHEMA IF NOT EXISTS review;
CREATE SCHEMA IF NOT EXISTS ml;

CREATE TABLE IF NOT EXISTS ops.import_job (
    id UUID PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    stored_path TEXT NOT NULL,
    file_hash VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    total_rows BIGINT NOT NULL DEFAULT 0,
    processed_rows BIGINT NOT NULL DEFAULT 0,
    rejected_rows BIGINT NOT NULL DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL,
    started_at TIMESTAMP,
    finished_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_import_job_created_at
    ON ops.import_job (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_import_job_file_hash
    ON ops.import_job (file_hash);

CREATE TABLE IF NOT EXISTS bronze.claim_raw (
    id BIGSERIAL PRIMARY KEY,
    import_id UUID,
    file_name VARCHAR(255) NOT NULL,
    sheet_name VARCHAR(255),
    row_number BIGINT,
    original_data JSONB NOT NULL,
    source VARCHAR(64) NOT NULL,
    ingested_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_bronze_import_row UNIQUE (import_id, sheet_name, row_number)
);

CREATE INDEX IF NOT EXISTS idx_bronze_claim_raw_import
    ON bronze.claim_raw (import_id);

CREATE INDEX IF NOT EXISTS idx_bronze_claim_raw_data
    ON bronze.claim_raw USING GIN (original_data);

CREATE TABLE IF NOT EXISTS silver.claim (
    transaction_id VARCHAR(64) PRIMARY KEY,
    import_id UUID,
    row_number BIGINT,
    normalized_data JSONB NOT NULL,
    confirmed_fraud BOOLEAN,
    file_name VARCHAR(255),
    processed_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_silver_claim_import
    ON silver.claim (import_id);

CREATE INDEX IF NOT EXISTS idx_silver_claim_data
    ON silver.claim USING GIN (normalized_data);

CREATE TABLE IF NOT EXISTS gold.fraud_prediction (
    transaction_id VARCHAR(64) PRIMARY KEY,
    confirmed_fraud BOOLEAN,
    predicted_fraud BOOLEAN,
    risk_score DOUBLE PRECISION NOT NULL,
    score_type VARCHAR(32) NOT NULL DEFAULT 'FRAUD_PROBABILITY',
    risk_level VARCHAR(16) NOT NULL DEFAULT 'LOW',
    threshold DOUBLE PRECISION,
    classification VARCHAR(255),
    model_version VARCHAR(64),
    reasons JSONB NOT NULL DEFAULT '[]'::jsonb,
    processed_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_gold_fraud_prediction_risk
    ON gold.fraud_prediction (risk_score DESC);

CREATE TABLE IF NOT EXISTS ops.rejected_record (
    id BIGSERIAL PRIMARY KEY,
    import_id UUID,
    file_name VARCHAR(255) NOT NULL,
    row_number BIGINT,
    original_data JSONB NOT NULL,
    error_reason TEXT NOT NULL,
    rejected_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_rejected_record_import
    ON ops.rejected_record (import_id);

CREATE TABLE IF NOT EXISTS review.fraud_review (
    id BIGSERIAL PRIMARY KEY,
    transaction_id VARCHAR(64) NOT NULL,
    decision VARCHAR(24) NOT NULL,
    notes TEXT,
    reviewer VARCHAR(120),
    reviewed_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_fraud_review_transaction
    ON review.fraud_review (transaction_id, reviewed_at DESC);

CREATE TABLE IF NOT EXISTS ml.model_registry (
    model_version VARCHAR(64) PRIMARY KEY,
    model_type VARCHAR(32) NOT NULL,
    feature_schema JSONB NOT NULL,
    metrics JSONB NOT NULL,
    threshold DOUBLE PRECISION,
    active BOOLEAN NOT NULL DEFAULT FALSE,
    trained_at TIMESTAMP NOT NULL
);
