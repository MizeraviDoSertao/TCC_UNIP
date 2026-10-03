# Fraud Insight — frontend Angular

Interface operacional para importação, inspeção e análise de sinistros processados
pelo backend Java e pelo modelo Python.

## Funcionalidades

- Dashboard com indicadores Bronze, Silver, Gold e rejeições.
- Distribuição por nível de risco e percentual previsto como fraude.
- Filtros por decisão da IA, rótulo confirmado, score, risco, período e modelo.
- Importação de arquivos CSV, XLS e XLSX.
- Acompanhamento do Spring Batch com atualização automática.
- Consulta de linhas rejeitadas.
- Exploração de sinistros com colunas dinâmicas descobertas pelo backend.
- Detalhe pesquisável com todos os atributos originais normalizados.
- Tratamento explícito para datasets sem coluna de fraude.

## Organização

```text
src/app
├── claims/               # exploração de dados da camada Silver
├── imports/              # upload e acompanhamento do Spring Batch
├── transaction-detail/   # análise individual Silver + Gold
├── dashboard.service.ts  # acesso HTTP ao dashboard
├── app.component.*       # página de visão geral
└── app.routes.ts         # rotas lazy-loaded
```

Os componentes coordenam estado de tela, enquanto os serviços concentram os
contratos HTTP. Modelos são estritamente tipados e imutáveis. As rotas usam lazy
loading para manter o bundle inicial pequeno.

## Executar

Com o backend disponível em `http://localhost:8080`:

```bash
npm install
npm start
```

Acesse `http://localhost:4200`. O proxy encaminha chamadas iniciadas por
`/process_fraud_automotive` para o backend.

## Validar

```bash
npm run build
```
