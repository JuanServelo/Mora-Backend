import * as regrasTaxaModel from '../models/regrasTaxaModel.js';
import * as tiposTaxaModel from '../models/tiposTaxaModel.js';
import * as contasConsumoModel from '../models/contasConsumoModel.js';
import * as fracaoModel from '../models/fracaoModel.js';
import * as faturasModel from '../models/faturasModel.js';
import * as multasModel from '../models/multasModel.js';
import * as notificacaoService from './notificacaoService.js';
import { emTransacao } from '../config/database.js';
import { faturavel, estornoPendente } from '../utils/multas.js';
import * as emailService from './emailFinanceiroService.js';
import { listarUnidades } from '../clients/portariaClient.js';
import { listarResidentesPorUnidade } from '../clients/authClient.js';
import { buscarTaxaPlataforma } from '../utils/planClient.js';
import { formatarBRL } from '../utils/dinheiro.js';

const erro = (mensagem, status = 400) => ({ erro: true, mensagem, status });

/**
 * Calcula a data de vencimento da competência.
 *
 * Se diaVencimento > diaFechamento o vencimento cai no mesmo mês (ex: fecha
 * dia 25, vence dia 10 do mês seguinte). Caso contrário, vence no mês da
 * competência (raro, mas possível quando o condomínio aceita pagamento
 * antecipado).
 */
function calcularVencimento(competenciaDate, diaFechamento, diaVencimento) {
  const [ano, mes] = competenciaDate.split('-').map(Number);
  const venceProximo = diaVencimento >= diaFechamento;
  const mesVenc = venceProximo ? mes + 1 : mes;
  const anoVenc = mesVenc > 12 ? ano + 1 : ano;
  const mesVencNorm = mesVenc > 12 ? 1 : mesVenc;
  return `${anoVenc}-${String(mesVencNorm).padStart(2, '0')}-${String(diaVencimento).padStart(2, '0')}`;
}

function competenciaLabel(competenciaDate) {
  const [ano, mes] = competenciaDate.split('-');
  const meses = ['jan', 'fev', 'mar', 'abr', 'mai', 'jun',
    'jul', 'ago', 'set', 'out', 'nov', 'dez'];
  return `${meses[Number(mes) - 1]}/${ano}`;
}

/**
 * Separa as multas candidatas por unidade: as que já podem ser cobradas e as
 * canceladas que precisam ser estornadas.
 *
 * A multa ainda no prazo de recurso fica de fora e entra num fechamento
 * futuro — cobrá-la agora tiraria do morador o direito de contestar.
 */
export function agruparMultas(multas, diasRecurso, hoje = new Date()) {
  const porUnidade = new Map();
  for (const m of multas) {
    const grupo = porUnidade.get(m.unidadeId) ?? { cobrar: [], estornar: [] };
    if (faturavel(m, diasRecurso, hoje)) grupo.cobrar.push(m);
    else if (estornoPendente(m)) grupo.estornar.push(m);
    else continue;
    porUnidade.set(m.unidadeId, grupo);
  }
  return porUnidade;
}

/**
 * Fecha a competência de um condomínio: gera uma fatura por unidade,
 * composta por taxas recorrentes + contas de consumo + taxa da plataforma.
 *
 * Idempotente: se a fatura da unidade já existe (e não está cancelada),
 * a unidade é marcada como "pulada" no resultado sem erro.
 *
 * Não cria cobranças no Asaas — isso acontece no momento em que o morador
 * aciona o pagamento, usando as próprias credenciais.
 */
export async function fecharCompetencia(condominioId, competenciaInput, authorization) {
  if (!competenciaInput) return erro('Informe a competência no formato YYYY-MM.');

  const competencia = competenciaInput.length === 7
    ? competenciaInput + '-01'
    : competenciaInput;

  if (!/^\d{4}-\d{2}-01$/.test(competencia)) {
    return erro('Competência inválida. Use o formato YYYY-MM.');
  }

  const regras = await regrasTaxaModel.obterOuCriar(condominioId);
  const vencimento = calcularVencimento(competencia, regras.diaFechamento, regras.diaVencimento);
  const label = competenciaLabel(competencia);

  const [taxas, contas, unidadesR, fracoesAll, multasCandidatas] = await Promise.all([
    tiposTaxaModel.listar(condominioId, { incluirInativos: false }),
    contasConsumoModel.listarParaFaturamento(condominioId, competencia),
    listarUnidades(condominioId, authorization),
    fracaoModel.listarPorCondominio(condominioId),
    multasModel.listarParaFaturamento(condominioId),
  ]);

  const multasPorUnidade = agruparMultas(multasCandidatas, regras.diasRecursoMulta);

  if (!unidadesR.ok) {
    return erro('Não foi possível obter as unidades do condomínio. Tente novamente.', 503);
  }
  const unidades = unidadesR.dados;
  if (!unidades.length) return erro('Nenhuma unidade encontrada no condomínio.');

  const fracaoMap = new Map(fracoesAll.map((f) => [f.unidadeId, f.milesimos]));
  const totalMilesimos = fracoesAll.reduce((s, f) => s + f.milesimos, 0);

  // Recusa antes de emitir, em vez de emitir errado.
  //
  // Uma conta em modo FRACAO_IDEAL sem nenhuma fração cadastrada dava
  // `Math.floor(valor * 0 / 0)`, ou seja NaN — e como `NaN > 0` é falso, o item
  // era descartado em silêncio. A conta saía marcada como rateada, a fatura
  // saía sem ela, e ninguém ficava sabendo. Melhor não fechar o mês e dizer o
  // que falta.
  if (totalMilesimos === 0) {
    const porFracao = contas.filter((c) => c.modoRateio === 'FRACAO_IDEAL');
    if (porFracao.length) {
      return erro(
        `Não há fração ideal cadastrada, e ${porFracao.length} conta(s) desta ` +
        'competência são rateadas por fração: ' +
        porFracao.map((c) => c.tipo).join(', ') +
        '. Cadastre as frações das unidades, ou mude o rateio dessas contas ' +
        'para divisão igual.',
      );
    }
  }

  let taxaPlataformaCentavos = 0;
  if (regras.incluirTaxaPlataforma) {
    const plan = await buscarTaxaPlataforma(condominioId, authorization);
    taxaPlataformaCentavos = plan.mensalidadeCentavos ?? 0;
  }

  const resultados = [];

  for (const unidade of unidades) {
    const fracaoMilesimos = fracaoMap.get(unidade.id) ?? 0;

    // Em modo FRACAO_IDEAL só fatura unidades com fração configurada.
    if (regras.modo === 'FRACAO_IDEAL' && fracaoMilesimos === 0) {
      resultados.push({ unidadeId: unidade.id, status: 'sem_fracao' });
      continue;
    }

    const peso = regras.modo === 'FRACAO_IDEAL' ? fracaoMilesimos : 1;
    const divisor = regras.modo === 'FRACAO_IDEAL' ? totalMilesimos : unidades.length;

    const itens = [];

    for (const taxa of taxas) {
      const valor = taxa.baseCalculo === 'POR_UNIDADE'
        ? taxa.valorCentavos
        : Math.floor((taxa.valorCentavos * peso) / divisor);
      if (valor > 0) itens.push({ tipo: 'TAXA', descricao: taxa.nome, valorCentavos: valor });
    }

    for (const conta of contas) {
      const pesos = conta.modoRateio === 'FRACAO_IDEAL' ? fracaoMilesimos : 1;
      const div = conta.modoRateio === 'FRACAO_IDEAL' ? totalMilesimos : unidades.length;
      // Divisor zero viraria NaN, e NaN é descartado pelo `valor > 0` abaixo
      // sem erro nenhum. A guarda no topo já impede chegar aqui; esta existe
      // para que um caminho novo não reintroduza o sumiço silencioso.
      const valor = div > 0 ? Math.floor((conta.valorTotalCentavos * pesos) / div) : 0;
      if (valor > 0) {
        itens.push({
          tipo: 'CONTA_CONSUMO',
          descricao: `${conta.tipo}${conta.descricao ? ': ' + conta.descricao : ''}`,
          valorCentavos: valor,
        });
      }
    }

    if (taxaPlataformaCentavos > 0) {
      const valor = Math.floor((taxaPlataformaCentavos * peso) / divisor);
      if (valor > 0) itens.push({ tipo: 'TAXA_PLATAFORMA', descricao: 'Plataforma Mora', valorCentavos: valor });
    }

    // Multas da unidade com o prazo de recurso vencido, e estornos de multas
    // canceladas depois de cobradas. O `origemId` é o que liga o item à multa
    // na gravação abaixo.
    const { cobrar = [], estornar = [] } = multasPorUnidade.get(unidade.id) ?? {};
    for (const m of cobrar) {
      itens.push({
        tipo: 'MULTA', descricao: `Multa: ${m.motivo}`.slice(0, 200),
        valorCentavos: m.valorCentavos, origemId: m.id,
      });
    }
    for (const m of estornar) {
      itens.push({
        tipo: 'ESTORNO', descricao: `Estorno de multa cancelada: ${m.motivo}`.slice(0, 200),
        valorCentavos: -m.valorCentavos, origemId: m.id,
      });
    }

    const total = itens.reduce((s, i) => s + i.valorCentavos, 0);
    if (total <= 0) {
      resultados.push({ unidadeId: unidade.id, status: 'sem_valor' });
      continue;
    }

    // Resolve responsável financeiro da unidade para notificação.
    let responsavelId = null;
    let responsavelEmail = null;
    try {
      const r = await listarResidentesPorUnidade(unidade.id, authorization);
      if (r.ok) {
        const resp = r.dados?.moradores?.find((m) => m.responsavelFinanceiro);
        if (resp) {
          responsavelId = resp.id;
          responsavelEmail = resp.email;
        }
      }
    } catch { /* não-fatal */ }

    try {
      const { fatura, criada } = await faturasModel.criarOuObter(
        condominioId, unidade.id, competencia,
        { vencimento, valorCentavos: total, responsavelUsuarioId: responsavelId },
      );

      if (!fatura || !criada) {
        resultados.push({ unidadeId: unidade.id, status: 'existente', faturaId: fatura?.id });
        continue;
      }

      // Item e marcação da multa na mesma transação: se a marcação falhasse
      // depois do item gravado, a multa seria cobrada de novo no mês seguinte.
      await emTransacao(async (cliente) => {
        const criados = await faturasModel.criarItens(fatura.id, itens, cliente);
        for (const item of criados) {
          if (item.tipo === 'MULTA') await multasModel.vincularItem(cliente, item.origemId, item.id);
          if (item.tipo === 'ESTORNO') await multasModel.vincularEstorno(cliente, item.origemId, item.id);
        }
      });

      // Notificação in-app
      await notificacaoService.criar(
        responsavelId, condominioId, 'NOVA_FATURA',
        'Nova cobrança disponível',
        `Sua fatura de ${label} está disponível. Valor: ${formatarBRL(total)}.`,
        { faturaId: fatura.id },
      );

      // E-mail assíncrono (não aguarda — falha não deve travar o fechamento)
      if (responsavelEmail) {
        emailService.novaFatura(responsavelEmail, {
          nomeUnidade: unidade.numero ?? unidade.id,
          competencia: label,
          valorBRL: formatarBRL(total),
          vencimento,
        }).catch(() => {});
      }

      resultados.push({ unidadeId: unidade.id, status: 'ok', faturaId: fatura.id });
    } catch (err) {
      console.error(`[faturamento] erro na unidade ${unidade.id}:`, err.message);
      resultados.push({ unidadeId: unidade.id, status: 'erro', erro: err.message });
    }
  }

  const criadas = resultados.filter((r) => r.status === 'ok').length;

  // Só carimba a conta como rateada se alguma fatura saiu de fato.
  //
  // Antes marcava sempre, mesmo com zero faturas criadas: a conta aparecia como
  // "rateada" para o síndico e o morador não via cobrança nenhuma, sem que nada
  // no caminho acusasse o problema. Fechamento que não fecha nada não pode
  // mudar o estado de nada.
  if (criadas > 0) {
    for (const conta of contas) {
      await contasConsumoModel.marcarRateada(conta.id);
    }
  }
  const existentes = resultados.filter((r) => r.status === 'existente').length;
  const erros = resultados.filter((r) => r.status === 'erro').length;

  return {
    ok: true,
    competencia,
    unidades: unidades.length,
    faturasCriadas: criadas,
    faturasExistentes: existentes,
    erros,
    detalhe: resultados,
  };
}

/**
 * Preflight — retorna um preview do que seria faturado para a competência.
 * Não grava nada; só informa o síndico antes de confirmar.
 */
export async function previewCompetencia(condominioId, competenciaInput, authorization) {
  const competencia = competenciaInput?.length === 7
    ? competenciaInput + '-01'
    : competenciaInput;

  const [regras, taxas, contas, unidadesR, fracoesAll, multasCandidatas] = await Promise.all([
    regrasTaxaModel.obterOuCriar(condominioId),
    tiposTaxaModel.listar(condominioId, { incluirInativos: false }),
    contasConsumoModel.listarParaFaturamento(condominioId, competencia),
    listarUnidades(condominioId, authorization),
    fracaoModel.listarPorCondominio(condominioId),
    multasModel.listarParaFaturamento(condominioId),
  ]);

  let multasACobrar = 0;
  let estornosDeMulta = 0;
  for (const g of agruparMultas(multasCandidatas, regras.diasRecursoMulta).values()) {
    multasACobrar += g.cobrar.length;
    estornosDeMulta += g.estornar.length;
  }

  if (!unidadesR.ok) return erro('Não foi possível obter as unidades.', 503);

  const unidades = unidadesR.dados;
  const totalMilesimos = fracoesAll.reduce((s, f) => s + f.milesimos, 0);
  const faturasExistentes = await faturasModel.listarPorCondominio(condominioId, {
    competencia,
  });
  const existentesSet = new Set(faturasExistentes.map((f) => f.unidadeId));

  return {
    ok: true,
    unidades: unidades.length,
    faturasNovas: unidades.filter((u) => !existentesSet.has(u.id)).length,
    faturasExistentes: existentesSet.size,
    taxas: taxas.length,
    contasDeConsumo: contas.length,
    multasACobrar,
    estornosDeMulta,
    modoRateio: regras.modo,
    fracoesCadastradas: fracoesAll.length,
    totalMilesimos,
  };
}
