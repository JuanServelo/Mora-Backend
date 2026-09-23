// Gera histórico financeiro de um condomínio: seis competências, com contas de
// consumo, faturas emitidas, meses quitados, um inadimplente e o mês corrente
// ainda aberto.
//
//   node scripts/seed-historico.js [condominioId] [emailSindico]
//
// Usa os mesmos endpoints da tela — nada é escrito direto no banco, exceto a
// marcação de atraso, que é a mesma chamada do job diário.
import 'dotenv/config';
import * as faturasModel from '../models/faturasModel.js';

const AUTH = process.env.AUTH_API_URL || 'http://localhost:3001';
const FIN = `http://localhost:${process.env.PORT || 3004}/api/financeiro`;

const CONDOMINIO = process.argv[2] || 'cond-parque-verde';
const EMAIL = process.argv[3] || 'sindico.verde@mora.test';
const SENHA = process.env.SENHA_SEED || 'Mora@2024';

/**
 * Seis meses até a competência corrente.
 *
 * Os valores variam de propósito: conta de luz igual todo mês não mostra nada
 * num gráfico de evolução de gastos, que é justamente o que estas telas exibem.
 */
const COMPETENCIAS = [
  { comp: '2026-04', agua: '820,00', luz: '1.340,00', quitada: true },
  { comp: '2026-05', agua: '910,50', luz: '1.180,00', quitada: true },
  { comp: '2026-06', agua: '1.045,00', luz: '1.620,00', quitada: true },
  { comp: '2026-07', agua: '1.230,00', luz: '1.890,00', quitada: true },
  // Agosto fica com uma unidade em aberto — é o que alimenta a inadimplência.
  { comp: '2026-08', agua: '1.115,00', luz: '1.705,00', quitada: 'parcial' },
  // Setembro é o mês corrente: emitido e ainda dentro do prazo.
  { comp: '2026-09', agua: '980,00', luz: '1.420,00', quitada: false },
];

let token;

async function api(caminho, opts = {}) {
  const r = await fetch(FIN + caminho, {
    ...opts,
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
      ...(opts.headers || {}),
    },
    body: opts.body ? JSON.stringify(opts.body) : undefined,
  });
  const j = await r.json().catch(() => ({}));
  if (!r.ok) throw new Error(`${opts.method || 'GET'} ${caminho} → ${r.status}: ${j.mensagem || JSON.stringify(j)}`);
  return j;
}

/** Vencimento da competência, no dia 10 — o mesmo que as regras usam. */
const vencimentoDe = (comp) => `${comp}-10`;

/** Data de pagamento plausível: alguns dias antes do vencimento. */
const pagoEmDe = (comp, dias) => `${comp}-${String(10 - dias).padStart(2, '0')}T14:30:00.000Z`;

async function entrar() {
  const r = await fetch(`${AUTH}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: EMAIL, senha: SENHA }),
  });
  const j = await r.json().catch(() => ({}));
  if (!j.token) throw new Error(`Login falhou para ${EMAIL}: ${j.mensagem || r.status}`);
  return j.token;
}

async function main() {
  token = await entrar();
  console.log(`\n═══ histórico financeiro — ${CONDOMINIO} ═══\n`);

  for (const m of COMPETENCIAS) {
    // As contas precisam existir antes do fechamento: é ele que as transforma
    // em itens da fatura. Criadas depois, ficariam para o mês seguinte.
    for (const [tipo, valor] of [['AGUA', m.agua], ['LUZ', m.luz]]) {
      await api('/contas-consumo', {
        method: 'POST',
        body: {
          tipo,
          descricao: tipo === 'AGUA' ? 'Consumo de água do prédio' : 'Energia das áreas comuns',
          competencia: m.comp,
          valor,
          modoRateio: 'FRACAO_IDEAL',
          vencimento: vencimentoDe(m.comp),
        },
      });
    }

    const r = await api('/fechar-competencia', { method: 'POST', body: { competencia: m.comp } });
    console.log(`${m.comp}  faturas: ${r.faturasCriadas}  (água ${m.agua} · luz ${m.luz})`);

    if (m.quitada === false) continue;

    const { faturas: itens } = await api(`/admin/faturas?competencia=${m.comp}`);
    // No mês parcial, a última unidade fica sem pagar de propósito.
    const aQuitar = m.quitada === 'parcial' ? itens.slice(0, -1) : itens;

    for (const [i, f] of aQuitar.entries()) {
      await api(`/admin/faturas/${f.id}/baixa-manual`, {
        method: 'PATCH',
        body: { pagoEm: pagoEmDe(m.comp, (i % 5) + 1) },
      });
    }
    const sobra = itens.length - aQuitar.length;
    console.log(`          quitadas: ${aQuitar.length}${sobra ? `  ·  ${sobra} deixada em aberto` : ''}`);
  }

  // Mesma chamada do job diário — vencidas e ainda abertas viram EM_ATRASO.
  const atrasadas = await faturasModel.marcarEmAtraso();
  console.log(`\nmarcadas EM_ATRASO: ${atrasadas.length}`);

  const kpis = await api('/admin/faturas/kpis?competencia=2026-09');
  console.log('\nKPIs:', JSON.stringify(kpis, null, 2));
  console.log('');
}

main().then(() => process.exit(0)).catch((e) => {
  console.error('\n❌', e.message);
  process.exit(1);
});
