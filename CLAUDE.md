# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Visão geral

TCC (UNIP) de detecção de fraude em sinistros de seguro automotivo. Três módulos independentes se comunicam via Kafka e PostgreSQL:

- `tcc/` — API Spring Boot 4 (Java 21, Maven): recebe upload de datasets (CSV/XLS/XLSX), processa com Spring Batch, persiste em camadas medallion (bronze/silver/gold), expõe dashboard, consulta de sinistros e revisão humana.
- `ml-fraud-py/` — serviço Python (pacote `fraud_detection`) que treina o modelo e faz inferência consumindo o Kafka.
- `fraud-dashboard/` — frontend Angular 17 (standalone, com rotas) que consome a API.

## Versionamento e branches

- Remoto: `origin` = github.com/MizeraviDoSertao/TCC_UNIP. Branches: `main` (estável) e `homolog` (homologação, espelho de `main` + alterações em teste).
- Todo trabalho é feito e commitado na `homolog`. Ao terminar cada alteração, prepare o commit (mensagem em pt-BR) na `homolog`.
- **Antes de qualquer `git push`, pergunte ao usuário**: (1) se já pode subir as alterações para `origin/homolog` e (2) se ele também quer que elas vão para a `main`. Só faça o merge/push na `main` com um "sim" explícito para a segunda pergunta.
- `plano-completo-ate-12-11.md` é um arquivo local de planejamento e **não deve ser versionado** em nenhuma branch (está em `.git/info/exclude`).
- Não versionar `.idea/`, `.DS_Store`, `fraud-dashboard/dist/` nem artefatos de modelo (`ml-fraud-py/artifacts/*.joblib`); já estão nos `.gitignore`.

## Comandos

Infra (Postgres 16 em 5432, Kafka KRaft em 9092) — na raiz:

```bash
docker compose up -d
```

API Java (em `tcc/`):

```bash
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
./mvnw clean package
./mvnw test                     # testes em src/test (JUnit)
./mvnw test -Dtest=ImportDatasetServiceTest   # um teste específico
```

ML Python (em `ml-fraud-py/`; Python >= 3.11; rodar de dentro da pasta, pois `FRAUD_MODEL_PATH` é relativo):

```bash
python -m pip install -r requirements.txt
python -m fraud_detection train --dataset <arquivo.csv|xlsx>   # gera artifacts/fraud_model.joblib + .metadata.json
python -m fraud_detection consume                              # consome `transactions`, publica em `fraud-results`
python -m fraud_detection inspect --dataset <arquivo>
python -m unittest discover -s tests -v
```

`train-model.py`, `consumer.py` e `consult-column.py` são apenas atalhos para a CLI acima.

Dashboard (em `fraud-dashboard/`):

```bash
npm install
npm start                       # ng serve com proxy.conf.json → http://localhost:4200
npm run build
```

## Configuração que costuma quebrar

- A API usa `server.servlet.context-path: /process_fraud_automotive`; todos os endpoints têm esse prefixo. O proxy do Angular encaminha o prefixo para `localhost:8080`.
- O schema do banco é gerenciado pelo **Flyway** (`tcc/src/main/resources/db/migration`) com `ddl-auto: validate`: mudar uma entity exige uma nova migration `V<n>__*.sql`, senão a API não sobe. As tabelas ficam nos schemas `bronze`, `silver`, `gold`, `ops`, `review` e `ml` (`V5` migra as tabelas antigas do `public`).
- O CSV de exemplo (`fraud_scenario_1.csv`) foi removido do repositório, mas o `ml-fraud-py/README.md` ainda o referencia; é preciso ter um dataset próprio para treinar e importar.
- Sem modelo treinado em `ml-fraud-py/artifacts/`, o consumer Python não tem o que aplicar: treine antes de rodar `consume`.
- Variáveis úteis: `DB_URL`/`DB_USERNAME`/`DB_PASSWORD`, `KAFKA_BOOTSTRAP_SERVERS`, `IMPORT_STORAGE_PATH` (onde os uploads são guardados; default no tmp do sistema), `IMPORT_TARGET_ALIASES` / `FRAUD_TARGET_ALIASES` (nomes aceitos para a coluna de rótulo de fraude).

## Arquitetura e fluxo de dados

1. **Upload**: `POST /imports` (`ImportController` → `ImportDatasetService`) valida extensão/tamanho, guarda o arquivo (`LocalImportFileStorageAdapter`), deduplica pelo SHA-256 e cria um `ImportJob` em `ops.import_job`, disparando o job do Spring Batch (`ImportJobLauncherAdapter`). `POST /imports/{id}/retry` reinicia um job que falhou.
2. **Spring Batch** (`config/batch/DatasetBatchConfiguration`, chunks de 100): `DynamicDatasetItemReader` escolhe a estratégia por extensão (`CsvDatasetReaderStrategy` / `ExcelDatasetReaderStrategy`); `DynamicDatasetProcessor` normaliza nomes de colunas (sem acento, minúsculas, não alfanumérico → `_`) e valores (booleanos, datas, moeda BR, números), identifica a coluna de rótulo pelos aliases e gera um `transactionId` determinístico (UUID a partir de importId+aba+linha). As colunas **não são fixas**: todas viram `features`.
3. `DatasetBatchWriter` grava cada linha crua na **bronze**, registra o schema detectado (`bronze.dataset_schema`), salva a aceita na **silver** e enfileira no **outbox** (`ops.transaction_outbox`); linhas com erro vão para `ops.rejected_record`. `ImportJobBatchListener` atualiza o status do import.
4. `TransactionOutboxPublisher` (scheduler, a cada `outbox.publish-delay-ms`) publica os eventos pendentes no tópico `transactions` via `TransactionKafkaProducer`.
5. **Python** (`fraud_detection/infrastructure/kafka.py`) consome `transactions`, aplica o pipeline salvo e publica em `fraud-results` (retries com commit manual; falhas vão para `transactions.DLT`). Com rótulo no dataset de treino usa Random Forest balanceado; sem rótulo usa Isolation Forest (aí `predictedFraud` é `null` e exige revisão humana).
6. `FraudResultKafkaConsumer` (Java) consome `fraud-results` e grava na **gold** (`gold.fraud_prediction`), normalizando probabilidade (0–1 ou 0–100) e derivando `riskLevel` quando ausente.
7. **Consulta/revisão**: `DashboardController` (`/dashboard/summary`, `/dashboard/results` com filtros, `/dashboard/results/{id}`), `ClaimController` (`/claims`, `/claims/{id}`, `POST /claims/{id}/reviews` grava em `review.fraud_review` e atualiza o rótulo confirmado em silver e gold), `ImportInspectionController` (`/imports/{id}/errors`, `/imports/{id}/schema`).

**Contrato Java ↔ Python:** a mensagem de resultado tem `transactionId`, `realFraud`, `predictedFraud`, `probability`, `scoreType`, `riskLevel`, `threshold`, `classification`, `modelVersion`, `reasons` (`FraudScoringMessage` no Java). A normalização de nomes de colunas é igual nos dois lados (`DynamicDatasetProcessor.normalizeName` e `fraud_detection/normalization.py`); mudar uma exige mudar a outra. Tópicos criados em `KafkaTopicConfig` (3 partições cada).

### Arquitetura hexagonal (`tcc/src/main/java/com/unip/fraud`)

- `application/domain`: records de domínio; `application/port/in`: casos de uso; `application/port/out`: portas de saída (repositórios, storage, launcher, outbox, producer); `application/service`: implementações dos casos de uso.
- `adapter/in`: controllers REST, leitura/processamento do Batch, consumer Kafka e scheduler do outbox. `adapter/out`: producer Kafka, launcher do Batch, storage local e persistência (`entity`, `repository` Spring Data, `mapper` MapStruct, `specification`, `adapter`).
- Injeção por construtor com parâmetros `final`, indentação de 2 espaços (também no Python). Jackson 3 (`tools.jackson.databind.ObjectMapper`), não `com.fasterxml`.

### ML Python (`ml-fraud-py/fraud_detection`)

Também hexagonal: `domain.py`/`ports.py`, `application/` (treino e scoring), `infrastructure/` (dataset, Kafka, repositório de modelos Joblib com manifesto `*.metadata.json`), `training/` (estratégias supervisionada e de anomalia + factory). Configuração por variáveis de ambiente em `config.py` (ver `ml-fraud-py/README.md`).
