import { NavLink, useLocation } from 'react-router-dom'

const NAV = [
  {
    label: 'Geral',
    items: [
      { to: '/',            icon: '◉', label: 'Dashboard' },
      { to: '/moradores',   icon: '⌂', label: 'Moradores' },
      { to: '/funcionarios',icon: '👤', label: 'Funcionários' },
    ],
  },
  {
    label: 'Controle de Acesso',
    items: [
      { to: '/visitantes', icon: '🚶', label: 'Visitantes' },
      { to: '/carros',     icon: '🚗', label: 'Veículos' },
    ],
  },
  {
    label: 'Operações',
    items: [
      { to: '/chaves',   icon: '🔑', label: 'Chaves' },
      { to: '/entregas', icon: '📦', label: 'Entregas' },
      { to: '/turnos',   icon: '🕐', label: 'Turnos' },
    ],
  },
]

export default function Sidebar() {
  const location = useLocation()

  return (
    <aside className="sidebar">
      <div className="sidebar-logo">
        <span>Mora</span>
        <span className="badge">Portaria</span>
      </div>

      <nav className="sidebar-nav">
        {NAV.map((section) => (
          <div key={section.label}>
            <div className="nav-section-label">{section.label}</div>
            {section.items.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.to === '/'}
                className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
              >
                <span className="icon">{item.icon}</span>
                {item.label}
              </NavLink>
            ))}
          </div>
        ))}
      </nav>

      <div className="sidebar-footer">
        <div className="avatar">P</div>
        <div className="info">
          <div className="name">Portaria</div>
          <div className="role">Sistema de Controle</div>
        </div>
      </div>
    </aside>
  )
}
