import dotenv from 'dotenv';
import mongoose from 'mongoose';

dotenv.config();

const email = process.argv[2];
if (!email) {
  console.error('Uso: node make-admin.js <email>');
  process.exit(1);
}

const mongoUri = process.env.MONGODB_URI || 'mongodb://localhost:27017/auth_db';

await mongoose.connect(mongoUri);

const result = await mongoose.connection.db
  .collection('users')
  .updateOne({ email }, { $set: { role: 'admin' } });

if (result.matchedCount === 0) {
  console.error(`Usuário não encontrado: ${email}`);
} else {
  console.log(`✓ ${email} agora é admin`);
}

await mongoose.disconnect();
