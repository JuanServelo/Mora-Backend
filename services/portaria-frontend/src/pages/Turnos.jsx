import { useEffect, useState, useCallback } from 'react'
import { api } from '../api/portariaApi'
import Modal from '../components/Modal'
import Toast from '../components/Toast'

const fmt = (dt) => dt ? new Date(dt).toLocaleString('pt-BR', { day:'2-digit', month:'2-digit', hour:'2-digit', minute:'2-digit' }) : '—'

function turnoStatus(t) {
  if (!t.entradas?.length) return 'inativo'
  if (t.entradas.length > (t.saidas?.length ?? 0)) return 'ativo'
  return 'pausado'
}

function duracao(entradas = [], saidas = []) {
  let total = 0
  entradas.forEach((e, i) => {
    const s = saidas[i]
    if (e && s) total += new Date(s) - new Date(e)
  })
  if (!total) return null
  const h = Math.floor(total / 3600000)
  const m = Math.floor((total % 3600000) / 60000)
  return `${h}h ${m}min`
}

export default function Turnos() {
  const [turnos, setTurnos]   = useState([])
  const [modal, setModal]     = useState(false)
  const [form, setForm]       = useState({ funcionario:'', cargo:'' })
  const [loading, setLoading] = useState(false)
  const [toast, setToast]     = useState(null)
  const [search, setSearch]   = useState('')

  const load = useCallback(async () => {
    try { setTurnos(await api.turnos.listar()) }
    catch (e) { setToast({ message: e.message, type:'error' }) }
  }, [])

  useEffect(() => { load() }, [load])

  const filtered = turnos.filter(t => {
    const q = search.toLowerCase()
    return t.funcionario?.toLowerCase().includes(q) || t.cargo?.toLowerCase().includes(q)
  })

  async function handleIniciar(e) {
    e.preventDefault(); setLoading(true)
    try {
      await api.turnos.iniciar(form)
      setToast({ message: `Turno de ${form.funcionario} iniciado!`, type:'success' })
      setModal(false); setForm({ funcionario:'', cargo:'' }); load()
    } catch (e) { setToast({ message: e.message, type:'error' }) }
    setLoading(false)
  }

  async function handleAcao(turno, acao) {
    const labels = { finalizar:'Pausar/finalizar', retomar:'Retomar' }
    try {
      await api.turnos[acao](turno.id)
      setToast({ message: `${labels[acao]} turno de ${turno.funcionario} — OK.`, type:'success' })
      load()
    } catch (e) { setToast({ message: e.message, type:'error' }) }
  }

  const statusConfig = {
    ativo:   { label:'Em Andamento', cls:'badge-green' },
    pausado: { label:'Pausado',      cls:'badge-amber' },
    inativo: { label:'Inativo',      cls:'badge-gray'  },
  }

  const ativos = filtered.filter(t => turnoStatus(t) === 'ativo').length

  return (
    <div>
      <div className="page-header">
        <h1>Turnos</h1>
        <p>Registro de turnos e horários dos funcionários da portaria</p>
      </div>

      <div className="toolbar">
        <div className="search-bar" style={{ flex:1, maxWidth:320 }}>
          <span>🔍</span>
          <input placeholder="Buscar por funcionário ou cargo..." value={search} onChange={e => setSearch(e.target.value)} />
        </div>
        <span style={{ fontSize:'0.78rem', color:'var(--text-muted)' }}>
          {ativos} turno(s) em andamento
        </span>
        <div className="spacer" />
        <button className="btn btn-primary" onClick={() => setModal(true)}>🕐 Iniciar Turno</button>
      </div>

      <div className="card" style={{ padding:0 }}>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Funcionário</th>
                <th>Cargo</th>
                <th>Início</th>
                <th>Último evento</th>
                <th>Duração</th>
                <th>Status</th>
                <th>Ações</th>
              </tr>
            </thead>
            <tbody>
              {filtered.length === 0 ? (
                <tr><td colSpan={7} style={{ textAlign:'center', padding:32, color:'var(--text-muted)' }}>
                  Nenhum turno registrado.
                </td></tr>
              ) : filtered.map(t => {
                const status = turnoStatus(t)
                const cfg = statusConfig[status]
                const inicioTurno = t.entradas?.[0]
                const ultimoEvento = [...(t.saidas ?? []), ...(t.entradas ?? [])].filter(Boolean).sort().at(-1)

                return (
                  <tr key={t.id}>
                    <td><span style={{ fontWeight:600 }}>{t.funcionario}</span></td>
                    <td><span className="badge badge-purple">{t.cargo}</span></td>
                    <td style={{ fontSize:'0.8rem', color:'var(--text-muted)' }}>{fmt(inicioTurno)}</td>
                    <td style={{ fontSize:'0.8rem', color:'var(--text-muted)' }}>{fmt(ultimoEvento)}</td>
                    <td style={{ fontSize:'0.8rem', color:'var(--text-muted)' }}>
                      {duracao(t.entradas, t.saidas) ?? '—'}
                    </td>
                    <td>
                      <span className={`badge ${cfg.cls}`}>
                        <span className="dot" style={{
                          background: status==='ativo'?'var(--green)': status==='pausado'?'var(--amber)':'var(--text-muted)'
                        }} />
                        {cfg.label}
                      </span>
                    </td>
                    <td>
                      <div style={{ display:'flex', gap:6 }}>
                        {status === 'ativo' && (
                          <button className="btn btn-amber btn-sm" onClick={() => handleAcao(t, 'finalizar')}>
                            Pausar
                          </button>
                        )}
                        {status === 'pausado' && (
                          <button className="btn btn-success btn-sm" onClick={() => handleAcao(t, 'retomar')}>
                            Retomar
                          </button>
                        )}
                        {status === 'ativo' && (
                          <button className="btn btn-danger btn-sm" onClick={() => handleAcao(t, 'finalizar')}>
                            Encerrar
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      </div>

      <Modal open={modal} onClose={() => setModal(false)} title="Iniciar Novo Turno">
        <form onSubmit={handleIniciar}>
          <div className="form-grid" style={{ gap:14 }}>
            <div className="form-group">
              <label className="form-label">Nome do funcionário *</label>
              <input className="form-input" required value={form.funcionario}
                onChange={e => setForm(f => ({ ...f, funcionario: e.target.value }))}
                placeholder="Nome do porteiro/funcionário" />
            </div>
            <div className="form-group">
              <label className="form-label">Cargo *</label>
              <input className="form-input" required value={form.cargo}
                onChange={e => setForm(f => ({ ...f, cargo: e.target.value }))}
                placeholder="Porteiro, Segurança..." />
            </div>
          </div>
          <div style={{ background:'var(--bg-2)', borderRadius:8, padding:12, marginTop:12, fontSize:'0.78rem', color:'var(--text-muted)' }}>
            O horário de início será registrado automaticamente no momento da confirmação.
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn-ghost" onClick={() => setModal(false)}>Cancelar</button>
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? 'Iniciando...' : '🕐 Iniciar Turno'}
            </button>
          </div>
        </form>
      </Modal>

      {toast && <Toast {...toast} onClose={() => setToast(null)} />}
    </div>
  )
}
