# Auth API — Autenticação e Gestão de Usuários (RF03/RF04)



API Node.js + Express + PostgreSQL (Sequelize) com fluxo **invite-only**, JWT, perfis granulares e gestão de usuários.



## Fluxo principal



1. Gestor emite convite (`POST /api/user-management/invites`) → e-mail com código (48h)

2. Usuário acessa `/ativar` no frontend e valida código (`POST /api/invites/validate`)

3. Usuário preenche dados e ativa conta (`POST /api/invites/activate`) → JWT + redirect por perfil

4. Login subsequente via `POST /api/auth/login` ou Google OAuth



Registro público (`POST /api/auth/register`) está **desabilitado**.



## E-mails (convite + esqueci senha)



O sistema envia e-mails via **Gmail SMTP** quando `MAIL_USER` e `MAIL_PASS` estão configurados no `.env`.



### Configurar Gmail SMTP



1. Ative verificação em 2 etapas na conta Google

2. Crie uma App Password em https://myaccount.google.com/apppasswords

3. No `.env` do auth-api:



```env

MAIL_USER=seuemail@gmail.com

MAIL_PASS=xxxx xxxx xxxx xxxx

MAIL_FROM=Mora <seuemail@gmail.com>

FRONTEND_URL=http://localhost:5173

```



4. Reinicie o auth-api (`npm run dev`)



**Convite:** e-mail com código + link `/ativar?codigo=XXXX`  

**Esqueci senha:** e-mail com link `/reset-password?token=XXXX`



Se o SMTP falhar ao emitir convite, o convite ainda é criado e a API retorna `emailEnviado: false` com o código para uso manual.



## Google OAuth



### Configurar no Google Cloud Console



1. Crie um projeto em https://console.cloud.google.com/

2. Configure OAuth consent screen

3. Crie credenciais OAuth 2.0 (tipo **Web application**)

4. Authorized redirect URI: `http://localhost:3001/api/auth/google/callback`

5. No `.env`:



```env

GOOGLE_CLIENT_ID=seu-client-id.apps.googleusercontent.com

GOOGLE_CLIENT_SECRET=seu-client-secret

GOOGLE_CALLBACK_URL=http://localhost:3001/api/auth/google/callback

```



6. Reinicie o auth-api



No frontend, o botão **Entrar com Google** redireciona para `/api/auth/google` e retorna via `/auth/callback?token=...`.



**Nota:** OAuth só funciona para contas já existentes e ativas (não substitui fluxo de convite para novos usuários).



## Endpoints



### Autenticação — `/api/auth`



| Método | Rota | Descrição |

|--------|------|-----------|

| POST | `/login` | Login e-mail/senha (mensagens RF04) |

| POST | `/register` | Desabilitado (403) |

| GET | `/me` | Perfil autenticado |

| PUT | `/me` | Atualizar nome, telefone, e-mail, senha |

| POST | `/me/foto` | Upload avatar JPG/PNG ≤5MB |

| POST | `/forgot-password` | Recuperação de senha (envia e-mail) |

| POST | `/reset-password/:token` | Redefinir senha |

| GET | `/google` | Inicia OAuth Google |

| GET | `/google/callback` | Callback OAuth → redirect frontend |



### Convites — `/api/invites`



| Método | Rota | Descrição |

|--------|------|-----------|

| POST | `/validate` | Validar código de convite |

| POST | `/activate` | Criar conta e obter JWT |



### Gestão — `/api/user-management` (autenticado + perfil gestor)



| Método | Rota | Descrição |

|--------|------|-----------|

| GET | `/users` | Listar usuários e convites pendentes |

| POST | `/invites` | Emitir convite (retorna `emailEnviado`) |

| POST | `/invites/:id/resend` | Reenviar convite expirado |

| PATCH | `/users/:id/deactivate` | Desativar usuário (+ cascata) |



## Frontend (Mora-Frontend)



| Rota | Descrição |

|------|-----------|

| `/login` | Login e-mail/senha + botão Google |

| `/auth/callback` | Recebe token OAuth |

| `/esqueceu-senha` | Solicitar reset por e-mail |

| `/reset-password` | Definir nova senha (link do e-mail) |

| `/ativar` | Ativação por código de convite |



## Scripts



```bash

npm run dev          # servidor com hot-reload

npm test             # testes unitários

node make-admin.js email@exemplo.com  # promove usuário existente a ADMINISTRATOR

```



Para o **primeiro usuário** (banco vazio), insira manualmente no PostgreSQL ou use script de seed.


