from __future__ import annotations

import json
import logging
import time
from dataclasses import dataclass
from typing import Any

from kafka import KafkaConsumer, KafkaProducer
from kafka.errors import NoBrokersAvailable

from fraud_detection.application.scoring import FraudScoringService
from fraud_detection.config import Settings
from fraud_detection.domain import ScoringRequest

logger = logging.getLogger(__name__)


@dataclass(frozen=True, slots=True)
class IncomingMessage:
  key: bytes | None
  value: dict[str, Any]
  topic: str
  partition: int
  offset: int


class KafkaScoringWorker:
  def __init__(self, settings: Settings, scoring_service: FraudScoringService) -> None:
    self._settings = settings
    self._scoring_service = scoring_service

  def run_forever(self) -> None:
    consumer = self._connect_consumer()
    producer = self._connect_producer()
    logger.info("Fraud scoring worker listening to %s", self._settings.input_topic)
    for raw_message in consumer:
      message = IncomingMessage(
        key=raw_message.key,
        value=raw_message.value,
        topic=raw_message.topic,
        partition=raw_message.partition,
        offset=raw_message.offset,
      )
      self._process_with_retry(message, consumer, producer)

  def _process_with_retry(self, message: IncomingMessage, consumer, producer) -> None:
    for attempt in range(1, self._settings.message_max_retries + 1):
      try:
        result = self._process(message.value)
        producer.send(
          self._settings.output_topic,
          key=message.key,
          value=result,
        ).get(timeout=30)
        consumer.commit()
        logger.info("Scored transaction %s", result["transactionId"])
        return
      except Exception as exception:  # Kafka loop must isolate individual messages.
        logger.exception(
          "Could not score message %s/%s/%s (attempt %s)",
          message.topic,
          message.partition,
          message.offset,
          attempt,
        )
        if attempt < self._settings.message_max_retries:
          time.sleep(self._settings.kafka_retry_seconds)
          continue
        producer.send(
          f"{self._settings.input_topic}.DLT",
          key=message.key,
          value={
            "source": message.value,
            "error": str(exception),
            "topic": message.topic,
            "partition": message.partition,
            "offset": message.offset,
          },
        ).get(timeout=30)
        consumer.commit()

  def _process(self, payload: dict[str, Any]) -> dict[str, Any]:
    if not isinstance(payload, dict):
      raise ValueError("Kafka payload must be a JSON object")
    transaction_id = payload.get("transactionId")
    features = payload.get("features")
    real_fraud = payload.get("realFraud")
    if not isinstance(transaction_id, str):
      raise ValueError("transactionId must be a string")
    if not isinstance(features, dict):
      raise ValueError("features must be a JSON object")
    if real_fraud is not None and not isinstance(real_fraud, bool):
      raise ValueError("realFraud must be boolean or null")
    result = self._scoring_service.score(
      ScoringRequest(
        transaction_id=transaction_id,
        real_fraud=real_fraud,
        features=features,
      )
    )
    return result.to_event()

  def _connect_consumer(self) -> KafkaConsumer:
    while True:
      try:
        return KafkaConsumer(
          self._settings.input_topic,
          bootstrap_servers=self._settings.kafka_bootstrap_servers,
          key_deserializer=lambda value: value,
          value_deserializer=lambda value: json.loads(value.decode("utf-8")),
          auto_offset_reset="earliest",
          enable_auto_commit=False,
          group_id=self._settings.consumer_group,
        )
      except NoBrokersAvailable:
        logger.warning(
          "Kafka is unavailable; retrying in %.1f seconds",
          self._settings.kafka_retry_seconds,
        )
        time.sleep(self._settings.kafka_retry_seconds)

  def _connect_producer(self) -> KafkaProducer:
    while True:
      try:
        return KafkaProducer(
          bootstrap_servers=self._settings.kafka_bootstrap_servers,
          acks="all",
          retries=5,
          key_serializer=lambda value: value,
          value_serializer=lambda value: json.dumps(
            value, ensure_ascii=False, allow_nan=False
          ).encode("utf-8"),
        )
      except NoBrokersAvailable:
        logger.warning(
          "Kafka is unavailable; retrying in %.1f seconds",
          self._settings.kafka_retry_seconds,
        )
        time.sleep(self._settings.kafka_retry_seconds)
