import sequelize from '../config/database.js';
import { PERFIS, PERFIL_LEGADO_PARA_NOVO } from '../constants/perfis.js';

const NOVOS = Object.values(PERFIS);

/**
 * Simplificação de 11 perfis para 6.
 *
 * Postgres não permite remover valor de enum, então a estratégia é trocar o
 * tipo: cria o enum novo, converte a coluna com o de-para, e descarta o antigo.
 * Idempotente — se a coluna já usa o tipo novo, não faz nada.
 */
async function migrarColuna(tabela, coluna, tipoAntigo, tipoNovo) {
  const [[atual]] = await sequelize.query(`
    SELECT udt_name FROM information_schema.columns
    WHERE table_name = '${tabela}' AND column_name = '${coluna}'
  `);

  if (!atual) return `${tabela}.${coluna}: coluna inexistente, ignorada`;
  if (atual.udt_name === tipoNovo) return `${tabela}.${coluna}: já migrada`;

  const casos = Object.entries(PERFIL_LEGADO_PARA_NOVO)
    .map(([antigo, novo]) => `WHEN '${antigo}' THEN '${novo}'`)
    .join(' ');

  await sequelize.transaction(async (t) => {
    const q = (sql) => sequelize.query(sql, { transaction: t });

    // O default referencia o tipo antigo e impediria o ALTER TYPE.
    await q(`ALTER TABLE ${tabela} ALTER COLUMN ${coluna} DROP DEFAULT`);

    await q(`
      ALTER TABLE ${tabela}
      ALTER COLUMN ${coluna} TYPE ${tipoNovo}
      USING (CASE ${coluna}::text ${casos} ELSE NULL END)::${tipoNovo}
    `);

    await q(`DROP TYPE IF EXISTS ${tipoAntigo}`);
  });

  return `${tabela}.${coluna}: migrada`;
}

export async function migrarPerfisV2() {
  const valores = NOVOS.map((v) => `'${v}'`).join(', ');

  // CREATE TYPE não aceita IF NOT EXISTS; o bloco torna a criação idempotente.
  await sequelize.query(`
    DO $$ BEGIN
      CREATE TYPE enum_users_perfil_v2 AS ENUM (${valores});
    EXCEPTION WHEN duplicate_object THEN NULL; END $$;
  `);
  await sequelize.query(`
    DO $$ BEGIN
      CREATE TYPE enum_invites_perfil_v2 AS ENUM (${valores});
    EXCEPTION WHEN duplicate_object THEN NULL; END $$;
  `);

  // O antigo LESSEE era, por definição, o responsável financeiro da unidade.
  // Como ele vira MORADOR, a distinção precisa ser preservada nesta coluna
  // ANTES da conversão do enum — depois dela o rótulo LESSEE não existe mais.
  await sequelize.query(`
    ALTER TABLE invites
      ADD COLUMN IF NOT EXISTS "responsavelFinanceiro" BOOLEAN NOT NULL DEFAULT false
  `);
  await sequelize.query(`
    UPDATE invites SET "responsavelFinanceiro" = true
    WHERE perfil::text = 'LESSEE'
  `);
  await sequelize.query(`
    UPDATE users SET "responsavelFinanceiro" = true
    WHERE perfil::text IN ('LESSEE', 'RESIDENT_OWNER')
      AND "unidadeId" IS NOT NULL
      AND "responsavelFinanceiro" IS DISTINCT FROM true
  `);

  const r1 = await migrarColuna('users', 'perfil', 'enum_users_perfil', 'enum_users_perfil_v2');
  const r2 = await migrarColuna('invites', 'perfil', 'enum_invites_perfil', 'enum_invites_perfil_v2');

  // Convidado não acessa o sistema; a flag acompanha o perfil.
  await sequelize.query(`
    UPDATE users SET "semAcessoSistema" = true
    WHERE perfil = '${PERFIS.CONVIDADO}' AND "semAcessoSistema" IS DISTINCT FROM true
  `);

  if (r1.endsWith('migrada') || r2.endsWith('migrada')) {
    console.log(`Perfis simplificados para 6 — ${r1}; ${r2}`);
  }
}
