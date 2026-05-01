import dotenv from 'dotenv';
import sequelize from './config/database.js';
import User from './models/User.js';

dotenv.config();

const email = process.argv[2];
if (!email) {
  console.error('Uso: node make-admin.js <email>');
  process.exit(1);
}

await sequelize.authenticate();

const [count] = await User.update({ role: 'admin' }, { where: { email } });

if (count === 0) {
  console.error(`Usuário não encontrado: ${email}`);
} else {
  console.log(`${email} agora é admin`);
}

await sequelize.close();
