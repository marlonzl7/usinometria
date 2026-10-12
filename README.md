# Usinometria - Estimador de Tempo de Usinagem

Aplicação para estimar o tempo total de fabricação de peças usinadas com
base em dados históricos reais, usando um modelo empírico simples que é
continuamente refinado à medida que novos trabalhos são concluídos.

## Motivação

Em vez de tentar modelar com precisão os parâmetros físicos da usinagem
(avanço, rotação, profundidade de corte etc.), o sistema parte de uma
premissa simples: **o volume de material removido é o principal preditor
do tempo de fabricação**, e essa relação pode ser aprendida a partir do
histórico de trabalhos já realizados.

O objetivo não é uma fórmula teoricamente perfeita, e sim um sistema que
possa ser validado, medido e refinado continuamente com dados reais.

## Como funciona

1. O usuário informa o nome da peça e o volume a ser removido (em **mm³**).
2. O sistema calcula o tempo estimado com base na taxa empírica atual:

```
R = ΣVolume / ΣTempoReal   (taxa global, mm³/h)
T = Volume / R             (tempo estimado, h)
```

3. A peça é fabricada (status `IN_PROGRESS`).
4. Ao concluir, o usuário informa o tempo real gasto.
5. O trabalho passa para `COMPLETED` e entra no histórico, refinando a
   taxa usada nas próximas estimativas.

Trabalhos também podem ser cancelados (`CANCELED`) e, depois de
concluídos, editados. Apenas trabalhos `COMPLETED` entram no cálculo da taxa.

## Stack

- **Backend:** Java 21 + Spring Boot + PostgreSQL + Flyway
- **Frontend:** React (PWA) com Vite
- **Comunicação:** REST sobre HTTPS

```
React (PWA) → HTTP → Spring Boot API → JDBC → PostgreSQL
```

O frontend não acessa o banco diretamente; toda regra de negócio,
cálculo de estimativa e persistência ficam na API.

## Estrutura do repositório

| Pasta  | Conteúdo                                                        |
|--------|------------------------------------------------------------------|
| `api/` | API Spring Boot, migrations, Docker Compose. [Como rodar](api/README.md) |
| `web/` | Frontend React + Vite. [Como rodar](web/README.md)               |

## Início rápido (desenvolvimento)

```bash
cd api
cp .env.example .env        # defina DB_PASSWORD
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d db
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Swagger (somente dev): http://localhost:8080/swagger-ui.html

Detalhes de variáveis de ambiente, ambientes (dev/prod) e testes em
[`api/README.md`](api/README.md).

## Escopo do V1

- Cálculo de estimativa a partir de uma taxa empírica global
  (`R = ΣV / ΣT`), sem tabela de estatísticas separada: os totais são
  obtidos via `SUM()` sobre a tabela de trabalhos.
- Registro manual do tempo real ao concluir um trabalho (sem
  cronometragem automática por timestamps de início/fim).
- Cancelamento e edição de trabalhos concluídos.
- Tela inicial focada nos trabalhos em aberto, com a taxa atual e uma
  explicação de como a estimativa é calculada.
- Material fixo (aço carbono), não é uma variável do modelo ainda.

Fora do escopo do V1: geometria da peça, parâmetros de corte/ferramenta,
análise de erro entre estimado x real (planejada para V2).

## Status

🚧 Em desenvolvimento. Endpoints do V1 implementados na API; frontend
ainda em fase inicial.