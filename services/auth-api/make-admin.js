import dotenv from 'dotenv';
import sequelize from './config/database.js';
import User from './models/User.js';
import { PERFIS, STATUS_USUARIO, CONDOMINIO_DEFAULT } from './constants/perfis.js';

dotenv.config();

const email = process.argv[2];
if (!email) {
  console.error('Uso: node make-admin.js <email>');
  process.exit(1);
}

await sequelize.authenticate();

const [count] = await User.update(
  {
    perfil: PERFIS.ADMINISTRATOR,
    status: STATUS_USUARIO.ACTIVE,
    condominioId: CONDOMINIO_DEFAULT,
  },
  { where: { email: email.toLowerCase().trim() } },
);

if (count === 0) {
  console.error(`Usuário não encontrado: ${email}`);
} else {
  console.log(`${email} agora é administrador (ADMINISTRATOR)`);
}

await sequelize.close();
