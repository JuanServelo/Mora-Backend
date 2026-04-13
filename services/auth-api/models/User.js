import mongoose from 'mongoose';
import bcrypt from 'bcryptjs';

const userSchema = new mongoose.Schema({
  nome: {
    type: String,
    required: [true, 'Nome é obrigatório'],
    trim: true,
  },
  email: {
    type: String,
    required: [true, 'Email é obrigatório'],
    unique: true,
    lowercase: true,
    trim: true,
  },
  senha: {
    type: String,
    minlength: 6,
    select: false,
  },
  googleId: {
    type: String,
    sparse: true,
    unique: true,
  },
  provider: {
    type: String,
    enum: ['local', 'google'],
    default: 'local',
  },
  role: {
    type: String,
    enum: ['user', 'admin'],
    default: 'user',
  },
  createdAt: {
    type: Date,
    default: Date.now,
  },
  resetToken: { type: String, select: false },
  resetTokenExpira: { type: Date, select: false },
});

userSchema.pre('save', async function (next) {
  if (!this.isModified('senha') || !this.senha) return next();
  this.senha = await bcrypt.hash(this.senha, 10);
  next();
});

userSchema.methods.compararSenha = async function (senhaDigitada) {
  return bcrypt.compare(senhaDigitada, this.senha);
};

export default mongoose.model('User', userSchema);
