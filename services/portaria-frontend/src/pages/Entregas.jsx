import { useEffect, useState, useCallback } from 'react'
import { api } from '../api/portariaApi'
import Modal from '../components/Modal'
import Toast from '../components/Toast'

const initForm = { nomeEntregador:'', destinatario:'', descricao:'' }
const fmt = (dt) => dt ? new Date(dt).toLocaleString('pt-BR', { day:'2-digit', month:'2-digit', hour:'2-digit', minute:'2-digit' }) : '—'

export default function Entregas() {
  const [lista, setLista]         = useState([])
  const [tab, setTab]             = useState('pendentes')
  const [modal, setModal]         = useState(null)
  const [selected, setSelected]   = useState(null)
  const [form, setForm]           = useState(initForm)
  const [recebedor, setRecebedor] = useState('')
  const [loading, setLoading]     = useState(false)
  const [toast, setToast]         = useState(null)
  const [search, setSearch]       = useState('')

  const load = useCallback(async () => {
    try {
      const data = tab === 'pendentes' ? await api.entregas.pendentes() : await api.entregas.listar()
      setLista(data)
    } catch (e) { setToast({ message: e.message, type:'error' }) }
  }, [tab])

  useEffect(() => { load() }, [load])

  const filtered = lista.filter(e => {
    const q = search.toLowerCase()
    return e.destinatario?.toLowerCase().includes(q) || e.nomeEntregador?.toLowerCase().includes(q) || e.descricao?.toLowerCase().includes(q)
  })

  async function handleRegistrar(e) {
    e.preventDefault(); setLoading(true)
    try {
      await api.entregas.registrar(form)
      setToast({ message: 'Entrega registrada!', type:'success' })
      setModal(null); setForm(initForm); load()
    } catch (e) { setToast({ message: e.message, type:'error' }) }
    setLoading(false)
  }

  async function handleRetirar(e) {
    e.preventDefault(); setLoading(true)
    try {
      await api.entregas.retirar(selected.id, recebedor)
      setToast({ message: `Entrega retirada por ${recebedor}.`, type:'success' })
      setModal(null); setRecebedor(''); load()
    } catch (e) { setToast({ message: e.message, type:'error' }) }
    setLoading(false)
  }

  const F = (k) => (e) => setForm(f => ({ ...f, [k]: e.target.value }))

  return (
    <div>
      <div className="page-header">
        <h1>Entregas</h1>
        <p>Registro de encomendas e controle de retiradas</p>
      </div>

      <div className="toolbar">
        <div className="tabs">
          <button className={`tab ${tab==='pendentes'?'active':''}`} onClick={() => setTab('pendentes')}>
            Pendentes {tab==='pendentes' && lista.length > 0 && (
              <span style={{ background:'var(--amber)', color:'#fff', fontSize:'0.65rem', fontWeight:700, padding:'1px 6px', borderRadius:99, marginLeft:4 }}>{lista.length}</span>
            )}
          </button>
          <button className={`tab ${tab==='todas'?'active':''}`} onClick={() => setTab('todas')}>Todas</button>
        </div>
        <div className="search-bar" style={{ flex:1, maxWidth:320 }}>
          <span>🔍</span>
          <input placeholder="Buscar por destinatário ou entregador..." value={search} onChange={e => setSearch(e.target.value)} />
        </div>
        <div className="spacer" />
        <button className="btn btn-primary" onClick={() => { setForm(initForm); setModal('add') }}>
          📦 Registrar Entrega
        </button>
      </div>

      <div className="card" style={{ padding:0 }}>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Entregador</th>
                <th>Destinatário</th>
                <th>Descrição</th>
                <th>Recebimento</th>
                <th>Retirada</th>
                <th>Status</th>
                {tab === 'pendentes' && <th>Ação</th>}
              </tr>
            </thead>
            <tbody>
              {filtered.length === 0 ? (
                <tr><td colSpan={7} style={{ textAlign:'center', padding:32, color:'var(--text-muted)' }}>
                  {tab === 'pendentes' ? '✓ Nenhuma entrega pendente.' : 'Nenhuma entrega registrada.'}
                </td></tr>
              ) : filtered.map(e => (
                <tr key={e.id}>
                  <td style={{ fontSize:'0.85rem' }}>{e.nomeEntregador}</td>
                  <td><span style={{ fontWeight:600 }}>{e.destinatario}</span></td>
                  <td style={{ fontSize:'0.82rem', color:'var(--text-muted)', maxWidth:180 }}>
                    <span style={{ display:'block', overflow:'hidden', textOverflow:'ellipsis', whiteSpace:'nowrap' }}>
                      {e.descricao || '—'}
                    </span>
                  </td>
                  <td style={{ fontSize:'0.8rem', color:'var(--text-muted)' }}>{fmt(e.dataRecebimento)}</td>
                  <td style={{ fontSize:'0.8rem', color:'var(--text-muted)' }}>
                    {e.retirada ? `${fmt(e.dataRetirada)} · ${e.recebedor}` : '—'}
                  </td>
                  <td>
                    <span className={`badge ${e.retirada?'badge-gray':'badge-amber'}`}>
                      {e.retirada ? '✓ Retirada' : 'Aguardando'}
                    </span>
                  </td>
                  {tab === 'pendentes' && (
                    <td>
                      <button className="btn btn-success btn-sm"
                        onClick={() => { setSelected(e); setRecebedor(''); setModal('retirar') }}>
                        Registrar Retirada
                      </button>
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Registrar entrega */}
      <Modal open={modal==='add'} onClose={() => setModal(null)} title="Registrar Nova Entrega">
        <form onSubmit={handleRegistrar}>
          <div className="form-grid" style={{ gap:14 }}>
            <div className="form-group">
              <label className="form-label">Nome do entregador *</label>
              <input className="form-input" required value={form.nomeEntregador} onChange={F('nomeEntregador')} placeholder="Correios, iFood, etc." />
            </div>
            <div className="form-group">
              <label className="form-label">Destinatário *</label>
              <input className="form-input" required value={form.destinatario} onChange={F('destinatario')} placeholder="Nome — Apt 101" />
            </div>
            <div className="form-group">
              <label className="form-label">Descrição do pacote</label>
              <input className="form-input" value={form.descricao} onChange={F('descricao')} placeholder="Caixa pequena, envelope..." />
            </div>
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn-ghost" onClick={() => setModal(null)}>Cancelar</button>
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? 'Registrando...' : '📦 Registrar'}
            </button>
          </div>
        </form>
      </Modal>

      {/* Retirada */}
      <Modal open={modal==='retirar'} onClose={() => setModal(null)} title="Registrar Retirada">
        {selected && (
          <form onSubmit={handleRetirar}>
            <div style={{ background:'var(--bg-2)', borderRadius:8, padding:14, marginBottom:16, fontSize:'0.85rem' }}>
              <div style={{ color:'var(--text-muted)', fontSize:'0.75rem', marginBottom:4 }}>Entrega para</div>
              <div style={{ fontWeight:700 }}>{selected.destinatario}</div>
              <div style={{ color:'var(--text-muted)', marginTop:4 }}>De: {selected.nomeEntregador} · {selected.descricao}</div>
            </div>
            <div className="form-group">
              <label className="form-label">Quem está retirando? *</label>
              <input className="form-input" required value={recebedor} onChange={e => setRecebedor(e.target.value)} placeholder="Nome de quem retirou" />
            </div>
            <div className="modal-footer">
              <button type="button" className="btn btn-ghost" onClick={() => setModal(null)}>Cancelar</button>
              <button type="submit" className="btn btn-success" disabled={loading}>
                {loading ? 'Confirmando...' : '✓ Confirmar Retirada'}
              </button>
            </div>
          </form>
        )}
      </Modal>

      {toast && <Toast {...toast} onClose={() => setToast(null)} />}
    </div>
  )
}
