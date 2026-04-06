import { useEffect, useState, useCallback } from 'react'
import { api } from '../api/portariaApi'
import Modal from '../components/Modal'
import Toast from '../components/Toast'

const fmt = (dt) => dt ? new Date(dt).toLocaleString('pt-BR', { day:'2-digit', month:'2-digit', hour:'2-digit', minute:'2-digit' }) : null

export default function Chaves() {
  const [chaves, setChaves]         = useState([])
  const [moradores, setMoradores]   = useState([])
  const [funcionarios, setFunc]     = useState([])
  const [tab, setTab]               = useState('todas')
  const [modal, setModal]           = useState(null) // 'add' | 'retirar'
  const [selected, setSelected]     = useState(null)
  const [nomeChave, setNomeChave]   = useState('')
  const [retirarForm, setRetirarForm] = useState({ responsavelId:'', tipoResponsavel:'MORADOR' })
  const [loading, setLoading]       = useState(false)
  const [toast, setToast]           = useState(null)

  const load = useCallback(async () => {
    try {
      const [c, m, f] = await Promise.all([
        api.chaves.listar(),
        api.moradores.listar(),
        api.funcionarios.listar(),
      ])
      setChaves(c); setMoradores(m); setFunc(f)
    } catch (e) { setToast({ message: e.message, type:'error' }) }
  }, [])

  useEffect(() => { load() }, [load])

  const filtered = tab === 'disponiveis'
    ? chaves.filter(c => c.disponivel)
    : tab === 'retiradas'
    ? chaves.filter(c => !c.disponivel)
    : chaves

  async function handleCadastrar(e) {
    e.preventDefault(); setLoading(true)
    try {
      await api.chaves.cadastrar({ nomeChave })
      setToast({ message: 'Chave cadastrada!', type:'success' })
      setModal(null); setNomeChave(''); load()
    } catch (e) { setToast({ message: e.message, type:'error' }) }
    setLoading(false)
  }

  async function handleRetirar(e) {
    e.preventDefault(); setLoading(true)
    try {
      await api.chaves.retirar(selected.id, retirarForm)
      setToast({ message: 'Chave retirada com sucesso!', type:'success' })
      setModal(null); load()
    } catch (e) { setToast({ message: e.message, type:'error' }) }
    setLoading(false)
  }

  async function handleDevolver(id, nome) {
    if (!confirm(`Registrar devolução de "${nome}"?`)) return
    try {
      await api.chaves.devolver(id)
      setToast({ message: 'Chave devolvida!', type:'success' })
      load()
    } catch (e) { setToast({ message: e.message, type:'error' }) }
  }

  async function handleDeletar(id, nome) {
    if (!confirm(`Remover chave "${nome}"? Esta ação não pode ser desfeita.`)) return
    try {
      await api.chaves.deletar(id)
      setToast({ message: 'Chave removida.', type:'success' })
      load()
    } catch (e) { setToast({ message: e.message, type:'error' }) }
  }

  const listaResponsaveis = retirarForm.tipoResponsavel === 'MORADOR' ? moradores : funcionarios

  return (
    <div>
      <div className="page-header">
        <h1>Chaves</h1>
        <p>Controle de chaves do condomínio — retiradas e devoluções</p>
      </div>

      <div className="toolbar">
        <div className="tabs">
          <button className={`tab ${tab==='todas'?'active':''}`} onClick={() => setTab('todas')}>Todas ({chaves.length})</button>
          <button className={`tab ${tab==='disponiveis'?'active':''}`} onClick={() => setTab('disponiveis')}>
            Disponíveis ({chaves.filter(c=>c.disponivel).length})
          </button>
          <button className={`tab ${tab==='retiradas'?'active':''}`} onClick={() => setTab('retiradas')}>
            Retiradas ({chaves.filter(c=>!c.disponivel).length})
          </button>
        </div>
        <div className="spacer" />
        <button className="btn btn-primary" onClick={() => setModal('add')}>🔑 + Nova Chave</button>
      </div>

      {filtered.length === 0 ? (
        <div className="empty-state"><div className="icon">🔑</div><p>Nenhuma chave encontrada.</p></div>
      ) : (
        <div className="key-grid">
          {filtered.map(c => (
            <div key={c.id} className="key-card">
              <div className="key-header">
                <div className="key-name">
                  🔑 {c.nomeChave}
                </div>
                <span className={`badge ${c.disponivel?'badge-green':'badge-amber'}`}>
                  {c.disponivel ? 'Disponível' : 'Retirada'}
                </span>
              </div>

              {!c.disponivel && (
                <div className="key-info">
                  <span>👤 <strong>{c.nomeResponsavel}</strong></span>
                  <span style={{ textTransform:'lowercase' }}>
                    <span className={`badge badge-${c.tipoResponsavel==='MORADOR'?'blue':'purple'}`} style={{ fontSize:'0.65rem' }}>
                      {c.tipoResponsavel}
                    </span>
                  </span>
                  <span>Retirada: {fmt(c.retirada)}</span>
                </div>
              )}

              {c.disponivel && c.devolucao && (
                <div className="key-info">
                  <span style={{ color:'var(--text-muted)' }}>Última devolução: {fmt(c.devolucao)}</span>
                </div>
              )}

              <div className="key-actions">
                {c.disponivel ? (
                  <>
                    <button className="btn btn-amber btn-sm" style={{ flex:1 }}
                      onClick={() => { setSelected(c); setRetirarForm({ responsavelId:'', tipoResponsavel:'MORADOR' }); setModal('retirar') }}>
                      Retirar
                    </button>
                    <button className="btn btn-ghost btn-sm btn-icon" onClick={() => handleDeletar(c.id, c.nomeChave)} title="Remover chave">
                      🗑
                    </button>
                  </>
                ) : (
                  <button className="btn btn-success btn-sm" style={{ flex:1 }} onClick={() => handleDevolver(c.id, c.nomeChave)}>
                    ✓ Devolver
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Cadastrar chave */}
      <Modal open={modal==='add'} onClose={() => setModal(null)} title="Cadastrar Nova Chave">
        <form onSubmit={handleCadastrar}>
          <div className="form-group">
            <label className="form-label">Nome da chave *</label>
            <input className="form-input" required value={nomeChave} onChange={e => setNomeChave(e.target.value)}
              placeholder="Ex: Chave Salão de Festas, Chave Bloco A..." />
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn-ghost" onClick={() => setModal(null)}>Cancelar</button>
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? 'Salvando...' : 'Cadastrar'}
            </button>
          </div>
        </form>
      </Modal>

      {/* Retirar chave */}
      <Modal open={modal==='retirar'} onClose={() => setModal(null)} title={`Retirar: ${selected?.nomeChave}`}>
        <form onSubmit={handleRetirar}>
          <div className="form-grid" style={{ gap:14 }}>
            <div className="form-group">
              <label className="form-label">Tipo de responsável</label>
              <div style={{ display:'flex', gap:8 }}>
                {['MORADOR','FUNCIONARIO'].map(t => (
                  <button key={t} type="button"
                    className={`btn btn-sm ${retirarForm.tipoResponsavel===t?'btn-primary':'btn-ghost'}`}
                    onClick={() => setRetirarForm(f => ({ ...f, tipoResponsavel:t, responsavelId:'' }))}>
                    {t === 'MORADOR' ? '⌂ Morador' : '👤 Funcionário'}
                  </button>
                ))}
              </div>
            </div>
            <div className="form-group">
              <label className="form-label">
                {retirarForm.tipoResponsavel === 'MORADOR' ? 'Morador' : 'Funcionário'} *
              </label>
              <select className="form-select" required value={retirarForm.responsavelId}
                onChange={e => setRetirarForm(f => ({ ...f, responsavelId:e.target.value }))}>
                <option value="">Selecione...</option>
                {listaResponsaveis.map(r => (
                  <option key={r.id} value={r.id}>
                    {r.nome}{r.apartamento ? ` — Apt ${r.apartamento}` : r.cargo ? ` — ${r.cargo}` : ''}
                  </option>
                ))}
              </select>
            </div>
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn-ghost" onClick={() => setModal(null)}>Cancelar</button>
            <button type="submit" className="btn btn-amber" disabled={loading || !retirarForm.responsavelId}>
              {loading ? 'Processando...' : '🔑 Confirmar Retirada'}
            </button>
          </div>
        </form>
      </Modal>

      {toast && <Toast {...toast} onClose={() => setToast(null)} />}
    </div>
  )
}
