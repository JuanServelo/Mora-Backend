import express from 'express';
import cors from 'cors';
import dotenv from 'dotenv';
import mongoose from 'mongoose';
import authRoutes from './routes/auth.js';

dotenv.config();

const app = express();
const PORT = process.env.PORT || 3001;

// Middleware
app.use(cors({ origin: process.env.FRONTEND_URL || 'http://localhost:5173' }));
app.use(express.json());

// Rotas
app.use('/api/auth', authRoutes);

// Rota protegida de exemplo
app.get('/api/health', (req, res) => {
  res.json({ status: 'ok', message: 'Auth API funcionando' });
});

// Conexão MongoDB e inicio do servidor
const startServer = async () => {
  const mongoUri = process.env.MONGODB_URI || 'mongodb://localhost:27017/auth_db';
  
  try {
    await mongoose.connect(mongoUri);
    console.log('✓ MongoDB conectado');
  } catch (err) {
    console.error('Erro ao conectar MongoDB:', err.message);
    console.error('Certifique-se de que o MongoDB está rodando (Docker ou instalação local)');
  }

  app.listen(PORT, () => {
    console.log(`✓ Servidor rodando em http://localhost:${PORT}`);
    if (!mongoose.connection.readyState) {
      console.warn('⚠ Requisições falharão até o MongoDB conectar');
    }
  });
};

startServer();
