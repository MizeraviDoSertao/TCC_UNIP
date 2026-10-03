DO $$
BEGIN
  IF to_regclass('public.bronze_transaction_raw') IS NOT NULL THEN
    EXECUTE $migration$
      INSERT INTO bronze.claim_raw (
        file_name, original_data, source, ingested_at
      )
      SELECT
        file_name,
        jsonb_build_object('_raw', raw_payload),
        COALESCE(source, 'legacy'),
        ingested_at
      FROM public.bronze_transaction_raw
    $migration$;
  END IF;
END $$;

DO $$
BEGIN
  IF to_regclass('public.silver_transaction') IS NOT NULL THEN
    EXECUTE $migration$
      INSERT INTO silver.claim (
        transaction_id,
        normalized_data,
        confirmed_fraud,
        file_name,
        processed_at
      )
      SELECT
        transaction_id,
        features_json::jsonb,
        real_fraud,
        file_name,
        processed_at
      FROM public.silver_transaction
      ON CONFLICT (transaction_id) DO NOTHING
    $migration$;
  END IF;
END $$;

DO $$
BEGIN
  IF to_regclass('public.gold_fraud_result') IS NOT NULL THEN
    EXECUTE $migration$
      INSERT INTO gold.fraud_prediction (
        transaction_id,
        confirmed_fraud,
        predicted_fraud,
        risk_score,
        score_type,
        risk_level,
        threshold,
        classification,
        model_version,
        reasons,
        processed_at
      )
      SELECT
        transaction_id,
        real_fraud,
        predicted_fraud,
        probability,
        'FRAUD_PROBABILITY',
        CASE
          WHEN probability >= 0.7 THEN 'HIGH'
          WHEN probability >= 0.3 THEN 'MEDIUM'
          ELSE 'LOW'
        END,
        0.5,
        classification,
        'legacy-model',
        '[]'::jsonb,
        processed_at
      FROM public.gold_fraud_result
      ON CONFLICT (transaction_id) DO NOTHING
    $migration$;
  END IF;
END $$;

DO $$
BEGIN
  IF to_regclass('public.rejected_record') IS NOT NULL THEN
    EXECUTE $migration$
      INSERT INTO ops.rejected_record (
        file_name,
        original_data,
        error_reason,
        rejected_at
      )
      SELECT
        file_name,
        jsonb_build_object('_raw', raw_payload),
        error_reason,
        rejected_at
      FROM public.rejected_record
    $migration$;
  END IF;
END $$;
