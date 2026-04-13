import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';

export default function EditarPerfil() {
  const { usuario, atualizarUsuario } = useAuth();
  const navigate = useNavigate();

  const [dados, setDados] = useState({ nome: usuario?.nome || '', email: usuario?.email || '' });
  const [senhas, setSenhas] = useState({ nova: '', confirmar: '' });
  const [erros, setErros] = useState({});
  const [sucesso, setSucesso] = useState('');
  const [carregando, setCarregando] = useState(false);

  const validar = () => {
    const e = {};
    if (!dados.nome.trim()) e.nome = 'Nome é obrigatório';
    if (!dados.email.trim()) e.email = 'Email é obrigatório';
    if (senhas.nova && senhas.nova.length < 6) e.nova = 'Senha deve ter no mínimo 6 caracteres';
    if (senhas.nova && senhas.nova !== senhas.confirmar) e.confirmar = 'As senhas não coincidem';
    return e;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSucesso('');
    const errosValidacao = validar();
    if (Object.keys(errosValidacao).length > 0) {
      setErros(errosValidacao);
      return;
    }
    setErros({});
    setCarregando(true);

    const payload = { nome: dados.nome, email: dados.email };
    if (senhas.nova) payload.senha = senhas.nova;

    try {
      const { data } = await api.put('/auth/me', payload);
      if (data.sucesso) {
        atualizarUsuario(data.usuario);
        setSucesso('Perfil atualizado com sucesso!');
        setSenhas({ nova: '', confirmar: '' });
      }
    } catch (err) {
      setErros({ geral: err.response?.data?.mensagem || 'Erro ao atualizar perfil' });
    } finally {
      setCarregando(false);
    }
  };

  const primeiroNome = usuario?.nome?.split(' ')[0] ?? '';

  return (
    <div className="home">
      <section className="home-hero">
        <div className="home-hero-text">
          <span className="home-greeting">Editar perfil</span>
          <h1 className="home-title">{primeiroNome}</h1>
          <p className="home-subtitle">{usuario?.email}</p>
        </div>
        <div className="home-avatar">{primeiroNome[0]?.toUpperCase()}</div>
      </section>

      <div className="perfil-grid">
        <div className="perfil-card">
          <h2 className="perfil-section-title">Dados pessoais</h2>
          <form onSubmit={handleSubmit} className="perfil-form">
            <label className="form-label">
              Nome
              <input
                className="form-input"
                type="text"
                value={dados.nome}
                onChange={(e) => setDados({ ...dados, nome: e.target.value })}
                placeholder="Seu nome completo"
              />
              {erros.nome && <span className="erro">{erros.nome}</span>}
            </label>

            <label className="form-label">
              Email
              <input
                className="form-input"
                type="email"
                value={dados.email}
                onChange={(e) => setDados({ ...dados, email: e.target.value })}
                placeholder="Seu email"
                disabled={usuario?.provider === 'google'}
              />
              {usuario?.provider === 'google' && (
                <span className="form-hint" style={{ color: 'var(--text-muted)' }}>
                  Email vinculado ao Google não pode ser alterado aqui.
                </span>
              )}
              {erros.email && <span className="erro">{erros.email}</span>}
            </label>

            <div className="perfil-divider" />

            <h3 className="perfil-section-subtitle">Alterar senha</h3>
            <p className="perfil-hint">Deixe em branco para manter a senha atual.</p>

            <label className="form-label">
              Nova senha
              <input
                className="form-input"
                type="password"
                value={senhas.nova}
                onChange={(e) => setSenhas({ ...senhas, nova: e.target.value })}
                placeholder="Mínimo 6 caracteres"
              />
              {erros.nova && <span className="erro">{erros.nova}</span>}
            </label>

            <label className="form-label">
              Confirmar nova senha
              <input
                className="form-input"
                type="password"
                value={senhas.confirmar}
                onChange={(e) => setSenhas({ ...senhas, confirmar: e.target.value })}
                placeholder="Repita a nova senha"
              />
              {erros.confirmar && <span className="erro">{erros.confirmar}</span>}
            </label>

            {erros.geral && <p className="erro">{erros.geral}</p>}
            {sucesso && <p className="sucesso">{sucesso}</p>}

            <div className="perfil-actions">
              <button type="button" className="btn-secondary" onClick={() => navigate('/dashboard')}>
                Cancelar
              </button>
              <button type="submit" className="btn-primary" disabled={carregando}>
                {carregando ? 'Salvando...' : 'Salvar alterações'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
