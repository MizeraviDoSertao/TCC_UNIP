CREATE TABLE IF NOT EXISTS bronze.dataset_schema (
    id BIGSERIAL PRIMARY KEY,
    import_id UUID NOT NULL,
    sheet_name VARCHAR(255) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    normalized_name VARCHAR(120) NOT NULL,
    inferred_type VARCHAR(24) NOT NULL,
    column_role VARCHAR(24) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_dataset_schema_column UNIQUE (
        import_id,
        sheet_name,
        original_name
    )
);

CREATE INDEX IF NOT EXISTS idx_dataset_schema_import
    ON bronze.dataset_schema (import_id, normalized_name);
