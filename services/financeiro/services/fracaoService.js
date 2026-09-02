import { emTransacao } from '../config/database.js';
import * as fracoes from '../models/fracaoModel.js';
import { listarUnidades } from '../clients/portariaClient.js';
import { ratear } from '../utils/dinheiro.js';

/**
 * Por convenção de condomínio, as frações somam 1000 milésimos.
 *
 * O rateio em si não depende disso — ele divide pela soma real, qualquer que
 * seja. O total redondo serve de conferência: se não fecha em 1000, alguém
 * digitou errado, e é melhor o síndico ver antes de emitir 200 faturas.
 */
export const SOMA_ESPERADA = 1000;

const erro = (mensagem, status = 400) => ({ erro: true, mensagem, status });

/**
 * Junta as unidades do portaria com as frações guardadas aqui.
 *
 * A lista sai completa de propósito, inclusive as unidades sem fração: é a
 * ausência que o síndico precisa enxergar, e uma listagem só do que existe
 * esconderia justamente o que falta.
 */
export async function listar(condominioId, authorization) {
  const unidades = await listarUnidades(condominioId, authorization);
  if (!unidades.ok) {
    return erro('Não foi possível consultar as unidades do condomínio.', 503);
  }

  const porUnidade = new Map(
    (await fracoes.listarPorCondominio(condominioId)).map((f) => [f.unidadeId, f.milesimos]),
  );

  const itens = unidades.dados.map((u) => ({
    ...u,
    milesimos: porUnidade.get(u.id) ?? null,
  }));

  const comFracao = itens.filter((i) => i.milesimos !== null);
  const soma = comFracao.reduce((a, i) => a + i.milesimos, 0);
  const faltando = itens.filter((i) => i.milesimos === null);

  return {
    itens,
    resumo: {
      unidades: itens.length,
      comFracao: comFracao.length,
      faltando: faltando.length,
      soma,
      somaEsperada: SOMA_ESPERADA,
      completo: faltando.length === 0 && soma === SOMA_ESPERADA,
    },
  };
}

export async function definir(condominioId, unidadeId, valor) {
  const milesimos = Number(valor);
  if (!Number.isInteger(milesimos) || milesimos <= 0) {
    return erro('A fração precisa ser um número inteiro de milésimos maior que zero.');
  }
  if (milesimos > SOMA_ESPERADA) {
    return erro(`Uma unidade não pode ter mais que ${SOMA_ESPERADA} milésimos.`);
  }
  return fracoes.definir(condominioId, unidadeId, milesimos);
}

export async function remover(condominioId, unidadeId) {
  const removeu = await fracoes.remover(condominioId, unidadeId);
  return removeu ? { removido: true } : erro('Unidade sem fração cadastrada.', 404);
}

/**
 * Propõe frações proporcionais à área construída de cada unidade.
 *
 * Só propõe: quem confirma é o síndico. A fração ideal consta da convenção do
 * condomínio, que é documento registrado — deduzir da área é um atalho para
 * quem ainda não digitou, não a fonte da verdade.
 *
 * Usa o mesmo rateio das faturas, então a soma fecha exatamente em 1000.
 */
export async function proporPorArea(condominioId, authorization) {
  const unidades = await listarUnidades(condominioId, authorization);
  if (!unidades.ok) {
    return erro('Não foi possível consultar as unidades do condomínio.', 503);
  }

  const semArea = unidades.dados.filter((u) => !u.area || u.area <= 0);
  if (semArea.length) {
    return erro(
      `${semArea.length} unidade(s) estão sem área cadastrada no portaria. ` +
      'Complete a área ou digite as frações à mão.',
    );
  }

  const parcelas = ratear(SOMA_ESPERADA, unidades.dados.map((u) => ({ chave: u.id, peso: u.area })));

  return {
    proposta: unidades.dados.map((u) => ({ ...u, milesimos: parcelas.get(u.id) })),
    soma: SOMA_ESPERADA,
  };
}

/**
 * Aplica várias frações de uma vez.
 *
 * Tudo ou nada: metade das unidades atualizadas seria um estado que ninguém
 * pediu e que o síndico não teria como perceber.
 */
export async function aplicarLote(condominioId, entradas) {
  if (!Array.isArray(entradas) || entradas.length === 0) {
    return erro('Nenhuma fração enviada.');
  }

  for (const e of entradas) {
    const m = Number(e.milesimos);
    if (!e.unidadeId || !Number.isInteger(m) || m <= 0) {
      return erro('Há entradas inválidas na lista de frações.');
    }
  }

  const total = await emTransacao((cliente) =>
    fracoes.definirEmLote(cliente, condominioId, entradas.map((e) => ({
      unidadeId: e.unidadeId,
      milesimos: Number(e.milesimos),
    }))));

  return { aplicadas: total };
}
