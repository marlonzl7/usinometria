import { QueryClient } from '@tanstack/react-query'
import { ApiError } from '../shared/api/errors'

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      // A taxa e as listas precisam estar atualizadas ao voltar para o app.
      refetchOnWindowFocus: true,
      // Não repete erros 4xx (são determinísticos); tenta de novo rede e 5xx.
      retry: (failureCount, error) => {
        if (error instanceof ApiError && error.status !== null && error.status < 500) {
          return false
        }
        return failureCount < 2
      },
    },
  },
})
