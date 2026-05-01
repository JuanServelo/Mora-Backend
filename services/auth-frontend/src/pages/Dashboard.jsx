import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const modules = [
  {
    icon: '🏢',
    title: 'Portaria',
    desc: 'Controle de entrada e saída de visitantes e entregas.',
    href: '#portaria',
    active: true,
  },
  {
    icon: '👥',
    title: 'Moradores',
    desc: 'Gestão de moradores, unidades e cadastros.',
    href: '#moradores',
    active: false,
  },
  {
    icon: '📋',
    title: 'Ocorrências',
    desc: 'Registro e acompanhamento de ocorrências no condomínio.',
    href: '#ocorrencias',
    active: false,
  },
  {
    icon: '💬',
    title: 'Avisos',
    desc: 'Comunicados e avisos para os moradores.',
    href: '#avisos',
    active: false,
  },
];

export default function Dashboard() {
  const { usuario } = useAuth();
  const navigate = useNavigate();

  const primeiroNome = usuario?.nome?.split(' ')[0] ?? 'Morador';

  return (
    <div className="home">
      <section className="home-hero">
        <div className="home-hero-text">
          <span className="home-greeting">Bem-vindo de volta,</span>
          <h1 className="home-title">{primeiroNome}</h1>
          <p className="home-subtitle">{usuario?.email}</p>
        </div>
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '0.5rem' }}>
          <div className="home-avatar">{primeiroNome[0].toUpperCase()}</div>
          <Link to="/perfil" className="perfil-link">Editar perfil</Link>
        </div>
      </section>

      <section className="home-section">
        <h2 className="home-section-title">Módulos</h2>
        <div className="home-grid">
          {modules.map((mod) => (
            <a
              key={mod.title}
              href={mod.href}
              className={`home-card ${!mod.active ? 'home-card--disabled' : ''}`}
            >
              <span className="home-card-icon">{mod.icon}</span>
              <div>
                <p className="home-card-title">
                  {mod.title}
                  {!mod.active && <span className="home-card-badge">Em breve</span>}
                </p>
                <p className="home-card-desc">{mod.desc}</p>
              </div>
            </a>
          ))}
        </div>
      </section>

      {usuario?.role === 'admin' && (
        <section className="home-section">
          <h2 className="home-section-title">Administração</h2>
          <div className="home-grid">
            <button className="home-card home-card--btn" onClick={() => navigate('/admin')}>
              <span className="home-card-icon">⚙️</span>
              <div>
                <p className="home-card-title">Gestão de Usuários</p>
                <p className="home-card-desc">Criar, editar e remover contas de usuários do sistema.</p>
              </div>
            </button>
          </div>
        </section>
      )}
    </div>
  );
}
