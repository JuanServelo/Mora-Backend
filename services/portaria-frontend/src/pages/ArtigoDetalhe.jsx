import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { conhecimentoApi } from '../api/conhecimentoApi';

const CATEGORIA_LABEL = {
  REGRA: 'Regra',
  MANUAL: 'Manual',
  TUTORIAL: 'Tutorial',
  ORIENTACAO_CONVIVENCIA: 'Orientação de Convivência',
  FAQ: 'FAQ',
};

export default function ArtigoDetalhe() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [artigo, setArtigo] = useState(null);
  const [erro, setErro] = useState('');

  useEffect(() => {
    conhecimentoApi
      .buscarPorId(id)
      .then((res) => setArtigo(res.data))
      .catch(() => setErro('Artigo não encontrado.'));
  }, [id]);

  if (erro) return <p style={{ padding: '2rem', color: 'red' }}>{erro}</p>;
  if (!artigo) return <p style={{ padding: '2rem' }}>Carregando...</p>;

  return (
    <div style={{ padding: '2rem', maxWidth: '800px', margin: '0 auto' }}>
      <button
        onClick={() => navigate('/conhecimento')}
        style={{ background: 'none', border: 'none', color: '#4f46e5', cursor: 'pointer', marginBottom: '1rem', fontSize: '0.9rem' }}
      >
        ← Voltar
      </button>

      <span
        style={{
          fontSize: '0.8rem',
          background: '#ede9fe',
          color: '#5b21b6',
          borderRadius: '4px',
          padding: '0.2rem 0.6rem',
          fontWeight: 600,
        }}
      >
        {CATEGORIA_LABEL[artigo.categoria] || artigo.categoria}
      </span>

      <h1 style={{ margin: '0.75rem 0 0.5rem' }}>{artigo.titulo}</h1>

      <p style={{ color: '#6b7280', fontSize: '0.85rem', marginBottom: '2rem' }}>
        {artigo.autor && <>Por <strong>{artigo.autor}</strong> · </>}
        {artigo.publicado ? (
          <span style={{ color: '#16a34a' }}>Publicado</span>
        ) : (
          <span style={{ color: '#dc2626' }}>Rascunho</span>
        )}
        {' · '}
        Criado em {new Date(artigo.criadoEm).toLocaleDateString('pt-BR')}
      </p>

      <div
        style={{
          background: '#fff',
          border: '1px solid #e5e7eb',
          borderRadius: '8px',
          padding: '1.5rem',
          lineHeight: '1.7',
          whiteSpace: 'pre-wrap',
        }}
      >
        {artigo.conteudo}
      </div>

      <div style={{ display: 'flex', gap: '1rem', marginTop: '1.5rem' }}>
        <button
          onClick={() => navigate(`/conhecimento/${artigo.id}/editar`)}
          style={{
            background: '#4f46e5',
            color: '#fff',
            border: 'none',
            borderRadius: '6px',
            padding: '0.5rem 1.2rem',
            cursor: 'pointer',
            fontWeight: 600,
          }}
        >
          Editar
        </button>
      </div>
    </div>
  );
}
