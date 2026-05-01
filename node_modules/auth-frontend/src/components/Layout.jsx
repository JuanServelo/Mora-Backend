import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Layout({ children }) {
  const { usuario, logout } = useAuth();

  return (
    <div className="layout">
      <header className="header">
        <Link to={usuario ? '/dashboard' : '/'} className="logo">Mora</Link>
        <nav>
          {usuario ? (
            <div className="header-user">
              <span className="header-user-name">{usuario.nome.split(' ')[0]}</span>
              <button className="btn-logout-header" onClick={logout}>Sair</button>
            </div>
          ) : (
            <>
              <Link to="/" className="nav-link">Entrar</Link>
              <Link to="/registrar" className="nav-link">Registrar</Link>
            </>
          )}
        </nav>
      </header>
      <main className={usuario ? 'main main--home' : 'main'}>{children}</main>
    </div>
  );
}
