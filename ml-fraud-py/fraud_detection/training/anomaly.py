from __future__ import annotations

from datetime import UTC, datetime

import numpy as np
import pandas as pd
from sklearn.ensemble import IsolationForest
from sklearn.pipeline import Pipeline

from fraud_detection.domain import ModelBundle, ModelType
from fraud_detection.normalization import coerce_feature_types
from fraud_detection.preprocessing import build_preprocessor


class AnomalyTrainingStrategy:
    def __init__(self, random_state: int, contamination: float) -> None:
        self._random_state = random_state
        self._contamination = contamination

    def train(
        self,
        features: pd.DataFrame,
        target: pd.Series | None,
        target_column: str | None,
    ) -> ModelBundle:
        if len(features) < 8:
            raise ValueError("Anomaly training requires at least 8 rows")
        typed, numeric, categorical = coerce_feature_types(features)
        pipeline = Pipeline(
            [
                ("preprocessor", build_preprocessor(numeric, categorical, scale_numeric=True)),
                (
                    "model",
                    IsolationForest(
                        n_estimators=300,
                        contamination=self._contamination,
                        random_state=self._random_state,
                        n_jobs=-1,
                    ),
                ),
            ]
        )
        pipeline.fit(typed)
        scores = pipeline.decision_function(typed)
        low_score = float(np.quantile(scores, 0.05))
        high_score = float(np.quantile(scores, 0.90))
        if high_score <= low_score:
            high_score = low_score + 1e-9
        anomaly_rate = float((pipeline.predict(typed) == -1).mean())
        return ModelBundle(
            model_version=f"anomaly-iforest-{datetime.now(UTC):%Y%m%d%H%M%S}",
            model_type=ModelType.ANOMALY,
            pipeline=pipeline,
            feature_columns=tuple(typed.columns),
            numeric_columns=numeric,
            categorical_columns=categorical,
            threshold=0.7,
            metrics={
                "training_anomaly_rate": anomaly_rate,
                "score_low_reference": low_score,
                "score_high_reference": high_score,
            },
            target_column=None,
            anomaly_low_score=low_score,
            anomaly_high_score=high_score,
        )
