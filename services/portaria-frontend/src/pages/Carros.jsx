import { useEffect, useState, useCallback } from 'react'
import { api } from '../api/portariaApi'
import Modal from '../components/Modal'
import Toast from '../components/Toast'

const initForm = { placa:'', modelo:'', proprietario:'' }
const fmt = (dt) => dt ? new Date(dt).toLocaleString('pt-BR', { day:'2-digit', month:'2-digit', hour:'2-digit', minute:'2-digit' }) : '—'

export default function Carros() {
  const [lista, setLista]     = useState([])
  const [tab, setTab]         = useState('dentro')
  const [modal, setModal]     = useState(false)
  const [form, setForm]       = useState(initForm)
  const [loading, setLoading] = useState(false)
  const [toast, setToast]     = useState(null)
  const [search, setSearch]   = useState('')

  const load = useCallback(async () => {
    try {
      const data = tab === 'dentro' ? await api.carros.dentro() : await api.carros.listar()
      setLista(data)
    } catch (e) { setToast({ message: e.message, type:'error' }) }
  }, [tab])

  useEffect(() => { load() }, [load])

  const filtered = lista.filter(c => {
    const q = search.toLowerCase()
    return c.placa?.toLowerCase().includes(q) || c.modelo?.toLowerCase().includes(q) || c.proprietario?.toLowerCase().includes(q)
  })

  async function handleEntrada(e) {
    e.preventDefault(); setLoading(true)
    try {
      await api.carros.entrada({ ...form, placa: form.placa.toUpperCase() })
      setToast({ message: `Veículo ${form.placa.toUpperCase()} registrado.`, type:'success' })
      setModal(false); setForm(initForm); setTab('dentro'); load()
    } catch (e) { setToast({ message: e.message, type:'error' }) }
    setLoading(false)
  }

  async function handleSaida(id, placa) {
    if (!confirm(`Registrar saída do veículo ${placa}?`)) return
    try {
      await api.carros.saida(id)
      setToast({ message: `Saída do veículo ${placa} registrada.`, type:'success' })
      load()
    } catch (e) { setToast({ message: e.message, type:'error' }) }
  }

  const F = (k) => (e) => setForm(f => ({ ...f, [k]: e.target.value }))

  return (
    <div>
      <div className="page-header">
        <h1>Veículos</h1>
        <p>Controle de entrada e saída de veículos</p>
      </div>

      <div className="toolbar">
        <div className="tabs">
          <button className={`tab ${tab==='dentro'?'active':''}`} onClick={() => setTab('dentro')}>
            No Condomínio {tab==='dentro' && lista.length > 0 && <span style={{ background:'var(--accent)', color:'#fff', fontSize:'0.65rem', fontWeight:700, padding:'1px 6px', borderRadius:99, marginLeft:4 }}>{lista.length}</span>}
          </button>
          <button className={`tab ${tab==='todos'?'active':''}`} onClick={() => setTab('todos')}>Histórico</button>
        </div>
        <div className="search-bar" style={{ flex:1, maxWidth:320 }}>
          <span>🔍</span>
          <input placeholder="Buscar por placa, modelo..." value={search} onChange={e => setSearch(e.target.value)} />
        </div>
        <div className="spacer" />
        <button className="btn btn-primary" onClick={() => { setForm(initForm); setModal(true) }}>
          🚗 Registrar Entrada
        </button>
      </div>

      <div className="card" style={{ padding:0 }}>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Placa</th>
                <th>Modelo</th>
                <th>Proprietário</th>
                <th>Entrada</th>
                <th>Saída</th>
                <th>Status</th>
                {tab === 'dentro' && <th>Ação</th>}
              </tr>
            </thead>
            <tbody>
              {filtered.length === 0 ? (
                <tr><td colSpan={7} style={{ textAlign:'center', padding:32, color:'var(--text-muted)' }}>
                  {tab === 'dentro' ? 'Nenhum veículo no condomínio.' : 'Nenhum veículo registrado.'}
                </td></tr>
              ) : filtered.map(c => (
                <tr key={c.id}>
                  <td>
                    <span style={{ fontFamily:'monospace', fontWeight:700, fontSize:'0.9rem', background:'var(--bg-3)', padding:'3px 8px', borderRadius:4 }}>
                      {c.placa}
                    </span>
                  </td>
                  <td style={{ fontSize:'0.85rem' }}>{c.modelo || '—'}</td>
                  <td style={{ fontSize:'0.82rem', color:'var(--text-muted)' }}>{c.proprietario || '—'}</td>
                  <td style={{ fontSize:'0.8rem', color:'var(--text-muted)' }}>{fmt(c.dataEntrada)}</td>
                  <td style={{ fontSize:'0.8rem', color:'var(--text-muted)' }}>{fmt(c.dataSaida)}</td>
                  <td>
                    <span className={`badge ${c.status==='DENTRO'?'badge-green':'badge-gray'}`}>
                      <span className="dot" style={{ background: c.status==='DENTRO'?'var(--green)':'var(--text-muted)' }} />
                      {c.status === 'DENTRO' ? 'Dentro' : 'Saiu'}
                    </span>
                  </td>
                  {tab === 'dentro' && (
                    <td>
                      <button className="btn btn-danger btn-sm" onClick={() => handleSaida(c.id, c.placa)}>
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

      <Modal open={modal} onClose={() => setModal(false)} title="Registrar Entrada de Veículo">
        <form onSubmit={handleEntrada}>
          <div className="form-grid" style={{ gap:14 }}>
            <div className="form-group">
              <label className="form-label">Placa *</label>
              <input className="form-input" required value={form.placa} onChange={F('placa')}
                placeholder="ABC-1234" style={{ textTransform:'uppercase', fontFamily:'monospace' }} />
            </div>
            <div className="form-group">
              <label className="form-label">Modelo</label>
              <input className="form-input" value={form.modelo} onChange={F('modelo')} placeholder="Honda Civic" />
            </div>
            <div className="form-group">
              <label className="form-label">Proprietário</label>
              <input className="form-input" value={form.proprietario} onChange={F('proprietario')} placeholder="Nome do proprietário" />
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
