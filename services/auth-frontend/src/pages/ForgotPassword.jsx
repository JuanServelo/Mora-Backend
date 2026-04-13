import { useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../services/api';

export default function ForgotPassword() {
  const [email, setEmail] = useState('');
  const [enviado, setEnviado] = useState(false);
  const [erro, setErro] = useState('');
  const [carregando, setCarregando] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErro('');
    setCarregando(true);
    try {
      await api.post('/auth/forgot-password', { email });
      setEnviado(true);
    } catch (err) {
      setErro(err.response?.data?.mensagem || 'Erro ao enviar email');
    } finally {
      setCarregando(false);
    }
  };

  if (enviado) {
    return (
      <div className="auth-page">
        <div className="auth-card">
          <div className="auth-success-icon">✉️</div>
          <h1>Email enviado</h1>
          <p className="subtitle">
            Se esse email estiver cadastrado, você receberá um link para redefinir sua senha em breve.
            Verifique também a caixa de spam.
          </p>
          <p className="link-alt" style={{ marginTop: '1.5rem' }}>
            <Link to="/">Voltar para o login</Link>
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <h1>Recuperar senha</h1>
        <p className="subtitle">
          Digite seu email e enviaremos um link para redefinir sua senha.
        </p>
        <form onSubmit={handleSubmit}>
          <input
            type="email"
            placeholder="Seu email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
          {erro && <p className="erro">{erro}</p>}
          <button type="submit" disabled={carregando}>
            {carregando ? 'Enviando...' : 'Enviar link'}
          </button>
        </form>
        <p className="link-alt">
          Lembrou a senha? <Link to="/">Entrar</Link>
        </p>
      </div>
    </div>
  );
}
