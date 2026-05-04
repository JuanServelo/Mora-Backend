import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { conhecimentoApi } from '../api/conhecimentoApi';

const CATEGORIAS = [
  { value: 'REGRA', label: 'Regra' },
  { value: 'MANUAL', label: 'Manual' },
  { value: 'TUTORIAL', label: 'Tutorial' },
  { value: 'ORIENTACAO_CONVIVENCIA', label: 'Orientação de Convivência' },
  { value: 'FAQ', label: 'FAQ' },
];

const FORM_INICIAL = {
  titulo: '',
  conteudo: '',
  categoria: 'FAQ',
  autor: '',
  publicado: false,
};

export default function ArtigoForm() {
  const navigate = useNavigate();
  const { id } = useParams();
  const isEdicao = Boolean(id);

  const [form, setForm] = useState(FORM_INICIAL);
  const [carregando, setCarregando] = useState(false);
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState('');

  useEffect(() => {
    if (isEdicao) {
      setCarregando(true);
      conhecimentoApi
        .buscarPorId(id)
        .then((res) => setForm(res.data))
        .catch(() => setErro('Erro ao carregar artigo.'))
        .finally(() => setCarregando(false));
    }
  }, [id, isEdicao]);

  function handleChange(e) {
    const { name, value, type, checked } = e.target;
    setForm((prev) => ({ ...prev, [name]: type === 'checkbox' ? checked : value }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setErro('');
    setSalvando(true);
    try {
      if (isEdicao) {
        await conhecimentoApi.atualizar(id, form);
      } else {
        await conhecimentoApi.criar(form);
      }
      navigate('/conhecimento');
    } catch (err) {
      const mensagem = err.response?.data?.erro || err.response?.data?.erros
        ? JSON.stringify(err.response.data.erros)
        : 'Erro ao salvar artigo.';
      setErro(mensagem);
    } finally {
      setSalvando(false);
    }
  }

  if (carregando) return <p style={{ padding: '2rem' }}>Carregando...</p>;

  return (
    <div style={{ padding: '2rem', maxWidth: '700px', margin: '0 auto' }}>
      <button
        onClick={() => navigate('/conhecimento')}
        style={{ background: 'none', border: 'none', color: '#4f46e5', cursor: 'pointer', marginBottom: '1rem', fontSize: '0.9rem' }}
      >
        ← Voltar
      </button>

      <h1 style={{ marginBottom: '1.5rem' }}>
        {isEdicao ? '✏️ Editar Artigo' : '📝 Novo Artigo'}
      </h1>

      {erro && (
        <div style={{ background: '#fee2e2', color: '#b91c1c', padding: '0.75rem 1rem', borderRadius: '6px', marginBottom: '1rem' }}>
          {erro}
        </div>
      )}

      <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
        <div>
          <label style={{ display: 'block', marginBottom: '0.3rem', fontWeight: 600 }}>Título *</label>
          <input
            name="titulo"
            value={form.titulo}
            onChange={handleChange}
            required
            style={{ width: '100%', padding: '0.5rem', borderRadius: '6px', border: '1px solid #d1d5db', boxSizing: 'border-box' }}
          />
        </div>

        <div>
          <label style={{ display: 'block', marginBottom: '0.3rem', fontWeight: 600 }}>Categoria *</label>
          <select
            name="categoria"
            value={form.categoria}
            onChange={handleChange}
            required
            style={{ width: '100%', padding: '0.5rem', borderRadius: '6px', border: '1px solid #d1d5db' }}
          >
            {CATEGORIAS.map((c) => (
              <option key={c.value} value={c.value}>{c.label}</option>
            ))}
          </select>
        </div>

        <div>
          <label style={{ display: 'block', marginBottom: '0.3rem', fontWeight: 600 }}>Autor</label>
          <input
            name="autor"
            value={form.autor}
            onChange={handleChange}
            placeholder="Nome do autor (opcional)"
            style={{ width: '100%', padding: '0.5rem', borderRadius: '6px', border: '1px solid #d1d5db', boxSizing: 'border-box' }}
          />
        </div>

        <div>
          <label style={{ display: 'block', marginBottom: '0.3rem', fontWeight: 600 }}>Conteúdo *</label>
          <textarea
            name="conteudo"
            value={form.conteudo}
            onChange={handleChange}
            required
            rows={12}
            placeholder="Escreva o conteúdo do artigo..."
            style={{ width: '100%', padding: '0.5rem', borderRadius: '6px', border: '1px solid #d1d5db', boxSizing: 'border-box', resize: 'vertical', fontFamily: 'inherit' }}
          />
        </div>

        <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer' }}>
          <input
            type="checkbox"
            name="publicado"
            checked={form.publicado}
            onChange={handleChange}
          />
          <span>Publicar artigo (visível para todos)</span>
        </label>

        <div style={{ display: 'flex', gap: '1rem', marginTop: '0.5rem' }}>
          <button
            type="submit"
            disabled={salvando}
            style={{
              background: '#4f46e5',
              color: '#fff',
              border: 'none',
              borderRadius: '6px',
              padding: '0.6rem 1.5rem',
              cursor: salvando ? 'not-allowed' : 'pointer',
              fontWeight: 600,
              opacity: salvando ? 0.7 : 1,
            }}
          >
            {salvando ? 'Salvando...' : isEdicao ? 'Salvar alterações' : 'Criar artigo'}
          </button>
          <button
            type="button"
            onClick={() => navigate('/conhecimento')}
            style={{ background: '#f3f4f6', border: 'none', borderRadius: '6px', padding: '0.6rem 1.5rem', cursor: 'pointer' }}
          >
            Cancelar
          </button>
        </div>
      </form>
    </div>
  );
}
