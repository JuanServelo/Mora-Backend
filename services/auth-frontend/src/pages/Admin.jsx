import { useState, useEffect } from 'react';
import api from '../services/api';

const ROLE_LABELS = { admin: 'Admin', user: 'Usuário' };
const PROVIDER_LABELS = { local: 'Local', google: 'Google' };

const emptyForm = { nome: '', email: '', senha: '', role: 'user' };

export default function Admin() {
  const [usuarios, setUsuarios] = useState([]);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState('');
  const [modal, setModal] = useState(null); // null | { mode: 'criar' | 'editar', usuario?: {} }
  const [form, setForm] = useState(emptyForm);
  const [salvando, setSalvando] = useState(false);
  const [formErro, setFormErro] = useState('');

  const carregar = async () => {
    try {
      const { data } = await api.get('/users');
      setUsuarios(data.usuarios);
    } catch (err) {
      setErro(err.response?.data?.mensagem || 'Erro ao carregar usuários');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { carregar(); }, []);

  const abrirCriar = () => {
    setForm(emptyForm);
    setFormErro('');
    setModal({ mode: 'criar' });
  };

  const abrirEditar = (u) => {
    setForm({ nome: u.nome, email: u.email, senha: '', role: u.role });
    setFormErro('');
    setModal({ mode: 'editar', usuario: u });
  };

  const fecharModal = () => setModal(null);

  const salvar = async (e) => {
    e.preventDefault();
    setSalvando(true);
    setFormErro('');
    try {
      const payload = { nome: form.nome, email: form.email, role: form.role };
      if (form.senha) payload.senha = form.senha;

      if (modal.mode === 'criar') {
        if (!form.senha) { setFormErro('Senha é obrigatória'); setSalvando(false); return; }
        payload.senha = form.senha;
        const { data } = await api.post('/users', payload);
        setUsuarios((prev) => [data.usuario, ...prev]);
      } else {
        const { data } = await api.put(`/users/${modal.usuario.id}`, payload);
        setUsuarios((prev) => prev.map((u) => (u._id === modal.usuario.id ? { ...u, ...data.usuario, _id: modal.usuario.id } : u)));
      }
      fecharModal();
    } catch (err) {
      setFormErro(err.response?.data?.mensagem || 'Erro ao salvar');
    } finally {
      setSalvando(false);
    }
  };

  const deletar = async (u) => {
    if (!window.confirm(`Deletar o usuário "${u.nome}"? Esta ação não pode ser desfeita.`)) return;
    try {
      await api.delete(`/users/${u._id}`);
      setUsuarios((prev) => prev.filter((x) => x._id !== u._id));
    } catch (err) {
      alert(err.response?.data?.mensagem || 'Erro ao deletar');
    }
  };

  return (
    <div className="admin-page">
      <div className="admin-header">
        <div>
          <h1 className="admin-title">Gestão de Usuários</h1>
          <p className="admin-subtitle">{usuarios.length} {usuarios.length === 1 ? 'usuário cadastrado' : 'usuários cadastrados'}</p>
        </div>
        <button className="btn-primary" onClick={abrirCriar}>+ Novo Usuário</button>
      </div>

      {erro && <p className="erro">{erro}</p>}

      {loading ? (
        <p className="admin-loading">Carregando...</p>
      ) : (
        <div className="admin-table-wrap">
          <table className="admin-table">
            <thead>
              <tr>
                <th>Nome</th>
                <th>Email</th>
                <th>Perfil</th>
                <th>Acesso</th>
                <th>Cadastro</th>
                <th>Ações</th>
              </tr>
            </thead>
            <tbody>
              {usuarios.map((u) => (
                <tr key={u._id}>
                  <td>
                    <div className="admin-user-cell">
                      <span className="admin-avatar">{u.nome[0].toUpperCase()}</span>
                      {u.nome}
                    </div>
                  </td>
                  <td className="admin-email">{u.email}</td>
                  <td>
                    <span className={`role-badge role-badge--${u.role}`}>
                      {ROLE_LABELS[u.role] ?? u.role}
                    </span>
                  </td>
                  <td className="admin-muted">{PROVIDER_LABELS[u.provider] ?? u.provider}</td>
                  <td className="admin-muted">
                    {new Date(u.createdAt).toLocaleDateString('pt-BR')}
                  </td>
                  <td>
                    <div className="admin-actions">
                      <button className="btn-icon" title="Editar" onClick={() => abrirEditar({ ...u, id: u._id })}>
                        ✏️
                      </button>
                      <button className="btn-icon btn-icon--danger" title="Deletar" onClick={() => deletar(u)}>
                        🗑️
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
              {usuarios.length === 0 && (
                <tr>
                  <td colSpan={6} className="admin-empty">Nenhum usuário encontrado</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}

      {modal && (
        <div className="modal-overlay" onClick={(e) => e.target === e.currentTarget && fecharModal()}>
          <div className="modal">
            <div className="modal-header">
              <h2 className="modal-title">{modal.mode === 'criar' ? 'Novo Usuário' : 'Editar Usuário'}</h2>
              <button className="modal-close" onClick={fecharModal}>✕</button>
            </div>
            <form onSubmit={salvar} className="modal-form">
              <label className="form-label">
                Nome
                <input
                  type="text"
                  className="form-input"
                  value={form.nome}
                  onChange={(e) => setForm((f) => ({ ...f, nome: e.target.value }))}
                  required
                  placeholder="Nome completo"
                />
              </label>
              <label className="form-label">
                Email
                <input
                  type="email"
                  className="form-input"
                  value={form.email}
                  onChange={(e) => setForm((f) => ({ ...f, email: e.target.value }))}
                  required
                  placeholder="email@exemplo.com"
                />
              </label>
              <label className="form-label">
                Senha {modal.mode === 'editar' && <span className="form-hint">(deixe em branco para manter)</span>}
                <input
                  type="password"
                  className="form-input"
                  value={form.senha}
                  onChange={(e) => setForm((f) => ({ ...f, senha: e.target.value }))}
                  placeholder={modal.mode === 'criar' ? 'Mínimo 6 caracteres' : 'Nova senha (opcional)'}
                  minLength={form.senha ? 6 : undefined}
                />
              </label>
              <label className="form-label">
                Perfil
                <select
                  className="form-input"
                  value={form.role}
                  onChange={(e) => setForm((f) => ({ ...f, role: e.target.value }))}
                >
                  <option value="user">Usuário</option>
                  <option value="admin">Admin</option>
                </select>
              </label>
              {formErro && <p className="erro">{formErro}</p>}
              <div className="modal-footer">
                <button type="button" className="btn-secondary" onClick={fecharModal}>Cancelar</button>
                <button type="submit" className="btn-primary" disabled={salvando}>
                  {salvando ? 'Salvando...' : modal.mode === 'criar' ? 'Criar' : 'Salvar'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
