import sequelize from '../config/database.js';

/**
 * As migrações RF03 e RF07 nasceram antes da simplificação de perfis e operam
 * com os rótulos antigos (`RESIDENT_OWNER`, `ADMINISTRATOR`). Depois que
 * migrate-perfis-v2 converte o enum, esses valores deixam de existir e qualquer
 * comparação com eles quebra o boot.
 *
 * Este helper diz em qual estado o banco está, para que as migrações legadas
 * pulem os trechos que não fazem mais sentido.
 */
export async function usaEnumPerfilLegado() {
  const [[coluna]] = await sequelize.query(`
    SELECT udt_name FROM information_schema.columns
    WHERE table_name = 'users' AND column_name = 'perfil'
  `);
  return coluna?.udt_name === 'enum_users_perfil';
}
