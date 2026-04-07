import { useEffect, useState, useCallback } from 'react'
import { api } from '../api/portariaApi'
import Modal from '../components/Modal'
import Toast from '../components/Toast'

const initForm = { nome:'', documento:'', motivoVisita:'' }

function fmt(dt) {
  if (!dt) return '—'
  return new Date(dt).toLocaleString('pt-BR', { day:'2-digit', month:'2-digit', hour:'2-digit', minute:'2-digit' })
}

export default function Visitantes() {
  const [lista, setLista]     = useState([])
  const [tab, setTab]         = useState('dentro')
  const [modal, setModal]     = useState(false)
  const [form, setForm]       = useState(initForm)
  const [loading, setLoading] = useState(false)
  const [toast, setToast]     = useState(null)
  const [search, setSearch]   = useState('')

  const load = useCallback(async () => {
    try {
      const data = tab === 'dentro' ? await api.visitantes.dentro() : await api.visitantes.listar()
      setLista(data)
    } catch (e) { setToast({ message: e.message, type:'error' }) }
  }, [tab])

  useEffect(() => { load() }, [load])

  const filtered = lista.filter(v => {
    const q = search.toLowerCase()
    return v.nome.toLowerCase().includes(q) || v.documento?.toLowerCase().includes(q)
  })

  async function handleEntrada(e) {
    e.preventDefault(); setLoading(true)
    try {
      await api.visitantes.entrada(form)
      setToast({ message: `${form.nome} registrado(a) como DENTRO.`, type:'success' })
      setModal(false); setForm(initForm); setTab('dentro'); load()
    } catch (e) { setToast({ message: e.message, type:'error' }) }
    setLoading(false)
  }

  async function handleSaida(id, nome) {
    if (!confirm(`Registrar saída de ${nome}?`)) return
    try {
      await api.visitantes.saida(id)
      setToast({ message: `Saída de ${nome} registrada.`, type:'success' })
      load()
    } catch (e) { setToast({ message: e.message, type:'error' }) }
  }

  const F = (k) => (e) => setForm(f => ({ ...f, [k]: e.target.value }))

  return (
    <div>
      <div className="page-header">
        <h1>Visitantes</h1>
        <p>Controle de entrada e saída de pessoas externas</p>
      </div>

      <div className="toolbar">
        <div className="tabs">
          <button className={`tab ${tab==='dentro'?'active':''}`} onClick={() => setTab('dentro')}>
            Dentro {tab==='dentro' && lista.length > 0 && <span className="badge-count" style={{ background:'var(--green)', marginLeft:4 }}>{lista.length}</span>}
          </button>
          <button className={`tab ${tab==='todos'?'active':''}`} onClick={() => setTab('todos')}>Histórico</button>
        </div>
        <div className="search-bar" style={{ flex:1, maxWidth:320 }}>
          <span>🔍</span>
          <input placeholder="Buscar visitante..." value={search} onChange={e => setSearch(e.target.value)} />
        </div>
        <div className="spacer" />
        <button className="btn btn-primary" onClick={() => { setForm(initForm); setModal(true) }}>
          👥 Registrar Entrada
        </button>
      </div>

      <div className="card" style={{ padding:0 }}>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Visitante</th>
                <th>Documento</th>
                <th>Motivo</th>
                <th>Entrada</th>
                <th>Saída</th>
                <th>Status</th>
                {tab === 'dentro' && <th>Ação</th>}
              </tr>
            </thead>
            <tbody>
              {filtered.length === 0 ? (
                <tr><td colSpan={7} style={{ textAlign:'center', padding:32, color:'var(--text-muted)' }}>
                  {tab === 'dentro' ? 'Nenhum visitante no condomínio.' : 'Nenhum visitante registrado.'}
                </td></tr>
              ) : filtered.map(v => (
                <tr key={v.id}>
                  <td><span style={{ fontWeight:600 }}>{v.nome}</span></td>
                  <td style={{ color:'var(--text-muted)', fontSize:'0.82rem' }}>{v.documento || '—'}</td>
                  <td style={{ fontSize:'0.82rem' }}>{v.motivoVisita || '—'}</td>
                  <td style={{ fontSize:'0.8rem', color:'var(--text-muted)' }}>{fmt(v.dataEntrada)}</td>
                  <td style={{ fontSize:'0.8rem', color:'var(--text-muted)' }}>{fmt(v.dataSaida)}</td>
                  <td>
                    <span className={`badge ${v.status==='DENTRO' ? 'badge-green':'badge-gray'}`}>
                      <span className="dot" style={{ background: v.status==='DENTRO'?'var(--green)':'var(--text-muted)' }} />
                      {v.status === 'DENTRO' ? 'Dentro' : 'Saiu'}
                    </span>
                  </td>
                  {tab === 'dentro' && (
                    <td>
                      <button className="btn btn-danger btn-sm" onClick={() => handleSaida(v.id, v.nome)}>
                        Registrar Saída
                      </button>
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* FAB */}
      <div style={{ position:'fixed', bottom:28, right:28 }}>
        <button className="btn btn-primary" onClick={() => { setForm(initForm); setModal(true) }}>
          👥+ Convidar Visitante
        </button>
      </div>

      <Modal open={modal} onClose={() => setModal(false)} title="Registrar Entrada de Visitante">
        <form onSubmit={handleEntrada}>
          <div className="form-grid" style={{ gap:14 }}>
            <div className="form-group">
              <label className="form-label">Nome do visitante *</label>
              <input className="form-input" required value={form.nome} onChange={F('nome')} placeholder="Nome completo" />
            </div>
            <div className="form-group">
              <label className="form-label">Documento (RG / CPF)</label>
              <input className="form-input" value={form.documento} onChange={F('documento')} placeholder="RG-12345678" />
            </div>
            <div className="form-group">
              <label className="form-label">Motivo da visita</label>
              <input className="form-input" value={form.motivoVisita} onChange={F('motivoVisita')} placeholder="Visita ao apartamento 101..." />
            </div>
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn-ghost" onClick={() => setModal(false)}>Cancelar</button>
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? 'Registrando...' : '✓ Registrar Entrada'}
            </button>
          </div>
        </form>
      </Modal>

      {toast && <Toast {...toast} onClose={() => setToast(null)} />}
    </div>
  )
}
