# Arquivos que não estão no Git

Clonar o repositório **não é suficiente** para subir a stack. Alguns arquivos ficam de fora do
versionamento por conterem segredo, e precisam ser passados de pessoa para pessoa.

Esta é a lista completa, por serviço, do que falta e do que para de funcionar sem cada um.

---

## Resumo: o que trocar entre os membros

| Arquivo | Quem precisa | Sem ele |
|---|---|---|
| `docker/.env` | **Toda a stack** | Serviços sobem com senha e segredos default |
| `services/meeting/src/main/resources/credentials.json` | meeting-service | **Não sobe** — `Exited (1)` |
| `tokens/` (raiz do repositório) | meeting-service | Sobe, mas criar reunião no Meet falha |
| `services/*/.env` | Só para rodar **fora** do Docker | Irrelevante se você usa `docker compose` |

**Em 90% dos casos, `docker/.env` é o único arquivo que você precisa receber.** Os demais só
importam para quem for mexer em reuniões ou rodar um serviço Node direto no host.

---

## 1. `docker/.env` — o essencial

É o arquivo que o `docker compose` lê. Todos os sete serviços tiram daqui senha do banco, segredo
do JWT, credenciais de e-mail e do gateway de pagamento.

```bash
cp docker/.env.example docker/.env
```

O `.env.example` está comentado bloco a bloco, dizendo o que cada variável quebra quando fica
vazia. O importante:

- **`JWT_SECRET` tem que ser o mesmo para todo mundo.** O auth-api emite os tokens e os outros
  seis validam com esse valor. Se dois membros usarem segredos diferentes, cada um só consegue
  usar a própria máquina — e o sintoma é 401 em tudo, o que parece bug de código.
- **`SESSION_SECRET` precisa ser diferente do `JWT_SECRET`.** Reutilizar faz as duas chaves caírem
  juntas se uma vazar.
- **Sem `ADMIN_SEED_EMAIL` e `ADMIN_SEED_PASSWORD`, não há como entrar no sistema pela primeira
  vez** — o seed do admin é pulado e o banco nasce sem nenhuma conta administrativa.
- **Sem `MAIL_USER` / `MAIL_PASS`, o convite é gravado no banco mas o e-mail não sai.** O morador
  nunca recebe o código de ativação. É a falha mais confusa de diagnosticar, porque a tela diz que
  o convite foi criado.

### A armadilha do `$` na chave do Asaas

A chave começa com `$`, e ela quebra em dois lugares antes de chegar ao serviço:

- **PowerShell**, entre aspas duplas, expande `$aact_...` como variável inexistente e grava string
  vazia. Use aspas simples, ou edite o arquivo num editor de texto.
- **Docker Compose** interpola o `.env` do mesmo jeito. Aqui o `$` precisa estar escapado como
  `$$` — o Compose entrega um `$` literal ao container.

---

## 2. `meeting-service` — Google Meet

O único serviço que precisa de arquivo além do `.env`, e o único que **não sobe** sem ele.

### `services/meeting/src/main/resources/credentials.json`

Credenciais OAuth 2.0 do projeto no Google Cloud. Sem o arquivo:

```
FileNotFoundException: Arquivo credentials.json não encontrado no classpath
Container meeting-service  Exited (1)
```

> **Atenção ao compartilhar:** o arquivo é lido do *classpath*, então é copiado para dentro da
> imagem no build. Quem tem a imagem tem a credencial. É a mesma classe de problema da senha do
> Postgres versionada — está anotado em [PENDENCIAS.md](PENDENCIAS.md). Montar por volume, como já
> é feito com `tokens/`, resolveria.

### `tokens/` — na raiz do repositório

O compose monta `../tokens:/app/tokens`. É onde o fluxo OAuth guarda o token de acesso depois que
alguém autoriza a conta Google uma vez.

Gerado por `GoogleMeetAuthHelper` (abre o navegador e pede autorização), **não** copiado de outra
pessoa: o token é vinculado à conta que autorizou.

Sem ele o serviço sobe normalmente, mas criar reunião falha na hora de chamar a API do Meet.

---

## 3. `services/*/.env` — só fora do Docker

Cada serviço Node tem um `.env.example` próprio:

| Serviço | Template |
|---|---|
| `auth-api` | `services/auth-api/.env.example` |
| `financeiro` | `services/financeiro/.env.example` |
| `gestao-geral` | `services/gestao-geral/.env.example` |

**Se você usa `docker compose`, pode ignorar esta seção.** Dentro do container o ambiente vem do
`docker/.env`, e esses arquivos nem são lidos.

Eles servem para rodar um serviço direto no host — `npm run dev`, com o Postgres do Docker. A
diferença que mais pega: **a porta do Postgres é `5433` no host**, não `5432`. Dentro da rede do
Compose é 5432; publicada para fora, 5433.

Os serviços Java não têm `.env`: o ambiente deles vem inteiro do `application.yml`/`.properties`,
com default para desenvolvimento local.

---

## 4. Frontend

O `.env` do frontend **está versionado** — só tem URLs de `localhost`, nenhum segredo. Não há nada
a trocar.

```
VITE_API_URL=http://localhost:3001          # auth-api
VITE_GESTAO_API_URL=http://localhost:3002   # gestao-geral
VITE_FINANCEIRO_API_URL=http://localhost:3004
```

`VITE_COMUNICACAO_API_URL` fica **ausente de propósito**: sem ela o client usa o proxy
`/comunicacao-api` do Vite, que aponta para a 8094. Defini-la desliga o proxy — só faça isso
apontando para a porta certa.

---

## Como passar os arquivos

Eles contêm segredo. **Não mande por commit, Slack público ou e-mail.**

O `docker/.env` é texto curto: o caminho mais simples é cada pessoa criar o seu a partir do
`.env.example` e combinar **apenas o `JWT_SECRET`** por canal privado — é a única variável que
precisa ser idêntica entre as máquinas.

Credenciais do Google e do Asaas são por conta: quem for mexer em reuniões ou cobrança gera as
próprias no console do respectivo serviço, em vez de compartilhar as de alguém.

---

## Conferir se está tudo no lugar

```bash
docker compose -f docker/docker-compose.yml up -d --build
docker compose -f docker/docker-compose.yml ps
```

Os sete serviços devem aparecer como `running`. Se algum estiver `Exited`, o log diz o porquê:

```bash
docker compose -f docker/docker-compose.yml logs meeting-service --tail 30
```

Health de cada um:

| Serviço | |
|---|---|
| auth-api | `curl http://localhost:3001/api/health` |
| gestao-geral | `curl http://localhost:3002/health` |
| financeiro | `curl http://localhost:3004/health` |
| portaria-service | `curl http://localhost:8090/actuator/health` |
| meeting-service | `curl http://localhost:8091/actuator/health` |
| plan-service | `curl http://localhost:8093/actuator/health` |
| comunicacao-service | `curl http://localhost:8094/actuator/health` |

Quem se registrou no Consul: `http://localhost:8500`.

### Dados de teste

```bash
cd services/auth-api && node scripts/seed-condominios-teste.js
```

Cria 6 condomínios e 363 usuários, todos com a senha `Teste1234` e e-mail no domínio
`@qa.mora.local`. Tudo marcado com o prefixo `qa-`, e `--limpar` remove sem tocar em dado real.
