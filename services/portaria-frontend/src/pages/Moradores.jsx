import { useEffect, useState, useCallback } from 'react'
import { api } from '../api/portariaApi'
import Modal from '../components/Modal'
import Toast from '../components/Toast'

const initForm = { nome:'', cpf:'', apartamento:'', bloco:'', telefone:'' }

function initials(name = '') {
  return name.split(' ').slice(0,2).map(w => w[0]).join('').toUpperCase()
}

export default function Moradores() {
  const [moradores, setMoradores] = useState([])
  const [filtered, setFiltered]   = useState([])
  const [search, setSearch]       = useState('')
  const [tab, setTab]             = useState('ativos')
  const [modal, setModal]         = useState(null) // 'add' | 'edit' | 'view'
  const [selected, setSelected]   = useState(null)
  const [form, setForm]           = useState(initForm)
  const [loading, setLoading]     = useState(false)
  const [toast, setToast]         = useState(null)

  const load = useCallback(async () => {
    try {
      const data = tab === 'todos'
        ? await api.moradores.listarTodos()
        : await api.moradores.listar()
      setMoradores(data)
    } catch (e) { setToast({ message: e.message, type:'error' }) }
  }, [tab])

  useEffect(() => { load() }, [load])

  useEffect(() => {
    const q = search.toLowerCase()
    setFiltered(moradores.filter(m =>
      m.nome.toLowerCase().includes(q) ||
      m.apartamento?.toLowerCase().includes(q) ||
      m.bloco?.toLowerCase().includes(q) ||
      m.cpf?.includes(q)
    ))
  }, [moradores, search])

  // Group by bloco/torre
  const byBloco = filtered.reduce((acc, m) => {
    const key = m.bloco ? `Torre ${m.bloco.toUpperCase()}` : 'Sem Torre'
    acc[key] = acc[key] || []
    acc[key].push(m)
    return acc
  }, {})

  async function handleSubmit(e) {
    e.preventDefault()
    setLoading(true)
    try {
      if (modal === 'edit' && selected) {
        await api.moradores.atualizar(selected.id, form)
        setToast({ message: 'Morador atualizado!', type:'success' })
      } else {
        await api.moradores.cadastrar(form)
        setToast({ message: 'Morador cadastrado!', type:'success' })
      }
      setModal(null); setForm(initForm); load()
    } catch (e) { setToast({ message: e.message, type:'error' }) }
    setLoading(false)
  }

  async function handleDesativar(id) {
    if (!confirm('Desativar este morador?')) return
    try {
      await api.moradores.desativar(id)
      setToast({ message: 'Morador desativado.', type:'success' })
      setModal(null); load()
    } catch (e) { setToast({ message: e.message, type:'error' }) }
  }

  function openEdit(m) {
    setSelected(m)
    setForm({ nome:m.nome, cpf:m.cpf, apartamento:m.apartamento, bloco:m.bloco||'', telefone:m.telefone||'' })
    setModal('edit')
  }

  function openView(m) { setSelected(m); setModal('view') }

  const F = (k) => (e) => setForm(f => ({ ...f, [k]: e.target.value }))

  return (
    <div>
      <div className="page-header">
        <h1>Moradores</h1>
        <p>Diretório de moradores cadastrados no condomínio</p>
      </div>

      {/* Info strip */}
      <div className="info-cards-row" style={{ marginBottom:24 }}>
        <div className="info-card">
          <div className="left">
            <div className="icon-box icon-blue">🔍</div>
            <input
              className="form-input"
              style={{ background:'transparent', border:'none', padding:0, fontSize:'0.9rem' }}
              placeholder="Buscar morador por nome, CPF ou apartamento..."
              value={search}
              onChange={e => setSearch(e.target.value)}
            />
          </div>
        </div>
        <div className="info-card">
          <div className="left">
            <div className="icon-box icon-green">⌂</div>
            <div>
              <div className="title">Total Cadastrados</div>
              <div className="sub">{moradores.length} morador(es) {tab === 'todos' ? 'no sistema' : 'ativos'}</div>
            </div>
          </div>
          <button className="btn btn-primary btn-sm" onClick={() => { setForm(initForm); setModal('add') }}>
            + Cadastrar
          </button>
        </div>
      </div>

      {/* Tabs + filter */}
      <div className="toolbar">
        <div className="tabs">
          <button className={`tab ${tab==='ativos'?'active':''}`} onClick={() => setTab('ativos')}>Ativos</button>
          <button className={`tab ${tab==='todos'?'active':''}`}  onClick={() => setTab('todos')}>Todos</button>
        </div>
        <div className="spacer" />
        <span style={{ fontSize:'0.75rem', color:'var(--text-muted)' }}>
          Exibindo {filtered.length} contato(s) · Privacidade e Segurança Garantidas.
        </span>
      </div>

      {/* Directory by Torre */}
      {filtered.length === 0 ? (
        <div className="empty-state">
          <div className="icon">👥</div>
          <p>{search ? 'Nenhum morador encontrado.' : 'Nenhum morador cadastrado.'}</p>
        </div>
      ) : (
        <div className="torre-grid">
          {Object.entries(byBloco).map(([torre, lista]) => (
            <div key={torre} className="torre-col">
              <div className="torre-header">{torre}</div>
              {lista.map(m => (
                <div key={m.id} className="person-card" onClick={() => openView(m)}>
                  <div className="avatar">{initials(m.nome)}</div>
                  <div className="info">
                    <div className="name">{m.nome}</div>
                    <div className="sub">Apt {m.apartamento}{m.bloco ? ` · Bloco ${m.bloco}` : ''}</div>
                  </div>
                  {!m.ativo && <span className="badge badge-gray">Inativo</span>}
                </div>
              ))}
            </div>
          ))}
        </div>
      )}

      {/* Convidar Visitante style button */}
      <div style={{ position:'fixed', bottom:28, right:28 }}>
        <button className="btn btn-primary" onClick={() => { setForm(initForm); setModal('add') }}>
          👤+ Cadastrar Morador
        </button>
      </div>

      {/* View Modal */}
      <Modal open={modal==='view'} onClose={() => setModal(null)} title="Detalhes do Morador">
        {selected && (
          <div>
            <div style={{ textAlign:'center', marginBottom:20 }}>
              <div style={{ width:64, height:64, borderRadius:'50%', background:'var(--bg-3)', display:'flex', alignItems:'center', justifyContent:'center', fontSize:'1.5rem', fontWeight:700, margin:'0 auto 12px' }}>
                {initials(selected.nome)}
              </div>
              <div style={{ fontWeight:700, fontSize:'1.1rem' }}>{selected.nome}</div>
              <div style={{ color:'var(--text-muted)', fontSize:'0.82rem' }}>
                Apt {selected.apartamento}{selected.bloco ? ` · Bloco ${selected.bloco}` : ''}
              </div>
              <div style={{ marginTop:8 }}>
                <span className={`badge ${selected.ativo ? 'badge-green':'badge-gray'}`}>
                  {selected.ativo ? 'Ativo' : 'Inativo'}
                </span>
              </div>
            </div>
            <div style={{ display:'flex', flexDirection:'column', gap:8, fontSize:'0.85rem' }}>
              <div style={{ display:'flex', justifyContent:'space-between' }}>
                <span style={{ color:'var(--text-muted)' }}>CPF</span><span>{selected.cpf}</span>
              </div>
              {selected.telefone && (
                <div style={{ display:'flex', justifyContent:'space-between' }}>
                  <span style={{ color:'var(--text-muted)' }}>Telefone</span><span>{selected.telefone}</span>
                </div>
              )}
              <div style={{ display:'flex', justifyContent:'space-between' }}>
                <span style={{ color:'var(--text-muted)' }}>Cadastrado em</span>
                <span>{selected.criadoEm ? new Date(selected.criadoEm).toLocaleDateString('pt-BR') : '—'}</span>
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn btn-danger btn-sm" onClick={() => handleDesativar(selected.id)}>
                Desativar
              </button>
              <button className="btn btn-ghost btn-sm" onClick={() => openEdit(selected)}>
                Editar
              </button>
            </div>
          </div>
        )}
      </Modal>

      {/* Add/Edit Modal */}
      <Modal open={modal==='add'||modal==='edit'} onClose={() => setModal(null)}
        title={modal==='edit' ? 'Editar Morador' : 'Cadastrar Morador'}>
        <form onSubmit={handleSubmit}>
          <div className="form-grid form-grid-2" style={{ gap:14 }}>
            <div className="form-group" style={{ gridColumn:'1/-1' }}>
              <label className="form-label">Nome completo *</label>
              <input className="form-input" required value={form.nome} onChange={F('nome')} placeholder="Nome do morador" />
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
              <label className="form-label">Apartamento *</label>
              <input className="form-input" required value={form.apartamento} onChange={F('apartamento')} placeholder="101" />
            </div>
            <div className="form-group">
              <label className="form-label">Bloco / Torre</label>
              <input className="form-input" value={form.bloco} onChange={F('bloco')} placeholder="A" />
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
