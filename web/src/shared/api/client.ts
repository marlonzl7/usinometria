import axios from 'axios'
import { toApiError } from './errors'

/**
 * Cliente HTTP único da aplicação. Em dev, o proxy do Vite e, em produção, o
 * Traefik removem o prefixo /api antes de chegar à API.
 */
export const api = axios.create({
  baseURL: '/api',
  timeout: 15_000,
})

type UnauthorizedHandler = () => void
let unauthorizedHandler: UnauthorizedHandler | null = null

/** Ponto reservado para o tratamento do 401 (API Key, fatia 7). */
export function setUnauthorizedHandler(handler: UnauthorizedHandler | null): void {
  unauthorizedHandler = handler
}

api.interceptors.request.use((config) => {
  // Reservado: enviar o header X-API-Key quando a API Key existir (fatia 7).
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    // Requisições canceladas (TanStack Query) não são erros de API.
    if (axios.isCancel(error)) return Promise.reject(error)

    const apiError = toApiError(error)
    if (apiError.code === 'UNAUTHORIZED') unauthorizedHandler?.()
    return Promise.reject(apiError)
  },
)
