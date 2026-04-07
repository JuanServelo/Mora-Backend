import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api/portariaApi'

function StatCard({ icon, value, label, sub, colorClass, onClick }) {
  return (
    <div className="stat-card" style={{ cursor: onClick ? 'pointer' : 'default' }} onClick={onClick}>
      <div className="stat-header">
        <div className={`stat-icon ${colorClass}`}>{icon}</div>
      </div>
      <div className="stat-value">{value ?? '—'}</div>
      <div className="stat-label">{label}</div>
      {sub && <div className="stat-sub">{sub}</div>}
    </div>
  )
}

function ActivityRow({ icon, text, sub, color }) {
  return (
    <div style={{ display:'flex', alignItems:'center', gap:12, padding:'10px 0', borderBottom:'1px solid var(--border)' }}>
      <div className={`stat-icon ${color}`} style={{ width:32, height:32, fontSize:'0.85rem', borderRadius:8, flexShrink:0 }}>{icon}</div>
      <div style={{ flex:1 }}>
        <div style={{ fontSize:'0.82rem', fontWeight:500 }}>{text}</div>
        <div style={{ fontSize:'0.72rem', color:'var(--text-muted)', marginTop:2 }}>{sub}</div>
      </div>
    </div>
  )
}

export default function Dashboard() {
  const navigate = useNavigate()
  const [data, setData] = useState({
    visitantesDentro: null,
    carrosDentro: null,
    entregasPendentes: null,
    chavesDisponiveis: null,
    totalMoradores: null,
    turnosAtivos: null,
  })
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    Promise.allSettled([
      api.visitantes.dentro(),
      api.carros.dentro(),
      api.entregas.pendentes(),
      api.chaves.disponiveis(),
      api.moradores.listar(),
      api.turnos.listar(),
    ]).then(([vis, car, ent, cha, mor, tur]) => {
      const turnosList = tur.value ?? []
      const ativos = turnosList.filter(t => t.entradas?.length > (t.saidas?.length ?? 0)).length
      setData({
        visitantesDentro:  vis.value?.length ?? 0,
        carrosDentro:      car.value?.length ?? 0,
        entregasPendentes: ent.value?.length ?? 0,
        chavesDisponiveis: cha.value?.length ?? 0,
        totalMoradores:    mor.value?.length ?? 0,
        turnosAtivos:      ativos,
      })
      setLoading(false)
    })
  }, [])

  const { visitantesDentro, carrosDentro, entregasPendentes, chavesDisponiveis, totalMoradores, turnosAtivos } = data

  return (
    <div>
      <div className="page-header">
        <h1>Dashboard</h1>
        <p>Visão geral em tempo real do condomínio</p>
      </div>

      {/* Info cards - Portaria Central style */}
      <div className="info-cards-row">
        <div className="info-card">
          <div className="left">
            <div className="icon-box icon-blue">🛡️</div>
            <div>
              <div className="title">Portaria Central</div>
              <div className="status-row" style={{ marginTop:4 }}>
                <span className="online-dot" />
                <span style={{ fontSize:'0.72rem', color:'var(--text-muted)' }}>Disponível 24/7 · Ramal 9001</span>
              </div>
            </div>
          </div>
          <div className="actions">
            <div className="action-btn" title="Ligar">📞</div>
            <div className="action-btn" title="Mensagem">💬</div>
          </div>
        </div>

        <div className="info-card">
          <div className="left">
            <div style={{ width:44, height:44, borderRadius:'50%', background:'var(--bg-3)', display:'flex', alignItems:'center', justifyContent:'center', fontSize:'1.2rem', position:'relative' }}>
              👤
              <span className="online-dot" style={{ position:'absolute', bottom:1, right:1, width:10, height:10, border:'2px solid var(--bg-1)' }} />
            </div>
            <div>
              <div className="title" style={{ display:'flex', alignItems:'center', gap:8 }}>
                Sistema Portaria
                <span className="badge badge-amber">Admin</span>
              </div>
              <div style={{ fontSize:'0.72rem', color:'var(--text-muted)', marginTop:2 }}>Atendimento seg à sex, 08h – 18h</div>
            </div>
          </div>
          <div className="actions">
            <div className="action-btn" title="Email">✉️</div>
            <div className="action-btn" title="Agenda">📅</div>
          </div>
        </div>
      </div>

      {/* Stats */}
      <div className="stat-grid">
        <StatCard icon="🚶" value={visitantesDentro} label="Visitantes no Condomínio"
          sub="entradas registradas hoje" colorClass="icon-blue"
          onClick={() => navigate('/visitantes')} />
        <StatCard icon="🚗" value={carrosDentro} label="Veículos no Condomínio"
          sub="na área de estacionamento" colorClass="icon-purple"
          onClick={() => navigate('/carros')} />
        <StatCard icon="📦" value={entregasPendentes} label="Entregas Pendentes"
          sub="aguardando retirada" colorClass="icon-amber"
          onClick={() => navigate('/entregas')} />
        <StatCard icon="🔑" value={chavesDisponiveis} label="Chaves Disponíveis"
          sub="prontas para retirada" colorClass="icon-green"
          onClick={() => navigate('/chaves')} />
        <StatCard icon="⌂" value={totalMoradores} label="Moradores Ativos"
          sub="cadastrados no sistema" colorClass="icon-blue"
          onClick={() => navigate('/moradores')} />
        <StatCard icon="🕐" value={turnosAtivos} label="Turnos em Andamento"
          sub="funcionários on-duty" colorClass="icon-green"
          onClick={() => navigate('/turnos')} />
      </div>

      {/* Quick actions */}
      <div className="divider" />
      <div style={{ display:'flex', gap:10, flexWrap:'wrap' }}>
        <button className="btn btn-primary" onClick={() => navigate('/visitantes')}>
          👥 Registrar Visitante
        </button>
        <button className="btn btn-ghost" onClick={() => navigate('/carros')}>
          🚗 Registrar Veículo
        </button>
        <button className="btn btn-ghost" onClick={() => navigate('/entregas')}>
          📦 Nova Entrega
        </button>
        <button className="btn btn-amber" onClick={() => navigate('/chaves')}>
          🔑 Controle de Chaves
        </button>
      </div>
    </div>
  )
}
