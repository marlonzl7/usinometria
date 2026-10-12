import { NavLink, Outlet } from 'react-router-dom'

const links = [
  { to: '/', label: 'Dashboard', icon: '🏠', end: true },
  { to: '/trabalhos', label: 'Trabalhos', icon: '🧾', end: false },
  { to: '/historico', label: 'Histórico', icon: '📜', end: false },
]

export function Layout() {
  return (
    <div className="app">
      <nav className="sidebar" aria-label="Navegação principal">
        <div className="brand">⚙️ Usinometria</div>
        {links.map((link) => (
          <NavLink
            key={link.to}
            to={link.to}
            end={link.end}
            className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}
          >
            <span aria-hidden="true">{link.icon}</span>
            {link.label}
          </NavLink>
        ))}
      </nav>
      <main className="content">
        <Outlet />
      </main>
    </div>
  )
}
