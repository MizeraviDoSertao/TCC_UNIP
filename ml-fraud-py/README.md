# Fraud detection service

Python service responsible for model training and Kafka inference. The Java
backend remains responsible for ingestion, medallion persistence and human
review.

## Architecture

```text
CLI / Kafka adapter
       |
       v
application services -> domain <- ports
       ^                            ^
       |                            |
dataset, Joblib and Kafka adapters  training strategies
```

Applied patterns:

- Hexagonal architecture and dependency inversion around datasets and model storage.
- Strategy + Factory to select supervised classification or anomaly detection.
- Repository for atomic, versioned model artifact persistence.
- Pipeline for identical preprocessing during training and inference.
- Consumer retry with manual Kafka offset commit and dead-letter topic.

## Training modes

If one configured target alias exists, the service validates binary labels and
trains a balanced Random Forest. Metrics are calculated on a stratified holdout,
then the production model is refitted on all labeled rows.

If the target does not exist, it trains an Isolation Forest. Its percentage is
an anomaly risk relative to the training population, not proof of fraud. For
that reason `predictedFraud` is `null` and the result requires human review.

CSV, XLS and XLSX are accepted. Column names are normalized with the same rules
as the Java service. Missing inference columns are imputed, future columns are
ignored until retraining, and a SHA-256 hash links each model to its dataset.
Every saved artifact also has a `*.metadata.json` manifest containing its
version, metrics, threshold and feature schema for audit or synchronization
with the Java `ml.model_registry` table.

## Commands

From `ml-fraud-py`:

```bash
python -m pip install -r requirements.txt
python -m fraud_detection train --dataset ../fraud_scenario_1.csv
python -m fraud_detection consume
python -m fraud_detection inspect --dataset ../fraud_scenario_1.csv
python -m unittest discover -s tests -v
```

The compatibility scripts remain available:

```bash
python train-model.py --dataset ../fraud_scenario_1.csv
python consumer.py
python consult-column.py --dataset ../fraud_scenario_1.csv
```

## Configuration

| Variable | Default |
| --- | --- |
| `FRAUD_MODEL_PATH` | `artifacts/fraud_model.joblib` |
| `FRAUD_TARGET_ALIASES` | `fraudfound_p,fraud,is_fraud,fraude,isfraud,label` |
| `FRAUD_RANDOM_STATE` | `42` |
| `FRAUD_TEST_SIZE` | `0.25` |
| `FRAUD_ANOMALY_CONTAMINATION` | `0.05` |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` |
| `KAFKA_TRANSACTIONS_TOPIC` | `transactions` |
| `KAFKA_RESULTS_TOPIC` | `fraud-results` |
| `KAFKA_CONSUMER_GROUP` | `ml-fraud-consumer` |
| `KAFKA_MESSAGE_MAX_RETRIES` | `3` |

Messages that cannot be scored after the configured retries are published to
`transactions.DLT`. Results use the contract expected by the Java consumer:
`transactionId`, `realFraud`, `predictedFraud`, `probability`, `scoreType`,
`riskLevel`, `threshold`, `classification`, `modelVersion` and `reasons`.
