from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path

DEFAULT_TARGET_ALIASES = (
  "fraudfound_p",
  "fraud",
  "is_fraud",
  "fraude",
  "isfraud",
  "label",
)


@dataclass(frozen=True, slots=True)
class Settings:
  artifact_path: Path
  kafka_bootstrap_servers: str
  input_topic: str
  output_topic: str
  consumer_group: str
  target_aliases: tuple[str, ...]
  random_state: int
  test_size: float
  anomaly_contamination: float
  kafka_retry_seconds: float
  message_max_retries: int

  @classmethod
  def from_environment(cls) -> "Settings":
    aliases = tuple(
      value.strip()
      for value in os.getenv(
        "FRAUD_TARGET_ALIASES", ",".join(DEFAULT_TARGET_ALIASES)
      ).split(",")
      if value.strip()
    )
    return cls(
      artifact_path=Path(
        os.getenv("FRAUD_MODEL_PATH", "artifacts/fraud_model.joblib")
      ),
      kafka_bootstrap_servers=os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092"),
      input_topic=os.getenv("KAFKA_TRANSACTIONS_TOPIC", "transactions"),
      output_topic=os.getenv("KAFKA_RESULTS_TOPIC", "fraud-results"),
      consumer_group=os.getenv("KAFKA_CONSUMER_GROUP", "ml-fraud-consumer"),
      target_aliases=aliases,
      random_state=_integer("FRAUD_RANDOM_STATE", 42),
      test_size=_bounded_float("FRAUD_TEST_SIZE", 0.25, minimum=0.05, maximum=0.5),
      anomaly_contamination=_bounded_float(
        "FRAUD_ANOMALY_CONTAMINATION", 0.05, minimum=0.001, maximum=0.5
      ),
      kafka_retry_seconds=_bounded_float(
        "KAFKA_RETRY_SECONDS", 5.0, minimum=0.1, maximum=300.0
      ),
      message_max_retries=_integer("KAFKA_MESSAGE_MAX_RETRIES", 3, minimum=1),
    )


def _integer(name: str, default: int, minimum: int | None = None) -> int:
  value = int(os.getenv(name, str(default)))
  if minimum is not None and value < minimum:
    raise ValueError(f"{name} must be at least {minimum}")
  return value


def _bounded_float(name: str, default: float, minimum: float, maximum: float) -> float:
  value = float(os.getenv(name, str(default)))
  if not minimum <= value <= maximum:
    raise ValueError(f"{name} must be between {minimum} and {maximum}")
  return value
