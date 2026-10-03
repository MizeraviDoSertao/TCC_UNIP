from __future__ import annotations

import numpy as np

from fraud_detection.domain import ModelBundle, ModelType, ScoringRequest, ScoringResult
from fraud_detection.normalization import prepare_inference_frame


class FraudScoringService:
  def __init__(self, bundle: ModelBundle) -> None:
    self._bundle = bundle

  def score(self, request: ScoringRequest) -> ScoringResult:
    self._validate(request)
    frame = prepare_inference_frame(
      request.features,
      self._bundle.feature_columns,
      self._bundle.numeric_columns,
      self._bundle.categorical_columns,
    )
    if self._bundle.model_type == ModelType.SUPERVISED:
      probability = float(self._bundle.pipeline.predict_proba(frame)[0][1])
      predicted_fraud: bool | None = probability >= self._bundle.threshold
      score_type = "FRAUD_PROBABILITY"
      reasons = tuple(
        f"Relevant model feature: {feature}"
        for feature in self._bundle.important_features[:3]
      )
    else:
      raw_score = float(self._bundle.pipeline.decision_function(frame)[0])
      probability = self._anomaly_probability(raw_score)
      predicted_fraud = None
      score_type = "ANOMALY_SCORE"
      reasons = (
        "Pattern differs from the training population",
        "Human review is required before confirming fraud",
      )

    probability = round(float(np.clip(probability, 0.0, 1.0)), 4)
    risk_level = _risk_level(probability)
    return ScoringResult(
      transaction_id=request.transaction_id,
      real_fraud=request.real_fraud,
      predicted_fraud=predicted_fraud,
      probability=probability,
      score_type=score_type,
      risk_level=risk_level,
      threshold=self._bundle.threshold,
      classification=_classification(score_type, predicted_fraud, risk_level),
      model_version=self._bundle.model_version,
      reasons=reasons,
    )

  def _anomaly_probability(self, raw_score: float) -> float:
    low = self._bundle.anomaly_low_score
    high = self._bundle.anomaly_high_score
    if low is None or high is None or high <= low:
      raise ValueError("Anomaly model does not contain valid score calibration")
    return (high - raw_score) / (high - low)

  def _validate(self, request: ScoringRequest) -> None:
    if not request.transaction_id or not request.transaction_id.strip():
      raise ValueError("transactionId is required")
    if len(request.transaction_id) > 64:
      raise ValueError("transactionId cannot exceed 64 characters")
    if not request.features:
      raise ValueError("features must contain at least one attribute")
    if request.real_fraud is not None and not isinstance(request.real_fraud, bool):
      raise ValueError("realFraud must be boolean or null")


def _risk_level(probability: float) -> str:
  if probability >= 0.7:
    return "HIGH"
  if probability >= 0.3:
    return "MEDIUM"
  return "LOW"


def _classification(score_type: str, predicted_fraud: bool | None, risk_level: str) -> str:
  if score_type == "ANOMALY_SCORE":
    return f"{risk_level.title()} anomaly risk - review required"
  return "Suspicious transaction" if predicted_fraud else "Normal transaction"
