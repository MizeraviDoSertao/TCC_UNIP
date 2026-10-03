ALTER TABLE ops.import_job
    ADD COLUMN IF NOT EXISTS batch_execution_id BIGINT;
