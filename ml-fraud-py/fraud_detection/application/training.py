from __future__ import annotations

import hashlib
from pathlib import Path

from fraud_detection.domain import TrainingReport, TrainingRequest
from fraud_detection.normalization import (
  find_target_column,
  normalize_frame_columns,
  parse_binary_target,
)
from fraud_detection.ports import DatasetReader, ModelRepository
from fraud_detection.training import TrainingStrategyFactory


class ModelTrainingService:
  def __init__(self, reader: DatasetReader, repository: ModelRepository) -> None:
    self._reader = reader
    self._repository = repository

  def train(self, request: TrainingRequest) -> TrainingReport:
    dataset_path = Path(request.dataset_path).expanduser().resolve()
    artifact_path = Path(request.artifact_path).expanduser().resolve()
    frame = normalize_frame_columns(self._reader.read(dataset_path))
    if frame.empty:
      raise ValueError("Dataset is empty")

    target_column = find_target_column(list(frame.columns), request.target_aliases)
    target = parse_binary_target(frame[target_column]) if target_column else None
    features = frame.drop(columns=[target_column]) if target_column else frame
    if features.empty:
      raise ValueError("Dataset has no feature columns")

    strategy = TrainingStrategyFactory(
      request.random_state,
      request.test_size,
      request.anomaly_contamination,
    ).create(has_target=target_column is not None)
    bundle = strategy.train(features, target, target_column)
    dataset_sha256 = _sha256(dataset_path)
    bundle.dataset_sha256 = dataset_sha256
    bundle.model_version = f"{bundle.model_version}-{dataset_sha256[:8]}"
    self._repository.save(bundle, artifact_path)
    return TrainingReport(
      model_version=bundle.model_version,
      model_type=bundle.model_type,
      artifact_path=str(artifact_path),
      rows=len(frame),
      feature_count=len(bundle.feature_columns),
      target_column=target_column,
      metrics=bundle.metrics,
      dataset_sha256=dataset_sha256,
    )


def _sha256(path: Path) -> str:
  digest = hashlib.sha256()
  with path.open("rb") as source:
    for chunk in iter(lambda: source.read(1024 * 1024), b""):
      digest.update(chunk)
  return digest.hexdigest()
