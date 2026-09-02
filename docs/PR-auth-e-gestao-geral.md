# Segurança da autenticação + novo serviço `gestao-geral`

## Por quê

Duas coisas motivaram este PR.

A primeira foi uma revisão do fluxo de autenticação, que encontrou o **JWT trafegando na query
string** do redirect do Google. A URL fica no histórico do navegador, nos logs de qualquer proxy
no caminho e no cabeçalho `Referer` enviado ao próximo site visitado — ou seja, o token de acesso
vazava por três canais ao mesmo tempo.

A segunda foi o painel do Admin Geral: ele precisa de números consolidados de todos os
condomínios, e **não existia um único endpoint de agregação no projeto**. Toda contagem exibida
era feita no navegador, sobre a lista completa baixada do servidor.

---

## Parte 1 — Autenticação

### O OAuth deixa de mandar o token na URL

O callback do Google agora devolve um **código de uso único**, válido por 2 minutos, do qual só
o hash é gravado no banco. O frontend troca esse código pelo JWT em `POST /oauth/exchange` e
limpa a URL do histórico logo em seguida.

```
antes:  /auth/callback?token=eyJhbGciOiJIUzI1NiJ9...
depois: /auth/callback?code=a3f9...   (descartável, expira em 2 min)
```

### Demais correções

| Falha | Correção |
|---|---|
| Fluxo OAuth sem proteção CSRF | `state` assinado na sessão |
| `session.secret` reutilizava o `JWT_SECRET` | `SESSION_SECRET` próprio; o boot aborta em produção sem ele |
| Logout só limpava o navegador | `POST /logout` revoga via `tokenVersion` — derruba todas as sessões |
| Reset de senha não derrubava sessões | Passa a revogar; quem redefine pode estar expulsando um invasor |
| Conta Google trocava o e-mail pela API | 403 quando `provider === 'google'` (o bloqueio existia só na tela) |
| Um rate limiter para tudo | Três, por finalidade — estourar o login não bloqueia mais o reset |
| `err.message` vazava host, porta e dialeto do banco | Mensagem genérica + `console.error` |
| Senha de admin fixa no código | Vem de `ADMIN_SEED_PASSWORD`; sem ela o seed é pulado |

### Arquivos

`routes/auth.js` · `models/User.js` · `middleware/auth.js` · `server.js`
Novos: `config/passport.js` · `config/regras.js` · `utils/tokens.js`

Migração `migrate-oauth-code.js` adiciona `oauthCode` e `oauthCodeExpira`, com índice.

---

## Parte 2 — Serviço `gestao-geral`

Node/Express, porta **3002**, sem banco próprio. Consolida os dados da plataforma para o painel
do Admin Geral, e é onde os demais painéis analíticos entram depois.

```
GET /api/gestao/dashboard                  KPIs e séries da plataforma
GET /api/gestao/condominios/:id/resumo     resumo de um cliente
GET /health
```

### Ele rejeita token inválido

Vale destacar porque é diferente do resto da malha. O `AuthFilter` do `portaria-service` faz
assim hoje:

```java
try {
    AuthContext.set(jwtUtil.parse(token));
} catch (Exception ignored) {
    // token inválido — a requisição segue mesmo assim
}
```

Num endpoint que expõe dados de **todos** os condomínios, isso não cabe. O middleware do
`gestao-geral` responde **401** para token ausente ou inválido e **403** quando o perfil não é
`ADMIN_GERAL`. É o modelo a replicar nos outros serviços.

### De onde vêm os dados

O dono do dado agrega o próprio dado. O `auth-api` ganhou os primeiros endpoints de agregação do
projeto (`COUNT`/`GROUP BY` em SQL), e o `gestao-geral` compõe a resposta repassando o token do
usuário — sem credencial de serviço e sem ler banco alheio.

O contrato já prevê `fontesIndisponiveis`, para quando entrarem a estrutura física do
`portaria-service` e as assinaturas do `plan-service`.

---

## Como testar

```bash
docker compose -f docker/docker-compose.yml up -d postgres auth-api gestao-geral
```

**Autorização do serviço novo:**

| Requisição | Esperado |
|---|---|
| `GET :3002/api/gestao/dashboard` sem token | 401 |
| Com token inválido | 401 |
| Com token de `MORADOR` | 403 |
| Com token de `ADMIN_GERAL` | 200 |

**OAuth:** entrar com Google e conferir que a URL de retorno traz `?code=` e não `?token=`, e
que o mesmo código recusa a segunda troca.

**Revogação:** logar, guardar o token, chamar `POST /api/auth/logout`, e conferir que o token
anterior passa a receber 401.

Testes do `auth-api`: **20/20**.

---

## Antes do merge

1. **Rotar os segredos.** `POSTGRES_PASSWORD=Vibers@2112` e o `JWT_SECRET` default continuam no
   `docker-compose.yml` versionado. Como os serviços validam HS256 com segredo compartilhado,
   quem lê o repositório forja um token com qualquer perfil. Não foi corrigido aqui porque exige
   coordenação de ambiente.
2. **`SESSION_SECRET` é obrigatório em produção** — o serviço não sobe sem ele.
3. **Backend e frontend precisam subir juntos**: o contrato do OAuth mudou de `?token=` para
   `?code=`.
4. **Rebuild do `portaria-service`** — `VeiculoService.java` mudou junto com os perfis.

---

## Também neste diff

O PR carrega mais duas frentes, resumidas para contexto do revisor:

- **Perfis de 11 para 6** (`ADMIN_GERAL`, `ADMIN_SINDICO`, `PORTEIRO`, `MORADOR`,
  `DONO_ALUGUEL`, `CONVIDADO`). A distinção entre proprietário e inquilino migrou para a flag
  `responsavelFinanceiro`. Migração converte os enums do Postgres trocando o tipo.
- **`condominioId` propagado** para as 23 tabelas raiz — antes só 7 apontavam para o cliente, e
  entregas, visitantes e assembleias eram globais. Constraints únicas globais viraram únicas
  por condomínio.

O PR do frontend traz as telas que consomem tudo isso.
