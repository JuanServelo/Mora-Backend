# Sistema de Autenticação JWT + MongoDB

Sistema de autenticação com **Node.js** (backend), **React** (frontend) e **MongoDB**, utilizando JWT para tokens.

## Estrutura

- `services/auth-api/` – API Node.js com Express, JWT e Mongoose
- `services/auth-frontend/` – Aplicação React com Vite

## Como testar

### 1. Subir o MongoDB

```bash
docker compose -f docker/docker-compose.auth.yml up -d
```

Ou, se já usar o compose principal:

```bash
docker compose -f docker/docker-compose.yml up -d mongodb
```

### 2. Iniciar o backend

```bash
cd services/auth-api
npm install
npm run dev
```

O backend deve iniciar em **http://localhost:3001**.

### 3. Iniciar o frontend

```bash
cd services/auth-frontend
npm install
npm run dev
```

O frontend deve iniciar em **http://localhost:5173**.

## Fluxo de testes

1. **Registrar um usuário**
   - Acesse http://localhost:5173/registrar
   - Preencha nome, email e senha (mín. 6 caracteres)
   - Clique em "Registrar"

2. **Login**
   - Acesse http://localhost:5173
   - Use email e senha do usuário criado
   - Clique em "Entrar"

3. **Rota protegida**
   - Após o login, você será redirecionado para o Dashboard
   - Clique em "Testar rota protegida /api/auth/me"
   - A resposta indica que o token JWT foi validado corretamente

4. **Logout**
   - Clique em "Sair" no Dashboard

## Endpoints da API

| Método | Rota | Descrição |
|--------|------|-----------|
| POST | `/api/auth/register` | Registro de usuário |
| POST | `/api/auth/login` | Login |
| GET | `/api/auth/me` | Dados do usuário (requer token) |
| GET | `/api/health` | Status da API |

O frontend usa proxy para `/api` → `http://localhost:3001`, então as requisições não precisam da URL completa.

## Variáveis de ambiente

Crie um arquivo `.env` em `services/auth-api/` (há um `.env.example`):

```
PORT=3001
MONGODB_URI=mongodb://localhost:27017/auth_db
JWT_SECRET=sua-chave-secreta-mudar-em-producao
FRONTEND_URL=http://localhost:5173
```

## Token JWT

O token é enviado no header `Authorization: Bearer <token>` e é armazenado no `localStorage` do navegador após login ou registro.
