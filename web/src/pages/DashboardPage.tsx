import { useQuery } from '@tanstack/react-query'
import { api } from '../shared/api/client'
import { getErrorMessage, toApiError } from '../shared/api/errors'

// TEMPORÁRIO (fatia 0): confere a ligação front → proxy → API, o cliente axios e
// o QueryClient. Remover na fatia 1, quando entram features/rate (useRate) e o
// card da taxa.
function ConnectionCheck() {
  const { data, error, isPending } = useQuery({
    queryKey: ['connection-check'],
    queryFn: async ({ signal }) => (await api.get('/jobs/rate', { signal })).data,
  })

  return (
    <div className="card">
      <div className="label">Verificação de conexão (temporária) — GET /api/jobs/rate</div>
      {isPending && <p>Carregando…</p>}
      {error && <p role="alert">{getErrorMessage(toApiError(error))}</p>}
      {data && <pre>{JSON.stringify(data, null, 2)}</pre>}
    </div>
  )
}

export function DashboardPage() {
  return (
    <>
      <h1>Dashboard</h1>
      <p className="sub">Taxa, explicação do cálculo e trabalhos em fabricação (fatias 1 e 2).</p>
      <ConnectionCheck />
    </>
  )
}
