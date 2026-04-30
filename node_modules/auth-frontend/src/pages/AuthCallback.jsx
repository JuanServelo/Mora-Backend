import { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';

export default function AuthCallback() {
  const [searchParams] = useSearchParams();
  const { login } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    const token = searchParams.get('token');
    const erro = searchParams.get('erro');
    console.log('[AuthCallback] URL params:', { token: token ? token.slice(0, 20) + '...' : null, erro });
    console.log('[AuthCallback] Full URL:', window.location.href);

    if (!token) {
      console.warn('[AuthCallback] Nenhum token na URL — redirecionando para login');
      navigate('/?erro=oauth');
      return;
    }

    api.defaults.headers.common['Authorization'] = `Bearer ${token}`;
    console.log('[AuthCallback] Token recebido, buscando dados do usuário...');

    api
      .get('/auth/me')
      .then(({ data }) => {
        console.log('[AuthCallback] Resposta /auth/me:', data);
        if (data.sucesso) {
          login(token, data.usuario);
          console.log('[AuthCallback] Login OK, redirecionando para /dashboard');
          navigate('/dashboard');
        } else {
          console.warn('[AuthCallback] sucesso=false, redirecionando para login');
          navigate('/?erro=oauth');
        }
      })
      .catch((err) => {
        console.error('[AuthCallback] Erro ao buscar /auth/me:', err.response?.status, err.response?.data);
        navigate('/?erro=oauth');
      });
  }, []);

  return <div className="loading">Autenticando com Google...</div>;
}
