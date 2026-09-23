import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { conhecimentoApi } from '../api/conhecimentoApi';

const CATEGORIAS = [
  { value: '', label: 'Todas as categorias' },
  { value: 'REGRA', label: 'Regras' },
  { value: 'MANUAL', label: 'Manuais' },
  { value: 'TUTORIAL', label: 'Tutoriais' },
  { value: 'ORIENTACAO_CONVIVENCIA', label: 'Orientações de Convivência' },
  { value: 'FAQ', label: 'FAQ' },
];

const CATEGORIA_LABEL = {
  REGRA: 'Regra',
  MANUAL: 'Manual',
  TUTORIAL: 'Tutorial',
  ORIENTACAO_CONVIVENCIA: 'Orientação de Convivência',
  FAQ: 'FAQ',
};

export default function BaseConhecimento() {
  const navigate = useNavigate();
  const [artigos, setArtigos] = useState([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState('');
  const [busca, setBusca] = useState('');
  const [categoriaSelecionada, setCategoriaSelecionada] = useState('');
  const [soPublicados, setSoPublicados] = useState(false);
  const [confirmarExclusao, setConfirmarExclusao] = useState(null);

  useEffect(() => {
    carregarArtigos();
  }, []);

  async function carregarArtigos() {
    try {
      setCarregando(true);
      const res = await conhecimentoApi.listarTodos();
      setArtigos(res.data);
    } catch {
      setErro('Erro ao carregar artigos.');
    } finally {
      setCarregando(false);
    }
  }

  async function confirmarEExcluir() {
    try {
      await conhecimentoApi.excluir(confirmarExclusao.id);
      setArtigos((prev) => prev.filter((a) => a.id !== confirmarExclusao.id));
    } catch {
      setErro('Erro ao excluir artigo.');
    } finally {
      setConfirmarExclusao(null);
    }
  }

  const artigosFiltrados = artigos.filter((a) => {
    const matchBusca = a.titulo.toLowerCase().includes(busca.toLowerCase());
    const matchCategoria = categoriaSelecionada ? a.categoria === categoriaSelecionada : true;
    const matchPublicado = soPublicados ? a.publicado : true;
    return matchBusca && matchCategoria && matchPublicado;
  });

  return (
    <div style={{ padding: '2rem', maxWidth: '900px', margin: '0 auto' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
        <h1 style={{ margin: 0 }}>📚 Base de Conhecimento e FAQ</h1>
        <button
          onClick={() => navigate('/conhecimento/novo')}
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
          + Novo Artigo
        </button>
      </div>

      {/* Filtros */}
      <div style={{ display: 'flex', gap: '1rem', marginBottom: '1.5rem', flexWrap: 'wrap' }}>
        <input
          type="text"
          placeholder="Buscar por título..."
          value={busca}
          onChange={(e) => setBusca(e.target.value)}
          style={{ flex: 1, minWidth: '200px', padding: '0.5rem', borderRadius: '6px', border: '1px solid #d1d5db' }}
        />
        <select
          value={categoriaSelecionada}
          onChange={(e) => setCategoriaSelecionada(e.target.value)}
          style={{ padding: '0.5rem', borderRadius: '6px', border: '1px solid #d1d5db' }}
        >
          {CATEGORIAS.map((c) => (
            <option key={c.value} value={c.value}>{c.label}</option>
          ))}
        </select>
        <label style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', cursor: 'pointer' }}>
          <input
            type="checkbox"
            checked={soPublicados}
            onChange={(e) => setSoPublicados(e.target.checked)}
          />
          Só publicados
        </label>
      </div>

      {carregando && <p>Carregando...</p>}
      {erro && <p style={{ color: 'red' }}>{erro}</p>}

      {!carregando && artigosFiltrados.length === 0 && (
        <p style={{ color: '#6b7280' }}>Nenhum artigo encontrado.</p>
      )}

      <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
        {artigosFiltrados.map((artigo) => (
          <div
            key={artigo.id}
            style={{
              background: '#fff',
              border: '1px solid #e5e7eb',
              borderRadius: '8px',
              padding: '1rem 1.5rem',
              boxShadow: '0 1px 3px rgba(0,0,0,0.05)',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
              <div>
                <span
                  style={{
                    fontSize: '0.75rem',
                    background: '#ede9fe',
                    color: '#5b21b6',
                    borderRadius: '4px',
                    padding: '0.15rem 0.5rem',
                    fontWeight: 600,
                    marginBottom: '0.4rem',
                    display: 'inline-block',
                  }}
                >
                  {CATEGORIA_LABEL[artigo.categoria] || artigo.categoria}
                </span>
                <h3 style={{ margin: '0.3rem 0 0.2rem', fontSize: '1rem' }}>{artigo.titulo}</h3>
                <p style={{ margin: 0, color: '#6b7280', fontSize: '0.85rem' }}>
                  {artigo.autor && <>Por {artigo.autor} · </>}
                  {artigo.publicado ? (
                    <span style={{ color: '#16a34a' }}>Publicado</span>
                  ) : (
                    <span style={{ color: '#dc2626' }}>Rascunho</span>
                  )}
                </p>
              </div>
              <div style={{ display: 'flex', gap: '0.5rem' }}>
                <button
                  onClick={() => navigate(`/conhecimento/${artigo.id}`)}
                  style={{ background: '#e0e7ff', color: '#4f46e5', border: 'none', borderRadius: '5px', padding: '0.35rem 0.8rem', cursor: 'pointer' }}
                >
                  Ver
                </button>
                <button
                  onClick={() => navigate(`/conhecimento/${artigo.id}/editar`)}
                  style={{ background: '#fef9c3', color: '#92400e', border: 'none', borderRadius: '5px', padding: '0.35rem 0.8rem', cursor: 'pointer' }}
                >
                  Editar
                </button>
                <button
                  onClick={() => setConfirmarExclusao(artigo)}
                  style={{ background: '#fee2e2', color: '#b91c1c', border: 'none', borderRadius: '5px', padding: '0.35rem 0.8rem', cursor: 'pointer' }}
                >
                  Excluir
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>

      {confirmarExclusao && (
        <div style={{
          position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.4)',
          display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000,
        }}>
          <div style={{
            background: '#fff', borderRadius: '12px', padding: '2rem',
            maxWidth: '420px', width: '90%', boxShadow: '0 8px 32px rgba(0,0,0,0.18)',
          }}>
            <div style={{ fontSize: '2rem', marginBottom: '0.5rem' }}>🗑️</div>
            <h2 style={{ margin: '0 0 0.5rem', fontSize: '1.1rem', fontWeight: 700 }}>Excluir artigo</h2>
            <p style={{ margin: '0 0 1.5rem', color: '#4b5563' }}>
              Tem certeza que deseja excluir <strong>"{confirmarExclusao.titulo}"</strong>? Essa ação não pode ser desfeita.
            </p>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem' }}>
              <button
                onClick={() => setConfirmarExclusao(null)}
                style={{
                  background: '#f3f4f6', color: '#374151', border: 'none',
                  borderRadius: '6px', padding: '0.5rem 1.2rem', cursor: 'pointer', fontWeight: 600,
                }}
              >
                Cancelar
              </button>
              <button
                onClick={confirmarEExcluir}
                style={{
                  background: '#dc2626', color: '#fff', border: 'none',
                  borderRadius: '6px', padding: '0.5rem 1.2rem', cursor: 'pointer', fontWeight: 600,
                }}
              >
                Sim, excluir
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
