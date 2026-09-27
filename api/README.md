# Usinometria API

API responsável pelas regras de negócio, cálculo de estimativas de tempo
de fabricação por usinagem e persistência dos dados de trabalhos.

Parte do projeto **Usinometria — Estimador de Tempo de Usinagem**.

## Stack

- **Java + Spring Boot**
- **PostgreSQL** (via JDBC)
- **Flyway** para versionamento de schema
- **Docker Compose** para orquestração local

## Como funciona

A API calcula o tempo estimado de fabricação de uma peça a partir do
volume de material a ser removido, usando uma taxa empírica derivada do
histórico de trabalhos já concluídos:

```
R = ΣVolume / ΣTempoReal   (taxa global, cm³/h)
T = Volume / R             (tempo estimado, h)
```

A taxa não é armazenada separadamente: os totais são obtidos via
`SUM()` sobre a tabela `job`, evitando duplicação e inconsistência entre
histórico e estatísticas.

Cada trabalho passa pelos estados:

```
IN_PROGRESS → COMPLETED
            → CANCELED
```

## Requisitos

- JDK 21+ (ajustar conforme a versão usada no projeto)
- Docker e Docker Compose
- Maven (ou o wrapper `./mvnw`, se presente)

## Configuração

A aplicação lê a senha do banco pela variável de ambiente `DB_PASSWORD`.

Crie um arquivo `.env` na raiz de `api/` (não versionado) com:

```
DB_PASSWORD=sua_senha_aqui
```

## Rodando localmente com Docker Compose

```bash
cd api
docker compose up -d
```

Isso sobe dois serviços:

| Serviço | Descrição                          | Porta |
|---------|-------------------------------------|-------|
| `db`    | PostgreSQL 16 (alpine)              | 5432  |
| `api`   | API Spring Boot                     | 8080  |

Os dados do PostgreSQL são persistidos no volume `usinometria_db_data`.

## Rodando a API fora do container (dev)

Com o banco já rodando (`docker compose up -d db`), execute:

```bash
./mvnw spring-boot:run
```

A aplicação usa por padrão `jdbc:postgresql://localhost:5432/usinometria`
(ver `application.properties`).

## Migrations

O schema é gerenciado pelo Flyway. As migrations ficam em
`src/main/resources/db/migration` e são aplicadas automaticamente na
inicialização da aplicação (`spring.flyway.enabled=true`).

| Versão | Descrição                              |
|--------|------------------------------------------|
| V001   | Cria a tabela `job` e suas constraints    |
| V002   | Insere o trabalho inicial (seed)          |

> A entidade `Job` é validada contra o schema existente
> (`ddl-auto=validate`) — qualquer alteração de schema deve ser feita
> via nova migration, nunca alterando o mapeamento sem migration
> correspondente.

## Modelo de dados

Tabela `job`:

| Coluna           | Tipo          | Observação                              |
|-------------------|---------------|------------------------------------------|
| `job_id`          | BIGINT        | PK, identity                              |
| `name`            | VARCHAR(150)  |                                            |
| `volume`          | DECIMAL(12,2) | Volume a remover, em cm³                  |
| `estimated_time`  | DECIMAL(7,2)  | Tempo estimado, em horas                  |
| `actual_time`     | DECIMAL(7,2)  | Tempo real, preenchido ao concluir        |
| `status`          | VARCHAR(20)   | `IN_PROGRESS`, `COMPLETED`, `CANCELED`    |
| `created_at`      | TIMESTAMPTZ   |                                            |
| `finished_at`     | TIMESTAMPTZ   | Obrigatório se `COMPLETED`                |
| `canceled_at`     | TIMESTAMPTZ   | Obrigatório se `CANCELED`                 |
| `edited_at`       | TIMESTAMPTZ   | Atualizado automaticamente                |

Constraints garantem consistência entre `status` e as datas
(`finished_at`/`canceled_at`) diretamente no banco.

## Status do projeto

🚧 Em desenvolvimento inicial — ainda sem endpoints expostos
(`JobController` é apenas um placeholder até a primeira funcionalidade
ser implementada).

## Escopo fora do V1

- Geometria da peça e parâmetros de corte/ferramenta como variáveis do
  modelo.
- Análise de erro entre estimado x real (planejada para V2).
- Tabela de estatísticas agregadas separada (os totais seguem via
  `SUM()` enquanto o volume de dados não justificar otimização).