import sequelize from '../config/database.js';
import { PERFIS } from '../constants/perfis.js';

/**
 * Remove as colunas legadas de `users`.
 *
 * `role` era o modelo de autorização anterior a `perfil`, e `bloco`,
 * `apartamento` e `vaga` foram substituídos por `unidadeId`. Os dois modelos
 * conviviam, o que abria espaço para divergência — um usuário podia ter
 * `role='admin'` e um perfil sem acesso administrativo, por exemplo.
 *
 * `entradaPermitida` **não** entra nesta lista, embora tenha a mesma idade:
 * `routes/portaria.js` ainda lê e escreve a coluna para liberar ou bloquear a
 * entrada de convidado. Derrubá-la aqui não a fazia sumir — `migrate-portaria`
 * roda depois e a recria com o default — mas zerava a autorização de todos os
 * convidados a cada boot, sem deixar rastro.
 *
 * Antes de derrubar, converte o que ainda não tinha sido convertido. Só então
 * remove: uma coluna descartada com dado dentro não volta.
 */
export async function removerColunasLegado() {
  const [colunas] = await sequelize.query(`
    SELECT column_name FROM information_schema.columns
    WHERE table_name = 'users'
      AND column_name IN ('role', 'bloco', 'apartamento', 'vaga')
  `);

  if (colunas.length === 0) return;

  const existe = (nome) => colunas.some((c) => c.column_name === nome);

  // ── Rede de segurança: quem tinha role=admin e ficou sem perfil vira Admin
  // Geral. Sem isto, um administrador perderia o acesso ao derrubar a coluna.
  if (existe('role')) {
    const [, meta] = await sequelize.query(`
      UPDATE users SET perfil = '${PERFIS.ADMIN_GERAL}'
      WHERE role = 'admin' AND perfil IS NULL
    `);
    if (meta?.rowCount) {
      console.log(`Admins legados convertidos para ADMIN_GERAL: ${meta.rowCount}`);
    }
  }

  // ── Registra o que se perde: vínculo em texto sem unidade correspondente.
  if (existe('bloco')) {
    const [[orfaos]] = await sequelize.query(`
      SELECT count(*)::int AS total FROM users
      WHERE "unidadeId" IS NULL
        AND (bloco IS NOT NULL OR apartamento IS NOT NULL)
    `);
    if (orfaos.total > 0) {
      console.warn(
        `Atenção: ${orfaos.total} usuário(s) tinham bloco/apartamento em texto e nenhum `
        + 'unidadeId. Esse vínculo não é recuperável e será descartado.',
      );
    }
  }

  await sequelize.query(`
    ALTER TABLE users
      DROP COLUMN IF EXISTS role,
      DROP COLUMN IF EXISTS bloco,
      DROP COLUMN IF EXISTS apartamento,
      DROP COLUMN IF EXISTS vaga
  `);

  // O tipo do enum fica órfão depois que a coluna some.
  await sequelize.query('DROP TYPE IF EXISTS enum_users_role');

  console.log(`Colunas legadas removidas de users: ${colunas.map((c) => c.column_name).join(', ')}`);
}
