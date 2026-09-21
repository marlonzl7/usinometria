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

1. O usuário informa o volume a ser removido de uma nova peça.
2. O sistema calcula o tempo estimado com base na taxa empírica atual:
```
R = ΣVolume / ΣTempoReal (taxa global, cm³/h)
T = Volume / R (tempo estimado, h)
```

3. A peça é fabricada (status `EM_FABRICACAO`).
4. Ao concluir, o usuário informa o tempo real gasto.
5. O trabalho passa para `CONCLUIDO` e entra no histórico, refinando a
   taxa usada nas próximas estimativas.

Trabalhos também podem ser `CANCELADO`.

## Stack

- **Backend:** Spring Boot + PostgreSQL
- **Mobile:** React Native / Expo
- **Comunicação:** REST sobre HTTPS

React (PWA) → HTTP → Spring Boot API → JDBC → PostgreSQL

O app mobile não acessa o banco diretamente; toda regra de negócio,
cálculo de estimativa e persistência ficam na API.

## Escopo do V1

- Cálculo de estimativa a partir de uma taxa empírica global
  (`R = ΣV / ΣT`), sem tabela de estatísticas separada, os totais são
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

🚧 Em desenvolvimento inicial.
