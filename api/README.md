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
R = ΣVolume / ΣTempoReal   (taxa global, mm³/h)
T = Volume / R             (tempo estimado, h)
```

A taxa não é armazenada separadamente: os totais são obtidos via
`SUM()` sobre a tabela `job` (apenas trabalhos `COMPLETED`), evitando
duplicação e inconsistência entre histórico e estatísticas. Trabalhos
`CANCELED` e `IN_PROGRESS` nunca entram no cálculo.

Cada trabalho passa pelos estados:

```
IN_PROGRESS → COMPLETED
            → CANCELED
```

## Unidades

- Volume: **mm³**
- Tempo (estimado e real): **horas** (decimal)

## Endpoints

| Método | Rota                    | Descrição                                          |
|--------|-------------------------|----------------------------------------------------|
| POST   | `/jobs`                 | Cria trabalho (`IN_PROGRESS`) e calcula a estimativa |
| GET    | `/jobs?status=`         | Lista trabalhos (mais recentes primeiro)           |
| GET    | `/jobs/{id}`            | Busca um trabalho                                  |
| GET    | `/jobs/rate`            | Taxa atual e quantidade de trabalhos que a sustentam |
| PATCH  | `/jobs/{id}/complete`   | Registra o tempo real e conclui                    |
| PATCH  | `/jobs/{id}/cancel`     | Cancela um trabalho em fabricação                  |
| PATCH  | `/jobs/{id}`            | Edita (parcialmente) um trabalho concluído         |

`difference` (retornado ao concluir) = `actualTime - estimatedTime`.
Positivo indica que o trabalho demorou mais que o estimado (resultado ruim);
negativo indica que terminou antes.

### Formato de erro

Sucesso retorna o objeto direto (sem envelope). Erros seguem:

```json
{
  "status": 409,
  "error": "UNEXPECTED_STATUS_JOB",
  "message": "Trabalho com status inesperado. Esperava: IN_PROGRESS",
  "details": null,
  "timestamp": "2026-09-27T14:10:00Z"
}
```

`details` só é preenchido em erros de validação de campo
(`["volume: deve ser maior que zero"]`). `error` é um código estável
para o frontend tratar programaticamente.

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

## Rodando os testes

A suíte de testes do `JobController` (`JobControllerTest`) não depende de
banco — usa o `JobService` mockado e roda isoladamente.

O teste de contexto (`ApiApplicationTests`) sobe a aplicação Spring Boot
completa, incluindo datasource e Flyway, então precisa do banco disponível
e das variáveis de ambiente carregadas no shell antes de rodar:

```bash
cd api
docker compose up -d db
export $(grep -v '^#' .env | xargs)
mvn test
```

> `mvn spring-boot:run` lê variáveis via `-Dspring-boot.run.profiles=local`,
> mas `mvn test` não carrega o `.env` automaticamente — por isso o `export`
> é necessário aqui também.

**Nota:** isso cria um acoplamento entre o teste de contexto e o ambiente
local (banco rodando, variáveis carregadas). Um perfil de teste dedicado
(H2 ou Testcontainers) eliminaria essa dependência e é um candidato natural
para quando o projeto configurar CI — por ora, mantido simples.

## Migrations

O schema é gerenciado pelo Flyway. As migrations ficam em
`src/main/resources/db/migration` e são aplicadas automaticamente na
inicialização da aplicação (`spring.flyway.enabled=true`).

| Versão | Descrição                                   |
|--------|----------------------------------------------|
| V001   | Cria a tabela `job` e suas constraints        |
| V002   | Insere o trabalho inicial (seed)              |
| V003   | Renomeia a coluna `job_id` para `id`          |
| V004   | Corrige o volume do seed inicial (0,77 cm³ = 770 mm³) |

> A entidade `Job` é validada contra o schema existente
> (`ddl-auto=validate`) — qualquer alteração de schema deve ser feita
> via nova migration, nunca alterando o mapeamento sem migration
> correspondente.

## Modelo de dados

Tabela `job`:

| Coluna           | Tipo          | Observação                                     |
|-------------------|---------------|-------------------------------------------------|
| `id`              | BIGINT        | PK, identity                                     |
| `name`            | VARCHAR(150)  |                                                   |
| `volume`          | DECIMAL(12,2) | Volume a remover, em mm³                         |
| `estimated_time`  | DECIMAL(7,2)  | Tempo estimado, em horas                         |
| `actual_time`     | DECIMAL(7,2)  | Tempo real, preenchido ao concluir               |
| `status`          | VARCHAR(20)   | `IN_PROGRESS`, `COMPLETED`, `CANCELED`           |
| `created_at`      | TIMESTAMPTZ   |                                                   |
| `finished_at`     | TIMESTAMPTZ   | Obrigatório se `COMPLETED`                       |
| `canceled_at`     | TIMESTAMPTZ   | Obrigatório se `CANCELED`                        |
| `edited_at`       | TIMESTAMPTZ   | Preenchido apenas quando um trabalho concluído é editado |

Constraints garantem consistência entre `status` e as datas
(`finished_at`/`canceled_at`) diretamente no banco.

## Status do projeto

🚧 Em desenvolvimento — endpoints do V1 implementados; frontend (React/PWA)
ainda não iniciado.

## Escopo fora do V1

- Geometria da peça e parâmetros de corte/ferramenta como variáveis do
  modelo.
- Análise de erro entre estimado x real (planejada para V2).
- Tabela de estatísticas agregadas separada (os totais seguem via
  `SUM()` enquanto o volume de dados não justificar otimização).
