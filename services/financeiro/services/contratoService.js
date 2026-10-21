import * as model from '../models/contratosModel.js';
import { listarUnidades } from '../clients/portariaClient.js';
import { listarResidentesPorUnidade } from '../clients/authClient.js';
import { paraCentavos } from '../utils/dinheiro.js';

const erro = (mensagem, status = 400) => ({ erro: true, mensagem, status });

const DATA = /^\d{4}-\d{2}-\d{2}$/;
const RESPONSAVEIS = ['PROPRIETARIO', 'INQUILINO'];

const hoje = () => new Date().toLocaleDateString('sv-SE', { timeZone: 'America/Sao_Paulo' });

/**
 * Compara o que o contrato diz com quem o auth-api marca como responsável
 * financeiro da unidade.
 *
 * O contrato **não muda** essa marcação: quem recebe a fatura continua sendo
 * decidido pela transferência de responsabilidade no auth-api, que tem efeitos
 * próprios (troca o perfil do dono, desativa convidados). Aqui só se avisa
 * quando os dois discordam, para o síndico ou o dono resolver por lá.
 *
 * Sem resposta do auth-api, devolve `null` — "não sei" é diferente de "não
 * diverge", e a tela não pode afirmar que está tudo certo.
 */
function divergencia(contrato, residentes) {
  if (!contrato.ativo || !residentes) return null;
  const atual = residentes.find((m) => m.responsavelFinanceiro)?.id ?? null;
  const esperado = contrato.responsavelTaxas === 'INQUILINO'
    ? contrato.inquilinoUsuarioId
    : contrato.proprietarioUsuarioId;
  return {
    responsavelAtualId: atual,
    responsavelEsperadoId: esperado,
    divergente: esperado !== null && atual !== esperado,
  };
}

/** Busca os moradores de cada unidade uma vez só, mesmo com vários contratos. */
async function comDivergencia(contratos, authorization) {
  const residentesPorUnidade = new Map();
  for (const c of contratos) {
    if (!c.ativo || residentesPorUnidade.has(c.unidadeId)) continue;
    const r = await listarResidentesPorUnidade(c.unidadeId, authorization);
    residentesPorUnidade.set(c.unidadeId, r.ok ? (r.dados?.moradores ?? []) : null);
  }
  return contratos.map((c) => ({
    ...c,
    divergencia: divergencia(c, residentesPorUnidade.get(c.unidadeId)),
  }));
}

export async function listar(condominioId, authorization) {
  return comDivergencia(await model.listar(condominioId), authorization);
}

export async function listarDaUnidade(condominioId, unidadeId, authorization) {
  return comDivergencia(await model.listar(condominioId, { unidadeId }), authorization);
}

/**
 * Cadastra um contrato.
 *
 * `donoId` vem preenchido quando quem cadastra é o próprio dono: aí a unidade
 * e o proprietário saem do escopo dele, nunca do corpo. Para o síndico, os dois
 * vêm do corpo e são conferidos contra a portaria e o auth-api.
 */
export async function criar(condominioId, corpo, { donoId = null, unidadeDoDono = null } = {}, authorization) {
  const unidadeId = donoId ? unidadeDoDono : corpo.unidadeId;
  if (!unidadeId) return erro('Informe a unidade.');

  const inicio = corpo.inicio;
  if (!DATA.test(inicio ?? '')) return erro('Informe a data de início (AAAA-MM-DD).');
  const fim = corpo.fim || null;
  if (fim && !DATA.test(fim)) return erro('Data de fim inválida (AAAA-MM-DD).');
  if (fim && fim < inicio) return erro('O fim do contrato não pode ser antes do início.');

  const responsavelTaxas = corpo.responsavelTaxas ?? 'INQUILINO';
  if (!RESPONSAVEIS.includes(responsavelTaxas)) {
    return erro('Informe quem paga a taxa: PROPRIETARIO ou INQUILINO.');
  }

  let valorAluguelCentavos = null;
  if (corpo.valorAluguel !== undefined && corpo.valorAluguel !== null && corpo.valorAluguel !== '') {
    valorAluguelCentavos = paraCentavos(corpo.valorAluguel);
    if (valorAluguelCentavos === null || valorAluguelCentavos < 0) return erro('Valor do aluguel inválido.');
  }

  if (!donoId) {
    const unidades = await listarUnidades(condominioId, authorization);
    if (!unidades.ok) return erro('Não foi possível confirmar a unidade. Tente novamente.', 503);
    if (!unidades.dados.some((u) => u.id === unidadeId)) {
      return erro('Unidade não encontrada neste condomínio.', 404);
    }
  }

  // Proprietário e inquilino precisam morar na unidade, ou ter vínculo com ela.
  // Sem conferir, um contrato poderia nomear como inquilino alguém de outro
  // apartamento.
  const residentes = await listarResidentesPorUnidade(unidadeId, authorization);
  if (!residentes.ok) return erro('Não foi possível confirmar os moradores da unidade. Tente novamente.', 503);
  const ids = new Set((residentes.dados?.moradores ?? []).map((m) => m.id));

  const proprietarioUsuarioId = donoId ?? Number(corpo.proprietarioUsuarioId);
  if (!proprietarioUsuarioId || !ids.has(proprietarioUsuarioId)) {
    return erro('O proprietário precisa estar vinculado à unidade.');
  }

  const inquilinoUsuarioId = corpo.inquilinoUsuarioId ? Number(corpo.inquilinoUsuarioId) : null;
  if (inquilinoUsuarioId && !ids.has(inquilinoUsuarioId)) {
    return erro('O inquilino precisa estar vinculado à unidade.');
  }
  if (inquilinoUsuarioId && inquilinoUsuarioId === proprietarioUsuarioId) {
    return erro('Proprietário e inquilino não podem ser a mesma pessoa.');
  }
  if (responsavelTaxas === 'INQUILINO' && !inquilinoUsuarioId) {
    return erro('Para o inquilino pagar a taxa, informe quem é o inquilino.');
  }

  const criado = await model.criar(condominioId, {
    unidadeId, proprietarioUsuarioId, inquilinoUsuarioId, inicio, fim,
    valorAluguelCentavos, responsavelTaxas,
  });
  if (criado.conflito) {
    return erro('Esta unidade já tem um contrato vigente. Encerre o atual antes de cadastrar outro.', 409);
  }

  const [comAviso] = await comDivergencia([criado], authorization);
  return comAviso;
}

/** Encerra um contrato. O dono só encerra os da própria unidade. */
export async function encerrar(condominioId, id, { unidadeDoDono = null } = {}) {
  const contrato = await model.porId(condominioId, id);
  // De outra unidade é 404: não confirma que o contrato existe.
  if (!contrato || (unidadeDoDono && contrato.unidadeId !== unidadeDoDono)) {
    return erro('Contrato não encontrado.', 404);
  }
  if (!contrato.ativo) return erro('Este contrato já está encerrado.', 409);

  const encerrado = await model.encerrar(condominioId, id, hoje());
  if (!encerrado) return erro('O contrato mudou enquanto era encerrado. Atualize a tela.', 409);
  return { ...encerrado, divergencia: null };
}
