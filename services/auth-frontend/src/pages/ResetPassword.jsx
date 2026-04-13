import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import api from '../services/api';

export default function ResetPassword() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const token = searchParams.get('token');

  const [form, setForm] = useState({ senha: '', confirmar: '' });
  const [erro, setErro] = useState('');
  const [carregando, setCarregando] = useState(false);
  const [sucesso, setSucesso] = useState(false);

  if (!token) {
    return (
      <div className="auth-page">
        <div className="auth-card">
          <h1>Link inválido</h1>
          <p className="subtitle">Este link de recuperação é inválido ou expirou.</p>
          <p className="link-alt" style={{ marginTop: '1.5rem' }}>
            <Link to="/esqueci-senha">Solicitar novo link</Link>
          </p>
        </div>
      </div>
    );
  }

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErro('');

    if (form.senha !== form.confirmar) {
      setErro('As senhas não coincidem');
      return;
    }

    setCarregando(true);
    try {
      await api.post(`/auth/reset-password/${token}`, { senha: form.senha });
      setSucesso(true);
      setTimeout(() => navigate('/'), 2500);
    } catch (err) {
      setErro(err.response?.data?.mensagem || 'Erro ao redefinir senha');
    } finally {
      setCarregando(false);
    }
  };

  if (sucesso) {
    return (
      <div className="auth-page">
        <div className="auth-card">
          <div className="auth-success-icon">✅</div>
          <h1>Senha redefinida</h1>
          <p className="subtitle">Sua senha foi alterada com sucesso. Redirecionando para o login...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <h1>Nova senha</h1>
        <p className="subtitle">Digite e confirme sua nova senha.</p>
        <form onSubmit={handleSubmit}>
          <input
            type="password"
            placeholder="Nova senha (mín. 6 caracteres)"
            value={form.senha}
            onChange={(e) => setForm({ ...form, senha: e.target.value })}
            minLength={6}
            required
          />
          <input
            type="password"
            placeholder="Confirmar nova senha"
            value={form.confirmar}
            onChange={(e) => setForm({ ...form, confirmar: e.target.value })}
            minLength={6}
            required
          />
          {erro && <p className="erro">{erro}</p>}
          <button type="submit" disabled={carregando}>
            {carregando ? 'Salvando...' : 'Redefinir senha'}
          </button>
        </form>
      </div>
    </div>
  );
}
