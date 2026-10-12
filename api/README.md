# Usinometria API

API responsável pelas regras de negócio, cálculo de estimativas de tempo
de fabricação por usinagem e persistência dos dados de trabalhos.

Parte do projeto **Usinometria — Estimador de Tempo de Usinagem**
([visão geral](../README.md)).

## Stack

- **Java 21 + Spring Boot 4**
- **PostgreSQL 16** (via JDBC)
- **Flyway** para versionamento de schema
- **springdoc-openapi** para documentação (Swagger UI), apenas em dev
- **Docker Compose** para orquestração

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

```
IN_PROGRESS → COMPLETED
            → CANCELED
```

## Unidades

- Volume: **mm³**
- Tempo (estimado e real): **horas** (decimal)
- Taxa: **mm³/h**

## Requisitos

- JDK 21
- Docker e Docker Compose
- Maven (ou o wrapper `./mvnw`)

## Configuração (variáveis de ambiente)

| Variável                 | Obrigatória | Padrão                                      | Descrição                                   |
|--------------------------|-------------|---------------------------------------------|---------------------------------------------|
| `DB_PASSWORD`            | **Sim**     | —                                           | Senha do PostgreSQL                         |
| `DB_URL`                 | Não         | `jdbc:postgresql://localhost:5432/usinometria` | URL JDBC (o compose define para `db:5432`)  |
| `DB_USER`                | Não         | `usinometria_app`                           | Usuário do banco                            |
| `SPRING_PROFILES_ACTIVE` | Não         | (nenhum = configuração segura de prod)      | `dev` habilita Swagger e logs de SQL        |

**Onde ficam os arquivos:** ambos em `api/` (ao lado do `docker-compose.yml`).

| Arquivo        | Versionado | Função                                                  |
|----------------|------------|----------------------------------------------------------|
| `.env.example` | Sim        | Modelo, sem valores reais                                |
| `.env`         | **Não**    | Valores reais. Crie com `cp .env.example .env`           |

O mesmo `.env` serve ao Docker Compose (lido automaticamente) e ao
Spring Boot rodando fora do container (via
`spring.config.import=optional:file:.env[.properties]`, relativo à pasta
de onde a aplicação é iniciada, ou seja, `api/`). Use senhas diferentes
em dev e prod; o `.env` de produção existe apenas no servidor.

## Ambientes

| Aspecto              | Dev                                         | Prod                                   |
|----------------------|---------------------------------------------|----------------------------------------|
| Profile Spring       | `dev`                                       | nenhum (padrão seguro)                 |
| Como roda            | Banco no Docker, API via `./mvnw`           | `docker compose up -d --build` (db + api) |
| Porta do banco       | `127.0.0.1:5432` (via `docker-compose.dev.yml`) | não publicada                      |
| Porta da API         | 8080                                        | `127.0.0.1:8080` (acesso externo pelo tunnel/proxy) |
| Swagger / OpenAPI    | Habilitado                                  | Desabilitado                           |
| Actuator             | `health`, `info`                            | apenas `health`, sem detalhes          |
| SQL no log           | Sim                                         | Não                                    |
| `.env`               | Local, senha de dev                         | No servidor, senha de prod             |

## Rodando em desenvolvimento

```bash
cd api
cp .env.example .env     # edite e defina DB_PASSWORD
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d db
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI (JSON): http://localhost:8080/v3/api-docs
- Health: http://localhost:8080/actuator/health

> Não use `docker-compose.override.yml` para a porta do banco: ele seria
> carregado automaticamente e poderia expor o Postgres em produção.

## Rodando em produção (Docker Compose)

```bash
cd api
cp .env.example .env     # no servidor; defina uma senha forte
docker compose up -d --build
docker compose logs -f api
```

Sobe `db` (PostgreSQL 16, sem porta publicada) e `api` (publicada apenas
em `127.0.0.1:8080`). Os dados ficam no volume `usinometria_db_data`.
Para expor a API na internet, use um túnel/proxy reverso (por exemplo
Cloudflare Tunnel) apontando para essa porta.

## Rodando os testes

`JobControllerTest` não depende de banco (usa o `JobService` mockado).

`ApiApplicationTests` sobe o contexto completo (datasource e Flyway) e
precisa do banco disponível. Como o `.env` é lido pela própria aplicação,
não é mais necessário `export`:

```bash
cd api
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d db
./mvnw test
```

Um perfil de teste dedicado (Testcontainers) é um candidato natural para
quando o projeto configurar CI.

## Endpoints

A documentação interativa (parâmetros, exemplos e respostas) fica no
Swagger UI em dev. Resumo:

| Método | Rota                    | Descrição                                            |
|--------|-------------------------|------------------------------------------------------|
| POST   | `/jobs`                 | Cria trabalho (`IN_PROGRESS`) e calcula a estimativa |
| GET    | `/jobs?status=`         | Lista trabalhos (mais recentes primeiro)             |
| GET    | `/jobs/{id}`            | Busca um trabalho                                    |
| GET    | `/jobs/rate`            | Taxa atual e quantidade de trabalhos que a sustentam |
| PATCH  | `/jobs/{id}/complete`   | Registra o tempo real e conclui                      |
| PATCH  | `/jobs/{id}/cancel`     | Cancela um trabalho em fabricação                    |
| PATCH  | `/jobs/{id}`            | Edita (parcialmente) um trabalho concluído           |

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

## Migrations

O schema é gerenciado pelo Flyway. As migrations ficam em
`src/main/resources/db/migration` e são aplicadas automaticamente na
inicialização (`spring.flyway.enabled=true`).

| Versão | Descrição                                              |
|--------|---------------------------------------------------------|
| V001   | Cria a tabela `job` e suas constraints                  |
| V002   | Insere o trabalho inicial (seed)                        |
| V003   | Renomeia a coluna `job_id` para `id`                    |
| V004   | Corrige o volume do seed inicial (0,77 cm³ = 770 mm³)   |

> A entidade `Job` é validada contra o schema (`ddl-auto=validate`).
> Qualquer alteração de schema deve ser feita via nova migration.

## Modelo de dados

Tabela `job`:

| Coluna           | Tipo          | Observação                                     |
|------------------|---------------|------------------------------------------------|
| `id`             | BIGINT        | PK, identity                                   |
| `name`           | VARCHAR(150)  |                                                |
| `volume`         | DECIMAL(12,2) | Volume a remover, em mm³                       |
| `estimated_time` | DECIMAL(7,2)  | Tempo estimado, em horas                       |
| `actual_time`    | DECIMAL(7,2)  | Tempo real, preenchido ao concluir             |
| `status`         | VARCHAR(20)   | `IN_PROGRESS`, `COMPLETED`, `CANCELED`         |
| `created_at`     | TIMESTAMPTZ   |                                                |
| `finished_at`    | TIMESTAMPTZ   | Obrigatório se `COMPLETED`                     |
| `canceled_at`    | TIMESTAMPTZ   | Obrigatório se `CANCELED`                      |
| `edited_at`      | TIMESTAMPTZ   | Preenchido apenas quando um concluído é editado |

Constraints garantem a consistência entre `status` e as datas
diretamente no banco.

## Segurança

Atualmente a API **não possui autenticação**. Antes de expô-la na
internet, configure a API Key (planejada) ou proteja o hostname com
Cloudflare Access.

## Status do projeto

🚧 Em desenvolvimento. Endpoints do V1 implementados; frontend (React/PWA)
em fase inicial.

## Escopo fora do V1

- Geometria da peça e parâmetros de corte/ferramenta como variáveis do modelo.
- Análise de erro entre estimado x real (planejada para V2).
- Tabela de estatísticas agregadas separada (os totais seguem via
  `SUM()` enquanto o volume de dados não justificar otimização).