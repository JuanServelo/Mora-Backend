import dotenv from 'dotenv';
import sequelize from './config/database.js';
import User from './models/User.js';
import { PERFIS, STATUS_USUARIO, CONDOMINIO_DEFAULT } from './constants/perfis.js';

dotenv.config();

const [,, email, senha = 'Admin@123'] = process.argv;

if (!email) {
  console.error('Uso: node make-admin.js <email> [senha]');
  console.error('Exemplo: node make-admin.js admin@mora.com Admin@123');
  process.exit(1);
}

await sequelize.authenticate();

const emailNorm = email.toLowerCase().trim();
let usuario = await User.findOne({ where: { email: emailNorm } });

if (usuario) {
  await User.update(
    {
      perfil: PERFIS.ADMIN_GERAL,
      status: STATUS_USUARIO.ACTIVE,
      condominioId: CONDOMINIO_DEFAULT,
      semAcessoSistema: false,
    },
    { where: { email: emailNorm } },
  );
  console.log(`✓ ${emailNorm} atualizado para ADMIN_GERAL`);
} else {
  await User.create({
    nome: 'Administrador',
    email: emailNorm,
    senha,
    perfil: PERFIS.ADMIN_GERAL,
    status: STATUS_USUARIO.ACTIVE,
    condominioId: CONDOMINIO_DEFAULT,
    provider: 'local',
    semAcessoSistema: false,
    tokenVersion: 0,
    activatedAt: new Date(),
  });
  console.log(`✓ Usuário criado: ${emailNorm} / senha: ${senha}`);
  console.log('  Perfil: ADMIN_GERAL (acesso total)');
}

await sequelize.close();
