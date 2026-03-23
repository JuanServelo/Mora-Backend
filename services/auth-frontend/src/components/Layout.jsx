import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Layout({ children }) {
  const { usuario } = useAuth();

  return (
    <div className="layout">
      <header className="header">
        <Link to="/" className="logo">Auth</Link>
        <nav>
          {usuario ? (
            <Link to="/dashboard" className="nav-link">Dashboard</Link>
          ) : (
            <>
              <Link to="/" className="nav-link">Entrar</Link>
              <Link to="/registrar" className="nav-link">Registrar</Link>
            </>
          )}
        </nav>
      </header>
      <main className="main">{children}</main>
    </div>
  );
}
