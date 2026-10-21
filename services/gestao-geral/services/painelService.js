import * as auth from '../clients/authClient.js';
import * as portaria from '../clients/portariaClient.js';
import * as comunicacao from '../clients/comunicacaoClient.js';
import * as financeiro from '../clients/financeiroClient.js';

/**
 * Painéis do síndico (RF-18): operacional, o que pede ação agora; estratégico,
 * como o condomínio vem andando mês a mês.
 *
 * Duas metades separadas de propósito. A busca fala com cinco serviços e
 * tolera que qualquer um esteja fora; a agregação (`resumir*`) só recebe os
 * dados e devolve os blocos — é ela que os testes exercitam, sem rede.
 *
 * Cada bloco é independente: fonte fora do ar vira bloco `null` e entra em
 * `fontesIndisponiveis`, e o resto do painel aparece normalmente.
 *
 * Agregação no banco (RNF-20): ocorrências (auth-api), leitura de avisos
 * (comunicacao) e multas e faturas (financeiro) chegam já contadas, em SQL, por
 * quem é dono do dado. Só o portaria ainda é contado aqui, sobre as listas que
 * ele devolve — falta a ele um endpoint de contagem. O contrato deste endpoint
 * não muda quando isso for feito.
 */

const FUSO = 'America/Sao_Paulo';

/** "YYYY-MM-DD" de hoje no fuso do condomínio. */
export const hojeLocal = (agora = new Date()) => agora.toLocaleDateString('sv-SE', { timeZone: FUSO });

/**
 * Mês "YYYY-MM" de uma data vinda de qualquer fonte.
 *
 * O portaria devolve hora local sem fuso ("2026-10-14T14:00:00"): é o mês que
 * está escrito. O auth-api devolve UTC ("...Z"): convertido para o fuso do
 * condomínio, senão uma ocorrência aberta às 22h do dia 31 cairia no mês
 * seguinte.
 */
export function mesDe(valor) {
  if (!valor) return null;
  const texto = String(valor);
  if (/^\d{4}-\d{2}-\d{2}(T[\d:.]+)?$/.test(texto)) return texto.slice(0, 7);
  const d = new Date(texto);
  if (Number.isNaN(d.getTime())) return null;
  return d.toLocaleDateString('sv-SE', { timeZone: FUSO }).slice(0, 7);
}

/** Os últimos `n` meses, do mais antigo ao atual, como "YYYY-MM". */
export function ultimosMeses(n, agora = new Date()) {
  const [ano, mes] = hojeLocal(agora).split('-').map(Number);
  const meses = [];
  for (let i = n - 1; i >= 0; i--) {
    const total = ano * 12 + (mes - 1) - i;
    meses.push(`${Math.floor(total / 12)}-${String((total % 12) + 1).padStart(2, '0')}`);
  }
  return meses;
}

/**
 * Quantas pessoas deveriam ler um aviso, pelo público dele.
 *
 * A gestão nunca entra: ela publica o comunicado, não é destinatária. Mesma
 * regra do comunicacao-service para decidir quem recebe.
 */
export function destinatariosDoAviso(publicoAlvo, porPerfil = {}) {
  const moradores = (porPerfil.MORADOR ?? 0) + (porPerfil.DONO_ALUGUEL ?? 0);
  const funcionarios = porPerfil.PORTEIRO ?? 0;
  if (publicoAlvo === 'MORADORES') return moradores;
  if (publicoAlvo === 'FUNCIONARIOS') return funcionarios;
  if (publicoAlvo === 'SINDICO') return 0;
  return moradores + funcionarios;
}

const RESERVA_CONTA = (r) => !['CANCELADA', 'RECUSADA', 'EXPIRADA'].includes(r.status);
const menor = (valores) => valores.filter(Boolean).sort()[0] ?? null;

// ── Operacional ─────────────────────────────────────────────────────────────

/**
 * Situação de agora. Cada argumento é o que a fonte devolveu, ou `null` se ela
 * não respondeu.
 *
 * - `ocorrencias`: a situação atual agregada pelo auth-api
 *   (`{ abertas, pendentes, emAnalise, maisAntigaDesde }`);
 * - `avisos`: o panorama do comunicacao — só os vigentes, cada um com
 *   `confirmacoes`.
 */
export function resumirOperacional(fontes) {
  const {
    entregas, reservasPendentes, pessoasDentro, visitantesDentro,
    ocorrencias, conversas, avisos, porPerfil, kpis,
  } = fontes;

  const entregasBloco = entregas && {
    aguardandoRetirada: entregas.length,
    maisAntigaDesde: menor(entregas.map((e) => e.dataRecebimento)),
  };

  const reservasBloco = reservasPendentes && {
    aguardandoAprovacao: reservasPendentes.length,
    // A que vence primeiro é a que o síndico precisa olhar antes.
    proximoPrazo: menor(reservasPendentes.map((r) => r.prazoDecisao)),
  };

  const presencaBloco = (pessoasDentro || visitantesDentro) && {
    atendimentosAbertos: pessoasDentro ? pessoasDentro.length : null,
    visitantesDentro: visitantesDentro ? visitantesDentro.length : null,
  };

  const ocorrenciasBloco = ocorrencias && {
    abertas: ocorrencias.abertas ?? 0,
    pendentes: ocorrencias.pendentes ?? 0,
    emAnalise: ocorrencias.emAnalise ?? 0,
    maisAntigaDesde: ocorrencias.maisAntigaDesde ?? null,
  };

  const conversasBloco = conversas && {
    abertas: conversas.filter((c) => !c.encerradaEm).length,
    comMensagemNova: conversas.filter((c) => !c.encerradaEm && c.naoLidas > 0).length,
  };

  let avisosBloco = null;
  if (avisos) {
    avisosBloco = {
      vigentes: avisos.length,
      // Sem a contagem de usuários, a taxa não tem denominador. Melhor não
      // mostrar porcentagem nenhuma do que mostrar uma inventada.
      leitura: avisos.map((a) => {
        const lidas = a.confirmacoes ?? null;
        const total = porPerfil ? destinatariosDoAviso(a.publicoAlvo, porPerfil) : null;
        return {
          id: a.id,
          titulo: a.titulo,
          dataFim: a.dataFim,
          leituras: lidas,
          destinatarios: total,
          percentual: lidas !== null && total ? Math.min(100, Math.round((lidas / total) * 100)) : null,
        };
      }).sort((x, y) => (x.percentual ?? 101) - (y.percentual ?? 101)),
    };
  }

  const financeiroBloco = kpis && {
    faturasEmAtraso: kpis.emAtraso ?? 0,
    faturasAbertas: kpis.abertas ?? 0,
    totalCentavos: kpis.totalCentavos ?? 0,
    recebidoCentavos: kpis.pagasCentavos ?? 0,
  };

  return {
    entregas: entregasBloco || null,
    reservas: reservasBloco || null,
    presenca: presencaBloco || null,
    ocorrencias: ocorrenciasBloco || null,
    conversas: conversasBloco || null,
    avisos: avisosBloco,
    financeiro: financeiroBloco || null,
  };
}

// ── Estratégico ─────────────────────────────────────────────────────────────

/**
 * Séries mensais da janela `meses`. Mesma convenção de `null` por fonte.
 *
 * `ocorrencias` e `multasPorMes` chegam agregadas por mês; mês sem linha vira
 * zero. As séries seguem a janela daqui, e não a da fonte, para todos os
 * gráficos do painel terem os mesmos meses.
 */
export function resumirEstrategico({ reservas, ocorrencias, kpisPorMes, multasPorMes }, meses) {
  const naJanela = (mes) => mes && meses.includes(mes);
  const serie = (fn) => meses.map((mes) => ({ mes, ...fn(mes) }));

  let reservasBloco = null;
  if (reservas) {
    const validas = reservas.filter(RESERVA_CONTA).filter((r) => naJanela(mesDe(r.inicio)));
    const porArea = new Map();
    for (const r of validas) {
      const nome = r.areaComumNome ?? 'Sem nome';
      porArea.set(nome, (porArea.get(nome) ?? 0) + 1);
    }
    reservasBloco = {
      porMes: serie((mes) => ({ total: validas.filter((r) => mesDe(r.inicio) === mes).length })),
      porArea: [...porArea].map(([area, total]) => ({ area, total })).sort((a, b) => b.total - a.total),
    };
  }

  let ocorrenciasBloco = null;
  if (ocorrencias) {
    const doMes = new Map((ocorrencias.porMes ?? []).map((m) => [m.mes, m]));
    ocorrenciasBloco = {
      porMes: serie((mes) => ({
        abertas: doMes.get(mes)?.abertas ?? 0,
        resolvidas: doMes.get(mes)?.resolvidas ?? 0,
      })),
      porCategoria: ocorrencias.porCategoria ?? [],
      // Calculado no auth-api, com o `updatedAt` da resolvida como data de
      // resolução — a ocorrência não guarda quando foi resolvida.
      tempoMedioResolucaoDias: ocorrencias.tempoMedioResolucaoDias ?? null,
    };
  }

  // Um mês sem resposta fica com valores nulos; os outros seguem valendo.
  const inadimplenciaBloco = kpisPorMes && {
    porMes: meses.map((mes) => {
      const k = kpisPorMes[mes];
      if (!k) return { mes, faturas: null, emAtraso: null, taxa: null, recebidoCentavos: null };
      return {
        mes,
        faturas: k.total ?? 0,
        emAtraso: k.emAtraso ?? 0,
        taxa: k.total ? Math.round(((k.emAtraso ?? 0) / k.total) * 1000) / 10 : null,
        recebidoCentavos: k.pagasCentavos ?? 0,
      };
    }),
  };

  let multasBloco = null;
  if (multasPorMes) {
    const doMes = new Map(multasPorMes.map((m) => [m.mes, m]));
    multasBloco = {
      porMes: serie((mes) => {
        const m = doMes.get(mes);
        return {
          aplicadas: m?.aplicadas ?? 0,
          canceladas: m?.canceladas ?? 0,
          // A soma é `bigint` no Postgres, e o driver a entrega como texto.
          valorCentavos: Number(m?.valorCentavos ?? 0),
        };
      }),
    };
  }

  return {
    reservas: reservasBloco,
    ocorrencias: ocorrenciasBloco,
    inadimplencia: inadimplenciaBloco || null,
    multas: multasBloco,
  };
}

// ── Busca ───────────────────────────────────────────────────────────────────

/** Lista de uma resposta, ou `null` se a fonte falhou. Aceita envelope. */
const lista = (r, chave) => {
  if (!r.ok) return null;
  const d = chave ? r.dados?.[chave] : r.dados;
  return Array.isArray(d) ? d : null;
};

/** Objeto de uma resposta, ou `null` se a fonte falhou ou veio sem ele. */
const objeto = (r, chave) => {
  if (!r.ok) return null;
  const d = chave ? r.dados?.[chave] : r.dados;
  return d && typeof d === 'object' && !Array.isArray(d) ? d : null;
};

export async function montarOperacional(condominioId, authorization) {
  const competencia = hojeLocal().slice(0, 7);

  const [entregas, pendentes, dentro, visitantes, ocorrencias, conversas, panorama, resumo, kpis] =
    await Promise.all([
      portaria.listar('/entregas/pendentes', authorization),
      portaria.listar('/reservas/pendentes', authorization),
      portaria.listar('/atendimento/dentro', authorization),
      portaria.listar('/visitantes/dentro', authorization),
      // Aqui só a situação atual interessa; a menor janela poupa as séries.
      auth.estatisticasReclamacoes(1, authorization),
      comunicacao.listarConversas(authorization),
      comunicacao.panoramaDeLeitura(authorization),
      auth.resumoCondominio(condominioId, authorization),
      financeiro.kpisDaCompetencia(competencia, authorization),
    ]);

  const fontesIndisponiveis = new Set();
  if (![entregas, pendentes, dentro, visitantes].every((r) => r.ok)) fontesIndisponiveis.add('portaria-service');
  if (!ocorrencias.ok || !resumo.ok) fontesIndisponiveis.add('auth-api');
  if (!conversas.ok || !panorama.ok) fontesIndisponiveis.add('comunicacao-service');
  if (!kpis.ok) fontesIndisponiveis.add('financeiro');

  return {
    sucesso: true,
    geradoEm: new Date().toISOString(),
    condominioId,
    ...resumirOperacional({
      entregas: lista(entregas),
      reservasPendentes: lista(pendentes),
      pessoasDentro: lista(dentro),
      visitantesDentro: lista(visitantes),
      ocorrencias: objeto(ocorrencias, 'atual'),
      conversas: lista(conversas, 'conversas'),
      avisos: lista(panorama, 'avisos'),
      porPerfil: resumo.ok ? (resumo.dados?.resumo?.usuarios?.porPerfil ?? null) : null,
      kpis: kpis.ok ? kpis.dados : null,
    }),
    fontesIndisponiveis: [...fontesIndisponiveis],
  };
}

export async function montarEstrategico(condominioId, numeroMeses, authorization) {
  const meses = ultimosMeses(numeroMeses);

  const [reservas, ocorrencias, multas, ...kpis] = await Promise.all([
    portaria.listar('/reservas', authorization),
    auth.estatisticasReclamacoes(numeroMeses, authorization),
    financeiro.multasPorMes(numeroMeses, authorization),
    ...meses.map((mes) => financeiro.kpisDaCompetencia(mes, authorization)),
  ]);

  const kpisOk = kpis.some((k) => k.ok);
  const kpisPorMes = kpisOk
    ? Object.fromEntries(meses.map((mes, i) => [mes, kpis[i].ok ? kpis[i].dados : null]))
    : null;

  const fontesIndisponiveis = new Set();
  if (!reservas.ok) fontesIndisponiveis.add('portaria-service');
  if (!ocorrencias.ok) fontesIndisponiveis.add('auth-api');
  if (!multas.ok || !kpis.every((k) => k.ok)) fontesIndisponiveis.add('financeiro');

  return {
    sucesso: true,
    geradoEm: new Date().toISOString(),
    condominioId,
    meses,
    ...resumirEstrategico({
      reservas: lista(reservas),
      ocorrencias: objeto(ocorrencias),
      kpisPorMes,
      multasPorMes: lista(multas, 'meses'),
    }, meses),
    // Assembleias (RF-11) ficam de fora até o meeting-service autenticar e
    // filtrar por condomínio: consultá-lo hoje traria dado de outros clientes.
    fontesIndisponiveis: [...fontesIndisponiveis],
  };
}
