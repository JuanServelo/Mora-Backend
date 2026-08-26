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

const MESES_SERIE = 12;

/** Converte [{ chave, total }] em { chave: total }. */
function porChave(linhas, campo) {
  return linhas.reduce((acc, linha) => {
    const chave = linha[campo] ?? 'indefinido';
    acc[chave] = Number(linha.total);
    return acc;
  }, {});
}

export async function estatisticasCondominios() {
  const porStatus = await Condominio.findAll({
    attributes: ['status', [fn('COUNT', col('id')), 'total']],
    group: ['status'],
    raw: true,
  });

  const contagem = porChave(porStatus, 'status');
  const total = Object.values(contagem).reduce((a, b) => a + b, 0);

  const desde = new Date();
  desde.setMonth(desde.getMonth() - (MESES_SERIE - 1));
  desde.setDate(1);
  desde.setHours(0, 0, 0, 0);

  const serie = await Condominio.findAll({
    attributes: [
      [fn('to_char', fn('date_trunc', 'month', col('createdAt')), 'YYYY-MM'), 'mes'],
      [fn('COUNT', col('id')), 'total'],
    ],
    where: { createdAt: { [Op.gte]: desde } },
    group: [literal("date_trunc('month', \"createdAt\")")],
    order: [[literal("date_trunc('month', \"createdAt\")"), 'ASC']],
    raw: true,
  });

  // Preenche os meses sem cadastro para o gráfico não ficar com buracos.
  const mapa = Object.fromEntries(serie.map((l) => [l.mes, Number(l.total)]));
  const criadosPorMes = [];
  for (let i = 0; i < MESES_SERIE; i += 1) {
    const d = new Date(desde);
    d.setMonth(desde.getMonth() + i);
    const chave = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
    criadosPorMes.push({ mes: chave, total: mapa[chave] ?? 0 });
  }

  const trintaDias = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000);
  const novosUltimos30 = await Condominio.count({
    where: { createdAt: { [Op.gte]: trintaDias } },
  });

  return {
    total,
    ativos: contagem.active ?? 0,
    inativos: contagem.inactive ?? 0,
    novosUltimos30,
    criadosPorMes,
  };
}

export async function estatisticasUsuarios() {
  const [porPerfilRaw, porStatusRaw, porCondominioRaw, convitesPendentes] = await Promise.all([
    User.findAll({
      attributes: ['perfil', [fn('COUNT', col('id')), 'total']],
      group: ['perfil'],
      raw: true,
    }),
    User.findAll({
      attributes: ['status', [fn('COUNT', col('id')), 'total']],
      group: ['status'],
      raw: true,
    }),
    User.findAll({
      attributes: ['condominioId', [fn('COUNT', col('id')), 'total']],
      where: { condominioId: { [Op.ne]: null } },
      group: ['condominioId'],
      order: [[fn('COUNT', col('id')), 'DESC']],
      raw: true,
    }),
    Invite.count({
      where: {
        status: STATUS_CONVITE.PENDING,
        expiresAt: { [Op.gt]: new Date() },
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

export async function estatisticasOcorrencias() {
  const linhas = await Reclamacao.findAll({
    attributes: ['status', [fn('COUNT', col('id')), 'total']],
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
