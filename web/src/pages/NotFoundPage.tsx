import { Link } from 'react-router-dom'

export function NotFoundPage() {
  return (
    <>
      <h1>Página não encontrada</h1>
      <p className="sub">
        <Link to="/">Voltar ao Dashboard</Link>
      </p>
    </>
  )
}
