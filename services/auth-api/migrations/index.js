import { Umzug, SequelizeStorage } from 'umzug';
import sequelize from '../config/database.js';
import { criarEstruturaInicial } from './estrutura-inicial.js';
import { garantirColunasNovas, migrarUsuariosLegados } from './migrate-rf03.js';
import { garantirColunasRf07 } from './migrate-rf07.js';
import { garantirTabelaCondominios } from './migrate-condominios.js';
import { garantirTabelaPortaria } from './migrate-portaria.js';
import { garantirColunasOauthCode } from './migrate-oauth-code.js';
import { garantirColunasJornadaSnapshot } from './migrate-jornada-snapshot.js';
import { migrarPerfisV2 } from './migrate-perfis-v2.js';
import { garantirCondominioIdReclamacoes } from './migrate-condominio-id.js';
import { removerColunasLegado } from './migrate-remover-legado.js';
import { garantirColunasInvites } from './migrate-invites.js';
import { migrarPerfilTerceiro } from './migrate-terceiro.js';

/**
 * Migrações do auth_db, na ordem em que rodam.
 *
 * Quem executa é o Umzug, o executor de migrações do Sequelize (é ele que o
 * `sequelize-cli` usa por baixo). Cada migração roda uma vez por banco: o nome
 * fica gravado em `SequelizeMeta`, e na subida seguinte ela é pulada. Antes
 * todas rodavam a cada boot, e só não estragavam nada porque cada uma checava
 * por conta própria se já tinha sido aplicada.
 *
 * A ordem é a da lista, a mesma em que o `server.js` as chamava. Ela importa:
 * `remover-colunas-legado` derruba colunas que as anteriores ainda leem, e
 * `perfis-v2` precisa das legadas já convertidas.
 *
 * Migração nova vai no fim, com o número seguinte. Uma que já rodou em algum
 * banco não se renomeia nem muda de lugar: o nome é a chave do histórico.
 *
 * Num banco que já existe, todas rodam uma vez na primeira subida e entram no
 * histórico. Não há risco: são as mesmas funções idempotentes de sempre.
 */
export const migracoes = [
  { name: '001-estrutura-inicial', up: criarEstruturaInicial },
  { name: '002-rf03-colunas-usuario', up: garantirColunasNovas },
  { name: '003-rf07-dependentes', up: garantirColunasRf07 },
  { name: '004-condominios', up: garantirTabelaCondominios },
  { name: '005-oauth-code', up: garantirColunasOauthCode },
  { name: '006-jornada-snapshot', up: garantirColunasJornadaSnapshot },
  { name: '007-rf03-usuarios-legados', up: migrarUsuariosLegados },
  { name: '008-perfis-v2', up: migrarPerfisV2 },
  { name: '009-condominio-reclamacoes', up: garantirCondominioIdReclamacoes },
  { name: '010-invites-responsavel-financeiro', up: garantirColunasInvites },
  { name: '011-perfil-terceiro', up: migrarPerfilTerceiro },
  { name: '012-remover-colunas-legado', up: removerColunasLegado },
  { name: '013-portaria', up: garantirTabelaPortaria },
];

export function criarMigrador() {
  return new Umzug({
    migrations: migracoes,
    storage: new SequelizeStorage({ sequelize }),
    // Silencioso: o server.js imprime uma linha só, com o que foi aplicado.
    logger: undefined,
  });
}

/** Aplica as pendentes e devolve os nomes aplicados nesta subida. */
export async function executarMigracoes() {
  const aplicadas = await criarMigrador().up();
  return aplicadas.map((m) => m.name);
}
