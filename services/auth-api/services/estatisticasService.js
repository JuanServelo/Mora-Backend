import { Op, fn, col, literal } from 'sequelize';
import User from '../models/User.js';
import Invite from '../models/Invite.js';
import Condominio from '../models/Condominio.js';
import Reclamacao from '../models/Reclamacao.js';
import { STATUS_USUARIO, STATUS_CONVITE } from '../constants/perfis.js';

/**
 * Agregações da plataforma.
 *
 * O dono do dado agrega o próprio dado, em SQL. Antes disso, toda contagem
 * exibida era feita no frontend sobre a lista completa — o que não escala e
 * obrigava a buscar os usuários de cada condomínio um a um.
 */

const MESES_PADRAO = 12;
const MESES_MIN = 1;
const MESES_MAX = 36;

/**
 * Normaliza a janela pedida pelo cliente.
 *
 * Vem da query string, então é texto não confiável: um valor absurdo viraria
 * uma série de milhares de pontos, e um valor inválido, `NaN` silencioso.
 */
function janelaDeMeses(meses) {
  const n = Number(meses);
  if (!Number.isFinite(n)) return MESES_PADRAO;
  return Math.min(MESES_MAX, Math.max(MESES_MIN, Math.trunc(n)));
}

/**
 * Ids dos condomínios que o filtro de status deixa passar.
 *
 * Devolve `null` quando não há filtro — e `null` significa "sem restrição",
 * distinto de `[]`, que significa "nenhum condomínio passou". Confundir os dois
 * faria um filtro sem resultado exibir a plataforma inteira.
 */
export async function condominiosDoFiltro(status) {
  if (!status || status === 'todos') return null;
  const linhas = await Condominio.findAll({
    attributes: ['id'],
    where: { status },
    raw: true,
  });
  return linhas.map((l) => l.id);
}

/** Cláusula de condomínio, respeitando a distinção acima. */
function ondeCondominio(ids, campo = 'condominioId') {
  if (ids === null) return {};
  return { [campo]: { [Op.in]: ids } };
}

/** Converte [{ chave, total }] em { chave: total }. */
function porChave(linhas, campo) {
  return linhas.reduce((acc, linha) => {
    const chave = linha[campo] ?? 'indefinido';
    acc[chave] = Number(linha.total);
    return acc;
  }, {});
}

export async function estatisticasCondominios({ meses, status } = {}) {
  const janela = janelaDeMeses(meses);
  const filtroStatus = status && status !== 'todos' ? { status } : {};

  const porStatus = await Condominio.findAll({
    attributes: ['status', [fn('COUNT', col('id')), 'total']],
    where: filtroStatus,
    group: ['status'],
    raw: true,
  });

  const contagem = porChave(porStatus, 'status');
  const total = Object.values(contagem).reduce((a, b) => a + b, 0);

  const desde = new Date();
  desde.setMonth(desde.getMonth() - (janela - 1));
  desde.setDate(1);
  desde.setHours(0, 0, 0, 0);

  const serie = await Condominio.findAll({
    attributes: [
      [fn('to_char', fn('date_trunc', 'month', col('createdAt')), 'YYYY-MM'), 'mes'],
      [fn('COUNT', col('id')), 'total'],
    ],
    where: { createdAt: { [Op.gte]: desde }, ...filtroStatus },
    group: [literal("date_trunc('month', \"createdAt\")")],
    order: [[literal("date_trunc('month', \"createdAt\")"), 'ASC']],
    raw: true,
  });

  // Preenche os meses sem cadastro para o gráfico não ficar com buracos.
  const mapa = Object.fromEntries(serie.map((l) => [l.mes, Number(l.total)]));
  const criadosPorMes = [];
  for (let i = 0; i < janela; i += 1) {
    const d = new Date(desde);
    d.setMonth(desde.getMonth() + i);
    const chave = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
    criadosPorMes.push({ mes: chave, total: mapa[chave] ?? 0 });
  }

  const trintaDias = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000);
  const novosUltimos30 = await Condominio.count({
    where: { createdAt: { [Op.gte]: trintaDias }, ...filtroStatus },
  });

  return {
    total,
    ativos: contagem.active ?? 0,
    inativos: contagem.inactive ?? 0,
    novosUltimos30,
    criadosPorMes,
    // Devolvido para a tela poder rotular o gráfico com a janela que valeu,
    // em vez de assumir 12 meses.
    meses: janela,
  };
}

export async function estatisticasUsuarios({ condominioIds = null } = {}) {
  const recorte = ondeCondominio(condominioIds);

  const [porPerfilRaw, porStatusRaw, porCondominioRaw, convitesPendentes] = await Promise.all([
    User.findAll({
      attributes: ['perfil', [fn('COUNT', col('id')), 'total']],
      where: recorte,
      group: ['perfil'],
      raw: true,
    }),
    User.findAll({
      attributes: ['status', [fn('COUNT', col('id')), 'total']],
      where: recorte,
      group: ['status'],
      raw: true,
    }),
    User.findAll({
      attributes: ['condominioId', [fn('COUNT', col('id')), 'total']],
      where: { condominioId: { [Op.ne]: null }, ...recorte },
      group: ['condominioId'],
      order: [[fn('COUNT', col('id')), 'DESC']],
      raw: true,
    }),
    Invite.count({
      where: {
        status: STATUS_CONVITE.PENDING,
        expiresAt: { [Op.gt]: new Date() },
        ...recorte,
      },
    }),
  ]);

  const porStatus = porChave(porStatusRaw, 'status');
  const total = Object.values(porStatus).reduce((a, b) => a + b, 0);

  // Resolve o nome do condomínio para o gráfico não mostrar só o slug.
  const condominios = await Condominio.findAll({
    attributes: ['id', 'nome'],
    raw: true,
  });
  const nomes = Object.fromEntries(condominios.map((c) => [c.id, c.nome]));

  return {
    total,
    ativos: porStatus[STATUS_USUARIO.ACTIVE] ?? 0,
    porPerfil: porChave(porPerfilRaw, 'perfil'),
    porStatus,
    porCondominio: porCondominioRaw.map((l) => ({
      condominioId: l.condominioId,
      nome: nomes[l.condominioId] ?? l.condominioId,
      total: Number(l.total),
    })),
    convitesPendentes,
  };
}

export async function estatisticasOcorrencias({ condominioIds = null } = {}) {
  const linhas = await Reclamacao.findAll({
    attributes: ['status', [fn('COUNT', col('id')), 'total']],
    where: ondeCondominio(condominioIds),
    group: ['status'],
    raw: true,
  });
  const porStatus = porChave(linhas, 'status');
  return {
    total: Object.values(porStatus).reduce((a, b) => a + b, 0),
    abertas: (porStatus.PENDENTE ?? 0) + (porStatus.EM_ANALISE ?? 0),
    porStatus,
  };
}

/** Resumo de um condomínio, para a tela de detalhe. */
export async function estatisticasDoCondominio(condominioId) {
  const [porPerfilRaw, porStatusRaw, convitesPendentes, ocorrencias] = await Promise.all([
    User.findAll({
      attributes: ['perfil', [fn('COUNT', col('id')), 'total']],
      where: { condominioId },
      group: ['perfil'],
      raw: true,
    }),
    User.findAll({
      attributes: ['status', [fn('COUNT', col('id')), 'total']],
      where: { condominioId },
      group: ['status'],
      raw: true,
    }),
    Invite.count({
      where: {
        condominioId,
        status: STATUS_CONVITE.PENDING,
        expiresAt: { [Op.gt]: new Date() },
      },
    }),
    Reclamacao.count({ where: { condominioId } }),
  ]);

  const porStatus = porChave(porStatusRaw, 'status');
  return {
    condominioId,
    usuarios: {
      total: Object.values(porStatus).reduce((a, b) => a + b, 0),
      ativos: porStatus[STATUS_USUARIO.ACTIVE] ?? 0,
      porPerfil: porChave(porPerfilRaw, 'perfil'),
      porStatus,
    },
    convitesPendentes,
    ocorrencias,
  };
}
