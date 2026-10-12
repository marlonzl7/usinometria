import { AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { describe, expect, it } from 'vitest'
import { ApiError, getErrorMessage, parseFieldErrors, toApiError } from './errors'

function axiosErrorWith(status: number, data: unknown): AxiosError {
  const config = {} as InternalAxiosRequestConfig
  return new AxiosError('Request failed', 'ERR_BAD_REQUEST', config, undefined, {
    status,
    statusText: '',
    headers: {},
    config,
    data,
  })
}

describe('toApiError', () => {
  it('converte o ErrorResponse da API', () => {
    const error = toApiError(
      axiosErrorWith(409, {
        status: 409,
        error: 'UNEXPECTED_STATUS_JOB',
        message: 'Trabalho com status inesperado. Esperava: IN_PROGRESS',
        details: null,
        timestamp: '2026-10-12T05:20:05Z',
      }),
    )

    expect(error.code).toBe('UNEXPECTED_STATUS_JOB')
    expect(error.status).toBe(409)
    expect(error.details).toEqual([])
  })

  it('preserva os details de VALIDATION_ERROR', () => {
    const error = toApiError(
      axiosErrorWith(400, {
        status: 400,
        error: 'VALIDATION_ERROR',
        message: 'Dados inválidos',
        details: ['volume: deve ser maior que zero'],
        timestamp: '2026-10-12T05:20:05Z',
      }),
    )

    expect(error.code).toBe('VALIDATION_ERROR')
    expect(error.details).toEqual(['volume: deve ser maior que zero'])
  })

  it('trata a falta de resposta como NETWORK_ERROR', () => {
    const error = toApiError(new AxiosError('Network Error', 'ERR_NETWORK'))

    expect(error.code).toBe('NETWORK_ERROR')
    expect(error.status).toBeNull()
  })

  it('trata 401 como UNAUTHORIZED', () => {
    const error = toApiError(axiosErrorWith(401, { error: 'QUALQUER_CODIGO' }))

    expect(error.code).toBe('UNAUTHORIZED')
  })

  it('trata 5xx sem corpo ErrorResponse como SERVER_UNAVAILABLE', () => {
    const error = toApiError(axiosErrorWith(502, '<html>Bad Gateway</html>'))

    expect(error.code).toBe('SERVER_UNAVAILABLE')
    expect(error.status).toBe(502)
  })

  it('trata 4xx sem corpo ErrorResponse como UNKNOWN', () => {
    expect(toApiError(axiosErrorWith(418, '')).code).toBe('UNKNOWN')
  })

  it('devolve o mesmo ApiError quando já convertido', () => {
    const original = new ApiError({ code: 'UNKNOWN', status: null, message: 'x' })

    expect(toApiError(original)).toBe(original)
  })

  it('converte erros que não são do axios em UNKNOWN', () => {
    expect(toApiError(new Error('boom')).code).toBe('UNKNOWN')
    expect(toApiError('texto solto').code).toBe('UNKNOWN')
  })
})

describe('getErrorMessage', () => {
  it('usa o texto da UI, não a mensagem da API', () => {
    const error = new ApiError({
      code: 'NO_HISTORY_AVAILABLE',
      status: 422,
      message: 'Não há histórico disponível para gerar uma estimativa',
    })

    expect(getErrorMessage(error)).toContain('trabalhos concluídos')
  })

  it('cai em UNKNOWN para um código que o front não conhece', () => {
    const error = new ApiError({ code: 'CODIGO_NOVO' as never, status: 400, message: '' })

    expect(getErrorMessage(error)).toBe('Ocorreu um erro inesperado.')
  })
})

describe('parseFieldErrors', () => {
  it('separa "campo: mensagem" por campo', () => {
    const parsed = parseFieldErrors([
      'name: é obrigatório',
      'volume: deve ser maior que zero',
    ])

    expect(parsed.fields).toEqual({
      name: 'é obrigatório',
      volume: 'deve ser maior que zero',
    })
    expect(parsed.general).toEqual([])
  })

  it('mantém só a primeira mensagem de cada campo', () => {
    const parsed = parseFieldErrors(['volume: é obrigatório', 'volume: deve ser maior que zero'])

    expect(parsed.fields).toEqual({ volume: 'é obrigatório' })
  })

  it('preserva dois-pontos dentro da mensagem', () => {
    const parsed = parseFieldErrors(['name: use o formato: letras'])

    expect(parsed.fields.name).toBe('use o formato: letras')
  })

  it('envia itens sem o formato esperado para general', () => {
    const parsed = parseFieldErrors(['erro sem campo'])

    expect(parsed.general).toEqual(['erro sem campo'])
  })
})
