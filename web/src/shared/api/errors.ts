import axios from 'axios'

/** Códigos `error` documentados no ErrorResponse da API. */
export type ApiErrorCode =
  | 'INVALID_PARAMETER'
  | 'VALIDATION_ERROR'
  | 'MALFORMED_REQUEST'
  | 'UNSUPPORTED_MEDIA_TYPE'
  | 'NO_HISTORY_AVAILABLE'
  | 'ESTIMATE_OUT_OF_RANGE'
  | 'RESOURCE_NOT_FOUND'
  | 'UNEXPECTED_STATUS_JOB'
  | 'EMPTY_UPDATE_REQUEST'
  | 'METHOD_NOT_ALLOWED'
  | 'ROUTE_NOT_FOUND'
  | 'INTERNAL_SERVER_ERROR'

/** Códigos gerados pelo próprio frontend (a API não os envia). */
export type ClientErrorCode =
  | 'NETWORK_ERROR' // sem resposta (offline, timeout)
  | 'SERVER_UNAVAILABLE' // 5xx sem corpo ErrorResponse (proxy/Traefik, API fora do ar)
  | 'UNAUTHORIZED' // 401 (reservado para a API Key, fatia 7)
  | 'UNKNOWN'

export type ErrorCode = ApiErrorCode | ClientErrorCode

/** Formato de erro devolvido pela API (schema ErrorResponse). */
export interface ErrorResponseBody {
  status: number
  error: string
  message: string
  details: string[] | null
  timestamp: string
}

interface ApiErrorInit {
  code: ErrorCode
  status: number | null
  message: string
  details?: string[]
}

export class ApiError extends Error {
  readonly code: ErrorCode
  /** Status HTTP; `null` quando não houve resposta. */
  readonly status: number | null
  /** Itens "campo: mensagem" de VALIDATION_ERROR. */
  readonly details: string[]

  constructor(init: ApiErrorInit) {
    super(init.message)
    this.name = 'ApiError'
    this.code = init.code
    this.status = init.status
    this.details = init.details ?? []
  }
}

function isErrorResponseBody(data: unknown): data is ErrorResponseBody {
  return (
    typeof data === 'object' &&
    data !== null &&
    typeof (data as { error?: unknown }).error === 'string'
  )
}

/** Converte qualquer erro lançado por uma chamada HTTP em ApiError. */
export function toApiError(error: unknown): ApiError {
  if (error instanceof ApiError) return error

  if (axios.isAxiosError(error)) {
    const response = error.response

    if (!response) {
      return new ApiError({ code: 'NETWORK_ERROR', status: null, message: error.message })
    }

    const { status, data } = response

    if (status === 401) {
      return new ApiError({ code: 'UNAUTHORIZED', status, message: 'Não autorizado' })
    }

    if (isErrorResponseBody(data)) {
      return new ApiError({
        code: data.error as ErrorCode,
        status,
        message: data.message ?? '',
        details: Array.isArray(data.details) ? data.details : [],
      })
    }

    // Sem corpo ErrorResponse: a API sempre responde com ele, então um 5xx
    // assim veio de um intermediário (proxy/Traefik) ou a API está fora do ar.
    return new ApiError({
      code: status >= 500 ? 'SERVER_UNAVAILABLE' : 'UNKNOWN',
      status,
      message: error.message,
    })
  }

  return new ApiError({
    code: 'UNKNOWN',
    status: null,
    message: error instanceof Error ? error.message : 'Erro desconhecido',
  })
}

const MESSAGES: Record<ErrorCode, string> = {
  INVALID_PARAMETER: 'Valor inválido na requisição.',
  VALIDATION_ERROR: 'Alguns campos estão inválidos. Corrija e tente novamente.',
  MALFORMED_REQUEST: 'Não foi possível enviar os dados. Tente novamente.',
  UNSUPPORTED_MEDIA_TYPE: 'Formato de envio não suportado.',
  NO_HISTORY_AVAILABLE:
    'Ainda não há trabalhos concluídos para calcular a taxa e gerar uma estimativa.',
  ESTIMATE_OUT_OF_RANGE: 'A estimativa excede o limite suportado. Verifique o volume informado.',
  RESOURCE_NOT_FOUND: 'Trabalho não encontrado.',
  UNEXPECTED_STATUS_JOB: 'O status deste trabalho mudou. Atualize a lista e tente novamente.',
  EMPTY_UPDATE_REQUEST: 'Altere ao menos um campo antes de salvar.',
  METHOD_NOT_ALLOWED: 'Operação indisponível.',
  ROUTE_NOT_FOUND: 'Operação indisponível.',
  INTERNAL_SERVER_ERROR: 'Erro interno no servidor. Tente novamente em instantes.',
  NETWORK_ERROR: 'Sem conexão com o servidor. Verifique sua internet.',
  SERVER_UNAVAILABLE: 'Servidor indisponível no momento. Tente novamente em instantes.',
  UNAUTHORIZED: 'Acesso não autorizado.',
  UNKNOWN: 'Ocorreu um erro inesperado.',
}

/** Texto da UI para o erro. Nunca exibe o `message` cru da API. */
export function getErrorMessage(error: ApiError): string {
  return MESSAGES[error.code] ?? MESSAGES.UNKNOWN
}

export interface ParsedDetails {
  /** Primeira mensagem de cada campo, ex.: { volume: "deve ser maior que zero" }. */
  fields: Record<string, string>
  /** Itens sem o formato "campo: mensagem". */
  general: string[]
}

/** Separa os itens "campo: mensagem" de `details` por campo. */
export function parseFieldErrors(details: string[]): ParsedDetails {
  const fields: Record<string, string> = {}
  const general: string[] = []

  for (const item of details) {
    const separator = item.indexOf(': ')
    if (separator <= 0) {
      general.push(item)
      continue
    }
    const field = item.slice(0, separator)
    if (!(field in fields)) {
      fields[field] = item.slice(separator + 2)
    }
  }

  return { fields, general }
}
