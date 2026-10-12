# Usinometria — Web (PWA)

Frontend do estimador de tempo de usinagem. React + Vite + TypeScript.

## Rodando em desenvolvimento

```bash
cd web
npm install
npm run dev
```

Requer a API rodando em `http://localhost:8080` (ver `api/README.md`). O proxy do Vite
encaminha `/api/*` para a API removendo o prefixo, como o Traefik fará em produção;
o front chama sempre `/api/...`, sem CORS.

## Scripts

| Comando | O que faz |
|---|---|
| `npm run dev` | Servidor de desenvolvimento (service worker desligado) |
| `npm run build` | Checagem de tipos + build de produção (gera manifest e service worker) |
| `npm run preview` | Serve o build em `localhost:4173`, com o mesmo proxy `/api`; use para testar o PWA |
| `npm test` | Vitest (funções puras) |
| `npm run lint` | oxlint |

## Estrutura

```
src/
  app/        roteador, layout com navegação, QueryClient, estilos
  pages/      Dashboard, Trabalhos, Histórico (compõem as features)
  features/   uma pasta por funcionalidade: View (componente), ViewModel (hook), Model (API)
  shared/     cliente axios, erros, utilitários, componentes base
```

Regras: `features` não importam umas das outras; `shared` não importa `features`.

## Decisões de arquitetura

- **Mesma origem** (Traefik): front em `/`, API em `/api`.
- **TanStack Query** para dados da API, encapsulado em hooks por funcionalidade.
- **PWA** online-only: o service worker guarda só o shell do app; respostas da API nunca são
  cacheadas e `/api` fica fora do fallback de navegação.
- Erros da API são normalizados em `ApiError` (`shared/api/errors.ts`); a UI usa o mapa de
  textos, nunca o `message` cru da API.
- Volume em mm³; a API usa horas decimais e a interface mostra hh:mm.
