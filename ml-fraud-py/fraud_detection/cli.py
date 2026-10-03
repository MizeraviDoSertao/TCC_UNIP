from __future__ import annotations

import argparse
import json
import logging
from dataclasses import asdict
from pathlib import Path
from typing import Sequence

from fraud_detection.application.scoring import FraudScoringService
from fraud_detection.application.training import ModelTrainingService
from fraud_detection.config import Settings
from fraud_detection.domain import TrainingRequest
from fraud_detection.infrastructure.dataset import PandasDatasetReader
from fraud_detection.infrastructure.kafka import KafkaScoringWorker
from fraud_detection.infrastructure.model_repository import JoblibModelRepository
from fraud_detection.normalization import normalize_frame_columns


def main(arguments: Sequence[str] | None = None) -> int:
  parser = _parser()
  args = parser.parse_args(arguments)
  logging.basicConfig(
    level=getattr(logging, args.log_level.upper()),
    format="%(asctime)s %(levelname)s %(name)s - %(message)s",
  )
  settings = Settings.from_environment()
  if args.command == "train":
    return _train(args, settings)
  if args.command == "consume":
    return _consume(settings)
  if args.command == "inspect":
    return _inspect(args)
  parser.error("A command is required")
  return 2


def _train(args: argparse.Namespace, settings: Settings) -> int:
  artifact = Path(args.artifact) if args.artifact else settings.artifact_path
  report = ModelTrainingService(PandasDatasetReader(), JoblibModelRepository()).train(
    TrainingRequest(
      dataset_path=args.dataset,
      artifact_path=str(artifact),
      target_aliases=settings.target_aliases,
      random_state=settings.random_state,
      test_size=settings.test_size,
      anomaly_contamination=settings.anomaly_contamination,
    )
  )
  print(json.dumps(asdict(report), indent=2, ensure_ascii=False))
  return 0


def _consume(settings: Settings) -> int:
  bundle = JoblibModelRepository().load(settings.artifact_path.resolve())
  KafkaScoringWorker(settings, FraudScoringService(bundle)).run_forever()
  return 0


def _inspect(args: argparse.Namespace) -> int:
  frame = normalize_frame_columns(PandasDatasetReader().read(Path(args.dataset).resolve()))
  print(json.dumps({"columns": list(frame.columns), "rows": len(frame)}, indent=2))
  return 0


def _parser() -> argparse.ArgumentParser:
  parser = argparse.ArgumentParser(description="Fraud model training and Kafka scoring")
  parser.add_argument("--log-level", default="INFO")
  commands = parser.add_subparsers(dest="command")
  train = commands.add_parser("train", help="Train a supervised or anomaly model")
  train.add_argument("--dataset", required=True)
  train.add_argument("--artifact")
  commands.add_parser("consume", help="Consume Kafka transactions and publish scores")
  inspect = commands.add_parser("inspect", help="Inspect normalized dataset columns")
  inspect.add_argument("--dataset", required=True)
  return parser


if __name__ == "__main__":
  raise SystemExit(main())
