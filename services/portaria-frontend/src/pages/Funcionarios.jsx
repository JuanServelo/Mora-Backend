import { useEffect, useState, useCallback } from 'react'
import { api } from '../api/portariaApi'
import Modal from '../components/Modal'
import Toast from '../components/Toast'

const initForm = { nome:'', cpf:'', cargo:'', matricula:'', telefone:'' }

function initials(name='') {
  return name.split(' ').slice(0,2).map(w=>w[0]).join('').toUpperCase()
}

export default function Funcionarios() {
  const [lista, setLista]       = useState([])
  const [search, setSearch]     = useState('')
  const [tab, setTab]           = useState('ativos')
  const [modal, setModal]       = useState(null)
  const [selected, setSelected] = useState(null)
  const [form, setForm]         = useState(initForm)
  const [loading, setLoading]   = useState(false)
  const [toast, setToast]       = useState(null)

  const load = useCallback(async () => {
    try {
      const data = tab === 'todos' ? await api.funcionarios.listarTodos() : await api.funcionarios.listar()
      setLista(data)
    } catch (e) { setToast({ message: e.message, type:'error' }) }
  }, [tab])

  useEffect(() => { load() }, [load])

  const filtered = lista.filter(f => {
    const q = search.toLowerCase()
    return f.nome.toLowerCase().includes(q) || f.cargo?.toLowerCase().includes(q) || f.matricula?.toLowerCase().includes(q)
  })

  async function handleSubmit(e) {
    e.preventDefault(); setLoading(true)
    try {
      if (modal === 'edit') {
        await api.funcionarios.atualizar(selected.id, form)
        setToast({ message: 'Funcionário atualizado!', type:'success' })
      } else {
        await api.funcionarios.cadastrar(form)
        setToast({ message: 'Funcionário cadastrado!', type:'success' })
      }
      setModal(null); setForm(initForm); load()
    } catch (e) { setToast({ message: e.message, type:'error' }) }
    setLoading(false)
  }

  async function handleDesativar(id) {
    if (!confirm('Desativar funcionário?')) return
    try {
      await api.funcionarios.desativar(id)
      setToast({ message: 'Funcionário desativado.', type:'success' })
      setModal(null); load()
    } catch (e) { setToast({ message: e.message, type:'error' }) }
  }

  function openEdit(f) {
    setSelected(f)
    setForm({ nome:f.nome, cpf:f.cpf, cargo:f.cargo, matricula:f.matricula||'', telefone:f.telefone||'' })
    setModal('edit')
  }

  const F = (k) => (e) => setForm(f => ({ ...f, [k]: e.target.value }))

  const cargos = [...new Set(lista.map(f => f.cargo).filter(Boolean))]

  return (
    <div>
      <div className="page-header">
        <h1>Funcionários</h1>
        <p>Cadastro de funcionários e equipe do condomínio</p>
      </div>

      <div className="toolbar">
        <div className="search-bar" style={{ flex:1, maxWidth:380 }}>
          <span>🔍</span>
          <input placeholder="Buscar por nome, cargo ou matrícula..." value={search} onChange={e => setSearch(e.target.value)} />
        </div>
        <div className="tabs">
          <button className={`tab ${tab==='ativos'?'active':''}`} onClick={() => setTab('ativos')}>Ativos</button>
          <button className={`tab ${tab==='todos'?'active':''}`}  onClick={() => setTab('todos')}>Todos</button>
        </div>
        <div className="spacer" />
        <button className="btn btn-primary" onClick={() => { setForm(initForm); setModal('add') }}>
          + Cadastrar
        </button>
      </div>

      {cargos.length > 0 && (
        <div style={{ display:'flex', gap:8, flexWrap:'wrap', marginBottom:18 }}>
          {cargos.map(c => (
            <span key={c} className="badge badge-blue">{c}</span>
          ))}
        </div>
      )}

      <div className="card" style={{ padding:0 }}>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Funcionário</th>
                <th>Cargo</th>
                <th>Matrícula</th>
                <th>Telefone</th>
                <th>Status</th>
                <th>Ações</th>
              </tr>
            </thead>
            <tbody>
              {filtered.length === 0 ? (
                <tr><td colSpan={6} style={{ textAlign:'center', padding:32, color:'var(--text-muted)' }}>
                  {search ? 'Nenhum resultado encontrado.' : 'Nenhum funcionário cadastrado.'}
                </td></tr>
              ) : filtered.map(f => (
                <tr key={f.id}>
                  <td>
                    <div style={{ display:'flex', alignItems:'center', gap:10 }}>
                      <div style={{ width:34, height:34, borderRadius:'50%', background:'var(--bg-3)', display:'flex', alignItems:'center', justifyContent:'center', fontWeight:700, fontSize:'0.78rem', flexShrink:0 }}>
                        {initials(f.nome)}
                      </div>
                      <div>
                        <div style={{ fontWeight:600, fontSize:'0.85rem' }}>{f.nome}</div>
                        <div style={{ fontSize:'0.72rem', color:'var(--text-muted)' }}>{f.cpf}</div>
                      </div>
                    </div>
                  </td>
                  <td><span className="badge badge-purple">{f.cargo}</span></td>
                  <td style={{ color:'var(--text-muted)', fontSize:'0.82rem' }}>{f.matricula || '—'}</td>
                  <td style={{ color:'var(--text-muted)', fontSize:'0.82rem' }}>{f.telefone || '—'}</td>
                  <td>
                    <span className={`badge ${f.ativo ? 'badge-green':'badge-gray'}`}>
                      <span className="dot" style={{ background: f.ativo ? 'var(--green)':'var(--text-muted)' }} />
                      {f.ativo ? 'Ativo' : 'Inativo'}
                    </span>
                  </td>
                  <td>
                    <div style={{ display:'flex', gap:6 }}>
                      <button className="btn btn-ghost btn-sm" onClick={() => openEdit(f)}>Editar</button>
                      {f.ativo && (
                        <button className="btn btn-danger btn-sm" onClick={() => handleDesativar(f.id)}>Desativar</button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <Modal open={modal==='add'||modal==='edit'} onClose={() => setModal(null)}
        title={modal==='edit' ? 'Editar Funcionário' : 'Cadastrar Funcionário'}>
        <form onSubmit={handleSubmit}>
          <div className="form-grid form-grid-2" style={{ gap:14 }}>
            <div className="form-group" style={{ gridColumn:'1/-1' }}>
              <label className="form-label">Nome completo *</label>
              <input className="form-input" required value={form.nome} onChange={F('nome')} placeholder="Nome do funcionário" />
            </div>
            <div className="form-group">
              <label className="form-label">CPF *</label>
              <input className="form-input" required value={form.cpf} onChange={F('cpf')} placeholder="000.000.000-00" disabled={modal==='edit'} />
            </div>
            <div className="form-group">
              <label className="form-label">Telefone</label>
              <input className="form-input" value={form.telefone} onChange={F('telefone')} placeholder="(11) 99999-0000" />
            </div>
            <div className="form-group">
              <label className="form-label">Cargo *</label>
              <input className="form-input" required value={form.cargo} onChange={F('cargo')} placeholder="Porteiro" />
            </div>
            <div className="form-group">
              <label className="form-label">Matrícula</label>
              <input className="form-input" value={form.matricula} onChange={F('matricula')} placeholder="PORT-001" />
            </div>
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn-ghost" onClick={() => setModal(null)}>Cancelar</button>
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? 'Salvando...' : modal==='edit' ? 'Salvar' : 'Cadastrar'}
            </button>
          </div>
        </form>
      </Modal>

      {toast && <Toast {...toast} onClose={() => setToast(null)} />}
    </div>
  )
}
