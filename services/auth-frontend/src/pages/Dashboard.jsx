import { useAuth } from '../context/AuthContext';
import api from '../services/api';
import { useState } from 'react';

export default function Dashboard() {
  const { usuario, logout } = useAuth();
  const [msgProtegida, setMsgProtegida] = useState('');

  const testarRotaProtegida = async () => {
    try {
      const { data } = await api.get('/auth/me');
      setMsgProtegida(data.sucesso ? `Olá, ${data.usuario.nome}! Token válido.` : 'Erro');
    } catch (err) {
      setMsgProtegida('Erro: ' + (err.response?.data?.mensagem || 'Falha na requisição'));
    }
  };

  return (
    <div className="dashboard">
      <div className="dashboard-card">
        <h1>Dashboard</h1>
        <p className="welcome">Bem-vindo, <strong>{usuario?.nome}</strong>!</p>
        <p className="email">{usuario?.email}</p>
        <button onClick={testarRotaProtegida} className="btn-testar">
          Testar rota protegida /api/auth/me
        </button>
        {msgProtegida && <p className="msg-protegida">{msgProtegida}</p>}
        <button onClick={logout} className="btn-logout">
          Sair
        </button>
      </div>
    </div>
  );
}
