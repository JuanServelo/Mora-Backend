# Mora — Backend

Backend do sistema de gestão condominial **Mora**, composto por três microsserviços independentes orquestrados via Docker Compose com Consul (service discovery) e Traefik (API gateway).

---

## Sobre o projeto

O Mora é um sistema para gestão de condomínios que oferece:

- Autenticação de moradores com JWT e Google OAuth
- Gestão de estrutura: blocos, apartamentos e vagas de garagem
- Portaria: controle de visitantes, funcionários, chaves, carros e entregas
- Reuniões condominiais com integração ao Google Meet, atas e enquetes
- Painel administrativo e área do morador

---

## Arquitetura

```
Frontend (React + Vite — porta 5173)
        │
        ├── Auth API (Node.js — porta 3001) ──── PostgreSQL: auth_db
        │
        ├── Portaria Service (Java — porta 8090) ─ PostgreSQL: mora
        │
        └── Meeting Service (Java — porta 8091) ── PostgreSQL: mora_meeting

[Consul — porta 8500]   service discovery entre os serviços
[Traefik — porta 8080]  API gateway / roteamento
[pgAdmin — porta 5050]  interface visual do banco de dados
```

---

## Serviços

| Serviço | Tecnologia | Porta | Banco |
|---|---|---|---|
| Auth API | Node.js 20 + Express + Sequelize | 3001 | auth_db |
| Portaria Service | Java 21 + Spring Boot 3.5 | 8090 | mora |
| Meeting Service | Java 21 + Spring Boot 3.2 | 8091 | mora_meeting |
| PostgreSQL | PostgreSQL 16 | 5432 | — |
| Consul | Consul 1.15 | 8500 | — |
| Traefik | Traefik 2.9 | 8080 / 8087 | — |
| pgAdmin | pgAdmin 4 | 5050 | — |

---

## Pré-requisitos

### Opção Docker (recomendada)
- **Docker** 24 ou superior
- **Docker Compose** v2

### Opção local (desenvolvimento)
- **Node.js** 20 ou superior
- **Java** 21 (JDK)
- **Maven** 3.9 ou superior
- **PostgreSQL** 16 rodando localmente

---

## Executando com Docker (recomendado)

### 1. Configure as variáveis de ambiente

```bash
cd Mora-Backend/docker
cp .env.example .env
```

Edite o arquivo `.env` com seus valores:

```env
# PostgreSQL
POSTGRES_DB=mora
POSTGRES_USER=admin
POSTGRES_PASSWORD=sua_senha_segura

# Auth API
JWT_SECRET=chave-secreta-longa-e-aleatoria
FRONTEND_URL=http://localhost:5173

# Google OAuth (opcional — necessário para login com Google)
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=
GOOGLE_CALLBACK_URL=http://localhost:3001/api/auth/google/callback

# Email (opcional — necessário para recuperação de senha)
MAIL_USER=seuemail@gmail.com
MAIL_PASS=sua-app-password-gmail
MAIL_FROM=Mora <seuemail@gmail.com>
```

### 2. Suba todos os serviços

```bash
cd Mora-Backend/docker
docker compose up -d
```

### 3. Verifique se está tudo rodando

```bash
docker compose ps
```

### 4. Acompanhe os logs

```bash
# Todos os serviços
docker compose logs -f

# Serviço específico
docker compose logs -f auth-api
docker compose logs -f portaria-service
docker compose logs -f meeting-service
```

### Parar os serviços

```bash
docker compose down

# Para remover também os volumes (apaga o banco de dados)
docker compose down -v
```

---

## Executando localmente (desenvolvimento)

Certifique-se de que o PostgreSQL está rodando e crie os bancos necessários:

```sql
CREATE DATABASE mora;
CREATE DATABASE auth_db;
CREATE DATABASE mora_meeting;
```

### Auth API

```bash
cd Mora-Backend/services/auth-api

# Crie o arquivo de variáveis de ambiente
cp .env.example .env  # ajuste os valores

# Instale as dependências
npm install

# Inicie em modo desenvolvimento (hot-reload)
npm run dev
```

Disponível em: **http://localhost:3001**

### Portaria Service

```bash
cd Mora-Backend/services/portaria-service

# Build e execução
mvn spring-boot:run
```

As variáveis de ambiente podem ser passadas como parâmetros ou configuradas no `application.yml`:

```bash
POSTGRES_HOST=localhost \
POSTGRES_DB=mora \
POSTGRES_USER=admin \
POSTGRES_PASSWORD=sua_senha \
mvn spring-boot:run
```

Disponível em: **http://localhost:8090**

### Meeting Service

```bash
cd Mora-Backend/services/meeting

mvn spring-boot:run
```

Disponível em: **http://localhost:8091**

---

## Variáveis de ambiente

### Auth API

| Variável | Padrão | Descrição |
|---|---|---|
| `PORT` | `3001` | Porta do servidor |
| `POSTGRES_HOST` | `localhost` | Host do PostgreSQL |
| `POSTGRES_PORT` | `5432` | Porta do PostgreSQL |
| `POSTGRES_DB` | `auth_db` | Nome do banco |
| `POSTGRES_USER` | `admin` | Usuário do banco |
| `POSTGRES_PASSWORD` | — | Senha do banco |
| `JWT_SECRET` | — | Chave para assinar tokens JWT |
| `FRONTEND_URL` | `http://localhost:5173` | URL do frontend (CORS) |
| `GOOGLE_CLIENT_ID` | — | Client ID do Google OAuth |
| `GOOGLE_CLIENT_SECRET` | — | Client Secret do Google OAuth |
| `GOOGLE_CALLBACK_URL` | — | Callback URL do Google OAuth |
| `MAIL_USER` | — | E-mail remetente (Gmail) |
| `MAIL_PASS` | — | App password do Gmail |
| `MAIL_FROM` | — | Nome e e-mail exibidos no envio |

### Portaria Service

| Variável | Padrão | Descrição |
|---|---|---|
| `POSTGRES_HOST` | `localhost` | Host do PostgreSQL |
| `POSTGRES_DB` | `mora` | Nome do banco |
| `POSTGRES_USER` | `admin` | Usuário do banco |
| `POSTGRES_PASSWORD` | `Vibers@2112` | Senha do banco |
| `SPRING_CLOUD_CONSUL_HOST` | `localhost` | Host do Consul |
| `SPRING_CLOUD_CONSUL_PORT` | `8500` | Porta do Consul |

### Meeting Service

| Variável | Padrão | Descrição |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/mora_meeting` | URL JDBC completa |
| `SPRING_DATASOURCE_USERNAME` | `admin` | Usuário do banco |
| `SPRING_DATASOURCE_PASSWORD` | `Vibers@2112` | Senha do banco |
| `SPRING_CLOUD_CONSUL_HOST` | `localhost` | Host do Consul |
| `SPRING_CLOUD_CONSUL_PORT` | `8500` | Porta do Consul |

---

## Banco de dados

Os bancos são criados automaticamente pelo script `docker/init-databases.sql` na primeira inicialização do container PostgreSQL. As tabelas são gerenciadas pelo Hibernate (`ddl-auto: update`) — não é necessário rodar migrations manualmente.

### Acessando o pgAdmin

1. Acesse **http://localhost:5050**
2. Login: `admin@mora.com` / senha: `admin`
3. Clique em **Add New Server** e preencha:

**Aba General:**
- Name: `Mora`

**Aba Connection:**
- Host: `postgres` (nome do container, não `localhost`)
- Port: `5432`
- Database: `mora`
- Username: `admin`
- Password: conforme configurado no `.env`

---

## Endpoints e documentação

### Auth API — principais rotas

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/api/auth/register` | Cadastro de usuário |
| `POST` | `/api/auth/login` | Login com e-mail e senha |
| `GET` | `/api/auth/me` | Dados do usuário autenticado |
| `POST` | `/api/auth/google` | Login com Google |
| `GET` | `/api/users` | Listar usuários (admin) |
| `GET` | `/api/health` | Health check |

### Portaria Service — Swagger UI

Disponível em **http://localhost:8090/swagger-ui.html** com todos os endpoints documentados interativamente.

### Meeting Service — Swagger UI

Disponível em **http://localhost:8091/swagger-ui.html**.

---

## Estrutura de diretórios

```
Mora-Backend/
├── docker/
│   ├── docker-compose.yml    # Orquestração de todos os serviços
│   ├── init-databases.sql    # Criação dos bancos de dados
│   └── .env.example          # Modelo de variáveis de ambiente
├── docs/                     # Documentação adicional
└── services/
    ├── auth-api/             # Node.js — autenticação e usuários
    │   ├── config/
    │   ├── middleware/
    │   ├── models/
    │   ├── routes/
    │   ├── utils/
    │   ├── Dockerfile
    │   └── server.js
    ├── portaria-service/     # Java — portaria e estruturas do condomínio
    │   ├── src/main/java/portaria/
    │   │   ├── controller/
    │   │   ├── dto/
    │   │   ├── exception/
    │   │   ├── model/
    │   │   ├── repository/
    │   │   └── service/
    │   ├── Dockerfile
    │   └── pom.xml
    └── meeting/              # Java — reuniões, atas e enquetes
        ├── src/main/java/com/mora/meeting/
        │   ├── controller/
        │   ├── dto/
        │   ├── entity/
        │   ├── mapper/
        │   └── repository/
        ├── Dockerfile
        └── pom.xml
```
