import { receitaDaPlataforma } from '../clients/planClient.js';
import { listarCondominios } from '../clients/authClient.js';

/**
 * Receita da plataforma com as assinaturas dos condomínios.
 *
 * Este é o outro fluxo de dinheiro do Mora: o `financeiro-service` cuida do que
 * o morador paga ao condomínio; aqui é o que o condomínio paga à plataforma.
 *
 * A conta é do plan-service, no banco: a função `receita_plataforma` devolve
 * indicadores, receita por plano, evolução mensal e o resumo por cliente, em
 * centavos. Aqui fica só o que depende de outro banco: o nome de cada
 * condomínio e quem não assina nada, que vêm do auth-api. São bancos
 * diferentes, então esse cruzamento não pode ser um JOIN.
 */

/** Normaliza a janela pedida: query string é texto não confiável. */
function janelaDeMeses(meses) {
  const n = Number(meses);
  if (!Number.isFinite(n)) return 12;
  return Math.min(36, Math.max(1, Math.trunc(n)));
}

export async function montarReceita(authorization, filtros = {}) {
  const meses = janelaDeMeses(filtros.meses);
  const [receitaR, condominiosR] = await Promise.all([
    receitaDaPlataforma({ meses, plano: filtros.plano }, authorization),
    listarCondominios(authorization),
  ]);

  // Sem o plan-service não há receita para mostrar — devolver zeros diria que a
  // plataforma não fatura nada, que é diferente de "não consegui consultar".
  if (!receitaR.ok) {
    return {
      erro: true,
      status: 503,
      mensagem: 'Não foi possível consultar as assinaturas. Tente novamente.',
    };
  }

  const fontesIndisponiveis = [];
  if (!condominiosR.ok) fontesIndisponiveis.push('auth-api');

  const receita = receitaR.dados;
  const listaCondominios = condominiosR.ok
    ? (condominiosR.dados?.condominios ?? condominiosR.dados ?? [])
    : [];
  const nomePorId = new Map(listaCondominios.map((c) => [c.id, c.nome]));

  // Cliente sem assinatura é receita que existe e não está sendo cobrada — é o
  // número que o Admin Geral precisa ver primeiro.
  //
  // Sobre TODAS as assinaturas, nunca sobre o plano filtrado: com um filtro
  // ativo, quem assina outro plano apareceria aqui como se não pagasse nada.
  const comAssinatura = new Set(receita.condominiosComAssinatura ?? []);
  const semAssinatura = listaCondominios
    .filter((c) => !comAssinatura.has(c.id))
    .map((c) => ({ condominioId: c.id, nome: c.nome }));

  const { mrrCentavos = 0 } = receita.indicadores ?? {};

  return {
    sucesso: true,
    geradoEm: new Date().toISOString(),
    filtros: receita.filtros,
    planosDisponiveis: receita.planosDisponiveis ?? [],
    indicadores: {
      ...receita.indicadores,
      arrCentavos: mrrCentavos * 12,
      clientesSemAssinatura: semAssinatura.length,
    },
    porPlano: receita.porPlano ?? [],
    evolucao: receita.evolucao ?? [],
    clientes: (receita.clientes ?? []).map((c) => ({
      ...c,
      nome: nomePorId.get(c.condominioId) ?? c.condominioId,
    })),
    semAssinatura,
    fontesIndisponiveis,
  };
}
