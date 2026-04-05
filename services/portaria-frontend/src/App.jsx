import { Routes, Route, useLocation } from 'react-router-dom'
import Sidebar from './components/Sidebar.jsx'
import Dashboard    from './pages/Dashboard.jsx'
import Moradores    from './pages/Moradores.jsx'
import Funcionarios from './pages/Funcionarios.jsx'
import Visitantes   from './pages/Visitantes.jsx'
import Carros       from './pages/Carros.jsx'
import Chaves       from './pages/Chaves.jsx'
import Entregas     from './pages/Entregas.jsx'
import Turnos       from './pages/Turnos.jsx'

const TITLES = {
  '/':             'Dashboard',
  '/moradores':    'Moradores',
  '/funcionarios': 'Funcionários',
  '/visitantes':   'Visitantes',
  '/carros':       'Veículos',
  '/chaves':       'Chaves',
  '/entregas':     'Entregas',
  '/turnos':       'Turnos',
}

export default function App() {
  const { pathname } = useLocation()
  const title = TITLES[pathname] ?? 'Portaria'

  return (
    <div className="app-shell">
      <Sidebar />
      <div className="main-content">
        <header className="topbar">
          <span className="topbar-title">{title}</span>
          <div className="topbar-right">
            <div style={{ display:'flex', alignItems:'center', gap:6 }}>
              <span className="online-dot" />
              <span style={{ fontSize:'0.75rem', color:'var(--text-muted)' }}>Sistema online</span>
            </div>
          </div>
        </header>

        <main className="page">
          <Routes>
            <Route path="/"             element={<Dashboard />} />
            <Route path="/moradores"    element={<Moradores />} />
            <Route path="/funcionarios" element={<Funcionarios />} />
            <Route path="/visitantes"   element={<Visitantes />} />
            <Route path="/carros"       element={<Carros />} />
            <Route path="/chaves"       element={<Chaves />} />
            <Route path="/entregas"     element={<Entregas />} />
            <Route path="/turnos"       element={<Turnos />} />
          </Routes>
        </main>
      </div>
    </div>
  )
}
