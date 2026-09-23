import { listarAssinaturas } from '../clients/planClient.js';
import { listarCondominios } from '../clients/authClient.js';

/**
 * Receita da plataforma com as assinaturas dos condomínios.
 *
 * Este é o outro fluxo de dinheiro do Mora: o `financeiro-service` cuida do que
 * o morador paga ao condomínio; aqui é o que o condomínio paga à plataforma.
 *
 * Valores em centavos, inteiros. O plan-service usa `BigDecimal` e serializa
 * como número decimal (200.00); a conversão acontece na entrada, e daqui para
 * frente toda soma é exata.
 */

const ATIVA = 'ATIVA';

/** 200.00 → 20000. Arredonda porque JSON já entregou um float. */
const paraCentavos = (valor) =>
  valor == null ? 0 : Math.round(Number(valor) * 100);

/** `2026-09` a partir de uma data ISO ou `YYYY-MM-DD`. */
const competenciaDe = (data) => String(data ?? '').slice(0, 7);

const MESES_PADRAO = 12;

/** Normaliza a janela pedida: query string é texto não confiável. */
function janelaDeMeses(meses) {
  const n = Number(meses);
  if (!Number.isFinite(n)) return MESES_PADRAO;
  return Math.min(36, Math.max(1, Math.trunc(n)));
}

/** Os últimos `n` meses, terminando no corrente, como `YYYY-MM`. */
function ultimosMeses(n, hoje = new Date()) {
  const saida = [];
  for (let i = n - 1; i >= 0; i--) {
    const d = new Date(hoje.getFullYear(), hoje.getMonth() - i, 1);
    saida.push(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`);
  }
  return saida;
}

/** Assinatura estava valendo naquele mês? */
function vigenteEm(assinatura, mes) {
  const inicio = competenciaDe(assinatura.vigenciaInicio);
  if (!inicio || inicio > mes) return false;
  const fim = competenciaDe(assinatura.vigenciaFim);
  return !fim || fim >= mes;
}

export async function montarReceita(authorization, filtros = {}) {
  const janela = janelaDeMeses(filtros.meses);
  const [assinaturasR, condominiosR] = await Promise.all([
    listarAssinaturas(authorization),
    listarCondominios(authorization),
  ]);

  const fontesIndisponiveis = [];
  if (!assinaturasR.ok) fontesIndisponiveis.push('plan-service');
  if (!condominiosR.ok) fontesIndisponiveis.push('auth-api');

  // Sem o plan-service não há receita para mostrar — devolver zeros diria que a
  // plataforma não fatura nada, que é diferente de "não consegui consultar".
  if (!assinaturasR.ok) {
    return {
      erro: true,
      status: 503,
      mensagem: 'Não foi possível consultar as assinaturas. Tente novamente.',
    };
  }

  const assinaturas = assinaturasR.dados;
  const listaCondominios = condominiosR.ok
    ? (condominiosR.dados?.condominios ?? condominiosR.dados ?? [])
    : [];
  const nomePorId = new Map(listaCondominios.map((c) => [c.id, c.nome]));

  // O filtro de plano recorta a base ANTES de agregar — os indicadores passam
  // a descrever só o plano escolhido, em vez de mostrar o total e destacar uma
  // fatia. Comparar "receita do plano X" com "receita total" no mesmo cartão
  // seria enganoso.
  const planoFiltrado = filtros.plano && filtros.plano !== 'todos' ? filtros.plano : null;
  const consideradas = planoFiltrado
    ? assinaturas.filter((a) => a.planNome === planoFiltrado)
    : assinaturas;

  const ativas = consideradas.filter((a) => a.status === ATIVA);

  // ── Receita recorrente ────────────────────────────────────────────────────
  const mrrCentavos = ativas.reduce((s, a) => s + paraCentavos(a.mensalidade), 0);

  // ── Por plano ─────────────────────────────────────────────────────────────
  const porPlanoMapa = new Map();
  for (const a of ativas) {
    const chave = a.planNome ?? 'Sem nome';
    const atual = porPlanoMapa.get(chave) ?? { plano: chave, assinaturas: 0, mrrCentavos: 0 };
    atual.assinaturas += 1;
    atual.mrrCentavos += paraCentavos(a.mensalidade);
    porPlanoMapa.set(chave, atual);
  }
  const porPlano = [...porPlanoMapa.values()].sort((a, b) => b.mrrCentavos - a.mrrCentavos);

  // ── Evolução na janela pedida ─────────────────────────────────────────────
  //
  // Reconstruída da vigência de cada assinatura, não de um histórico gravado.
  // Consequência: assinatura excluída some da série inteira, como se nunca
  // tivesse existido. Um snapshot mensal resolveria, e não existe hoje.
  const evolucao = ultimosMeses(janela).map((mes) => {
    const noMes = consideradas.filter((a) => vigenteEm(a, mes));
    return {
      mes,
      clientes: noMes.length,
      mrrCentavos: noMes.reduce((s, a) => s + paraCentavos(a.mensalidade), 0),
    };
  });

  // ── Por cliente ───────────────────────────────────────────────────────────
  const porCondominio = new Map();
  for (const a of consideradas) {
    // Se houver mais de uma, a ativa manda; senão a mais recente serve.
    const atual = porCondominio.get(a.condominioId);
    if (!atual || (a.status === ATIVA && atual.status !== ATIVA)) {
      porCondominio.set(a.condominioId, a);
    }
  }

  const clientes = [...porCondominio.entries()].map(([condominioId, a]) => ({
    condominioId,
    nome: nomePorId.get(condominioId) ?? condominioId,
    plano: a.planNome,
    status: a.status,
    vigente: a.vigente,
    desde: a.vigenciaInicio ?? null,
    mensalidadeCentavos: paraCentavos(a.mensalidade),
    // Quanto esse cliente já rendeu desde que assinou. Aproximação: mensalidade
    // vezes os meses decorridos, porque não há histórico de pagamento da
    // assinatura — o plan-service guarda o contrato, não as cobranças.
    acumuladoCentavos: paraCentavos(a.mensalidade) * mesesDesde(a.vigenciaInicio),
  })).sort((x, y) => y.mensalidadeCentavos - x.mensalidadeCentavos);

  // Cliente sem assinatura é receita que existe e não está sendo cobrada — é o
  // número que o Admin Geral precisa ver primeiro.
  //
  // Calculado sobre TODAS as assinaturas, nunca sobre a base filtrada: com um
  // filtro de plano ativo, quem assina outro plano apareceria aqui como se não
  // pagasse nada.
  const comQualquerAssinatura = new Set(assinaturas.map((a) => a.condominioId));
  const semAssinatura = listaCondominios
    .filter((c) => !comQualquerAssinatura.has(c.id))
    .map((c) => ({ condominioId: c.id, nome: c.nome }));

  return {
    sucesso: true,
    geradoEm: new Date().toISOString(),
    filtros: { meses: janela, plano: planoFiltrado ?? 'todos' },
    // Sempre a lista completa, mesmo com filtro ativo: montada da base
    // filtrada, o seletor ficaria preso no plano escolhido.
    planosDisponiveis: [...new Set(assinaturas.map((a) => a.planNome).filter(Boolean))].sort(),
    indicadores: {
      mrrCentavos,
      arrCentavos: mrrCentavos * 12,
      clientesAtivos: ativas.length,
      clientesSemAssinatura: semAssinatura.length,
      ticketMedioCentavos: ativas.length ? Math.round(mrrCentavos / ativas.length) : 0,
      acumuladoCentavos: clientes.reduce((s, c) => s + c.acumuladoCentavos, 0),
    },
    porPlano,
    evolucao,
    clientes,
    semAssinatura,
    fontesIndisponiveis,
  };
}

/** Meses completos entre o início da vigência e hoje, no mínimo 1. */
function mesesDesde(inicio) {
  const i = competenciaDe(inicio);
  if (!i) return 0;
  const [ano, mes] = i.split('-').map(Number);
  const hoje = new Date();
  const meses = (hoje.getFullYear() - ano) * 12 + (hoje.getMonth() + 1 - mes) + 1;
  return Math.max(1, meses);
}
