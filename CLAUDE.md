# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Visão geral

TCC (UNIP) de detecção de fraude em sinistros de seguro automotivo. Três módulos independentes se comunicam via Kafka e PostgreSQL:

- `tcc/` — API Spring Boot 4 (Java 21, Maven): ingere CSVs, persiste em camadas medallion (bronze/silver/gold) e expõe o dashboard REST.
- `ml-fraud-py/` — consumidor Python que aplica um RandomForest (scikit-learn) às transações.
- `fraud-dashboard/` — frontend Angular 17 (standalone) que consome a API.

## Versionamento e branches

- Remoto: `origin` = github.com/MizeraviDoSertao/TCC_UNIP. Branches: `main` (estável) e `homolog` (homologação, espelho de `main` + alterações em teste).
- Todo trabalho é feito e commitado na `homolog`. Ao terminar cada alteração, prepare o commit (mensagem em pt-BR) na `homolog`.
- **Antes de qualquer `git push`, pergunte ao usuário**: (1) se já pode subir as alterações para `origin/homolog` e (2) se ele também quer que elas vão para a `main`. Só faça o merge/push na `main` com um "sim" explícito para a segunda pergunta.
- `plano-completo-ate-12-11.md` é um arquivo local de planejamento e **não deve ser versionado** em nenhuma branch (está em `.git/info/exclude`).

## Comandos

Infra (Postgres 16 em 5432, Kafka KRaft em 9092) — na raiz:

```bash
docker compose up -d
```

API Java (em `tcc/`):

```bash
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
./mvnw clean package
```

Não há testes automatizados no projeto (não existe `src/test`).

ML Python (em `ml-fraud-py/`; rodar de dentro da pasta, pois os caminhos dos `.pkl` são relativos):

```bash
pip install -r requirements.txt
python consumer.py              # consome `transactions`, publica em `fraud-results`
python train-model.py           # re-treina e regrava fraud_model.pkl / model_columns.pkl
```

Dashboard (em `fraud-dashboard/`):

```bash
npm install
npm start                       # ng serve com proxy.conf.json → http://localhost:4200
npm run build
```

## Configuração que costuma quebrar

- `dataset.folder-path` (env `DATASET_PATH`) em `application.yml` tem como default um caminho macOS de outro desenvolvedor; defina `DATASET_PATH` apontando para a pasta com os CSVs (ex.: a raiz do repo, que contém `fraud_scenario_1.csv`).
- `train-model.py` e `consult-column.py` também têm caminhos absolutos hardcoded (`/Users/pablo/...`) para o CSV.
- A API usa `server.servlet.context-path: /process_fraud_automotive`, então os endpoints são `/process_fraud_automotive/dashboard/summary` e `/process_fraud_automotive/dashboard/results`. O proxy do Angular encaminha esse prefixo para `localhost:8080` (o `fraud-dashboard/README.md` está desatualizado nesse ponto).
- O schema do banco é gerado pelo Hibernate (`ddl-auto: update`); não há migrations.

## Arquitetura e fluxo de dados

1. `DatasetScheduler` roda **uma única vez**, 5 s após o boot (`fixedDelay = Long.MAX_VALUE`), chamando `ProcessDatasetUseCase`. Para reprocessar, reinicie a aplicação.
2. `DatasetProcessingAdapter`: `DatasetFileReaderAdapter` lê todo `*.csv` da pasta configurada (split simples por vírgula, sem suporte a aspas). Cada linha:
   - é salva crua na **bronze**;
   - `ParseUtils.parse` normaliza os cabeçalhos (minúsculas, espaço/hífen → `_`) e `TransactionMapper` (MapStruct) monta a `Transaction`: UUID novo, `realFraud` a partir da coluna `fraudfound_p` e um mapa `features` com um subconjunto fixo de colunas (Month, WeekOfMonth, DayOfWeek, Make, AccidentArea, Sex, Age, Fault, VehiclePrice);
   - é salva na **silver** e publicada no tópico Kafka `transactions` (`TransactionKafkaProducer`);
   - qualquer exceção manda a linha para a tabela de **rejeitados** com a mensagem de erro.
3. `ml-fraud-py/consumer.py` consome `transactions`, aplica `pd.get_dummies` + `reindex(model_columns)` sobre `features` e publica `{transactionId, realFraud, predictedFraud, probability, classification}` em `fraud-results`.
4. `FraudResultKafkaConsumer` (Java) consome `fraud-results` e grava na **gold** (`processedAt = now`).
5. `DashboardController` expõe `/dashboard/summary` (contagens por camada, fraudes reais vs. previstas) e `/dashboard/results` (paginado, filtros via JPA Specification em `GoldPersistenceAdapter`). Detalhes do fluxo de consulta: `tcc/FLUXO_ANALISE.md`.

**Contrato entre Java e Python:** as chaves de `features` montadas em `TransactionMapper.buildFeatures` precisam corresponder às colunas originais do CSV usadas no treino (`train-model.py` usa todas as colunas exceto `FraudFound_P`; colunas ausentes viram 0 no `reindex`). Mudar as features de um lado exige revisar o outro e, em geral, re-treinar o modelo. Os tópicos são criados em `KafkaTopicConfig` (3 partições cada).

### Arquitetura hexagonal (`tcc/src/main/java/com/unip/fraud`)

- `application/domain`: records de domínio; `application/port/in`: casos de uso; `application/port/out`: portas de saída (repositórios, leitor de dataset, producer).
- Os casos de uso são implementados por classes na raiz de `adapter/` (`DatasetProcessingAdapter`, `DashboardAdapter`, `FraudResultAdapter`, `DatasetFileReaderAdapter`); não há pacote `service`.
- `adapter/in`: controller REST, scheduler e o producer Kafka. `adapter/out`: consumer Kafka e persistência (`entity`, `repository` Spring Data e `adapter`, que converte entity ↔ domínio). A nomenclatura in/out dos adapters Kafka está invertida em relação ao usual (o producer fica em `in`, o consumer em `out`).
- Injeção por construtor com parâmetros `final`, indentação de 2 espaços. Jackson 3 (`tools.jackson.databind.ObjectMapper`), não `com.fasterxml`.
