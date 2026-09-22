# comunicacao-service

**Stack:** Node 20 · Express 4 — **Porta:** 3003 — **Banco:** `mora_comunicacao`
**Status:** em operação — conversas, notificações e confirmação de leitura

---

## Responsabilidade

Concentra o que **leva informação ao morador e traz resposta de volta**: a conversa com a
administração, a caixa de notificações de toda a plataforma e o registro de quem leu cada aviso.

| RF | Requisito | Situação |
|---|---|---|
| 12 | Gerenciar Comunicados e Base de Conhecimento | confirmação de leitura pronta; publicação e artigos seguem no portaria |
| 13 | Gerenciar Mensagens e Notificações | conversas e notificações prontas |

---

## O que este serviço deliberadamente **não** faz

**Avisos e base de conhecimento continuam no `portaria-service`.** Eles já funcionam lá —
`AvisoController` com 7 endpoints, `ArtigoConhecimentoController` com 9 — e mover esquema, código
e telas não entrega nada que o usuário não tenha hoje. O que faltava era a **confirmação de
leitura**, e é só isso que nasce aqui.

A consequência é uma tabela que referencia outro banco:

```
mora_comunicacao.aviso_leituras.aviso_id  ──▶  mora.avisos.id   (sem FK)
```

É o mesmo padrão já usado em `mora_financeiro.multas.ocorrencia_id`. Sem FK, **a validação é da
aplicação**: antes de gravar, o serviço busca o aviso no portaria e confere que ele é do
condomínio de quem confirma. Sem essa consulta, qualquer UUID entraria na tabela e o relatório do
síndico contaria leitura de aviso inexistente.

> `avisos.id` é **UUID** (`GenerationType.UUID` na entidade), não serial. A coluna aqui
> acompanha. Errar o tipo só apareceria na primeira confirmação real.

---

## As três tabelas

### `notificacoes` — a caixa de entrada de todo mundo

Uma linha por usuário e por evento. `origem` diz qual serviço publicou (`financeiro`, `portaria`,
`comunicacao`…), e `dados` é um JSONB livre com o que a tela precisa para montar o link de volta.

**`chave_unica` é a defesa contra repetição.** O fechamento de competência do financeiro pode
rodar duas vezes no mesmo mês; sem ela o morador receberia o mesmo aviso de fatura de novo. O
índice é **parcial** (`WHERE chave_unica IS NOT NULL`), para quem publica sem chave não colidir
com todos os outros que também não têm.

### `conversas`, `conversa_participantes`, `mensagens` — o chat

Dois formatos, e a diferença **não é cosmética**:

| Tipo | Quem abre | Como a outra ponta enxerga |
|---|---|---|
| `DIRETA` | só a gestão | participantes nomeados em `conversa_participantes` |
| `ADMINISTRACAO` | qualquer morador | **por regra**: mesmo condomínio + perfil de gestão |

O morador fala com *a administração*, não com uma pessoa. Isso resolve dois problemas de uma vez:

1. **Ninguém quer descobrir o nome do síndico** para relatar um vazamento, e a conversa precisa
   sobreviver à troca de síndico.
2. **O morador não enxerga o síndico em lista nenhuma.** O auth-api *aceita*
   `/api/user-management/users` para ele, mas recorta o resultado na unidade dele — pedindo a
   lista, recebe a si mesmo e os ocupantes do próprio apartamento, e nada mais. Sem o formato
   `ADMINISTRACAO` não haveria destinatário a escolher.

A gestão vira participante **ao responder**, não ao abrir. Antes disso a conversa aparece na lista
dela pela regra do condomínio, com contador de não lidas.

**Não lidas é marca d'água, não linha por mensagem:** `conversa_participantes.ultima_leitura_em`
comparada com `mensagens.criada_em`. Para quem ainda não é participante não há marca, e um
`COALESCE` faz tudo contar como não lido — que é o correto.

### `aviso_leituras` — RF-12

`UNIQUE (aviso_id, usuario_id)`. Confirmar duas vezes é a mesma confirmação, e a data que vale é a
da primeira: devolver a segunda como nova faria o relatório mostrar o morador lendo o aviso toda
vez que abrisse a tela.

**Abrir é ler.** O registro acontece quando o destinatário expande o comunicado, não num botão
separado. Na tela, o que foi lido sai da lista de novos e fica em "Já lidos".

### Público-alvo é filtrado aqui

O portaria devolve **todos** os avisos ativos do condomínio, sem olhar `publicoAlvo` — um morador
recebia comunicado interno de funcionário. O recorte passou a ser feito neste serviço, e a mesma
função decide duas coisas que precisam concordar:

| | |
|---|---|
| quem **vê** o aviso | `GET /avisos` |
| quem entra no **denominador** | `GET /avisos/:id/leituras` |

Se divergissem, o relatório cobraria ciência de gente que nunca recebeu o comunicado.

**A administração nunca é destinatária** — nem de `TODOS`. Ela é a remetente, e ninguém publica um
aviso para si mesmo. O síndico estava dentro de `FUNCIONARIOS`, o que o colocava no denominador do
próprio comunicado e o fazia receber, na tela inicial, os avisos que ele mesmo escreveu. A visão
da gestão é outra e já existe: **Comunicados**, com o que publicou e quem leu.

| Público | Quem recebe |
|---|---|
| `TODOS` | morador, dono de aluguel, porteiro |
| `MORADORES` | morador, dono de aluguel |
| `FUNCIONARIOS` | porteiro |

### Imagem no aviso

O aviso pode ter uma imagem, opcional. `POST /avisos/imagem` recebe o arquivo, e o que vai para o
aviso — que mora no portaria, na coluna `imagem_url` — é só a URL.

O upload ficou aqui porque **o portaria não tem nenhuma infraestrutura de arquivo**: nem uma linha
de `MultipartFile`. Construí-la em Java do zero custaria bem mais que reusar o padrão que o
auth-api já provou nas fotos de perfil.

Três cuidados, todos verificados:

- **O arquivo gravado é gerado pelo sharp**, nunca os bytes recebidos. Um `.txt` renomeado para
  `.png`, com `Content-Type` forjado, é recusado — porque o decodificador falha, e não porque o
  cabeçalho foi conferido.
- **O nome é sorteado aqui**, com extensão fixa. Nome de arquivo vindo do cliente é o caminho
  clássico para escrever fora do diretório pretendido.
- **Volume `comunicacao_uploads`.** Sem ele os arquivos somem no primeiro `up --build` e a coluna
  `imagem_url` fica apontando para 404 — foi exatamente o que aconteceu com as fotos de perfil do
  auth-api antes do `auth_uploads`.

---

## Notificações centralizadas: a rota interna

A decisão foi **centralizar as notificações aqui**, em vez de cada serviço manter as suas. Mas o
caso que motiva a centralização é justamente o mais difícil de autenticar:

> O fechamento de competência do `financeiro` roda **como job**. Não há usuário logado, e um JWT
> sintético não passa — `services/auth-api/middleware/auth.js` faz `User.findByPk(decoded.id)`, e
> não existe usuário com o id de um token forjado.

Daí a rota `POST /api/comunicacao/interno/notificacoes`, autenticada por **`X-Servico-Token`**,
com comparação em tempo constante. Duas regras:

- **Sem `SERVICO_TOKEN` configurado, a rota responde 503** — fechada, não aberta. O padrão
  inseguro é exatamente como o `AuthFilter` do portaria virou um buraco.
- **Reenvio com a mesma `chaveUnica` responde 200 com `repetida: true`**, não erro. Erro faria o
  publicador tentar de novo para sempre — mesmo raciocínio do webhook do Asaas.

A rota tem **prefixo próprio** (`/api/comunicacao/interno`) por um motivo que custou um 401
inexplicável: um `router.use(autenticar)` roda para toda requisição que *entra* no router, não só
para as que casam com alguma rota dele. Montada no mesmo prefixo das outras, a rota interna
levava 401 do middleware de JWT de outro router antes de chegar ao seu.

---

## Autorização

O `condominioId` vem **da claim do token**, nunca do corpo ou da query — mesma regra do
`financeiro` e do `gestao-geral`. O Admin Geral é o único que escolhe qual condomínio olhar, e
entra como **somente leitura**: ele acompanha, mas não fala em nome do condomínio nem confirma
leitura de aviso que não é destinado a ele.

| Perfil | Pode |
|---|---|
| `ADMIN_GERAL` | Ver conversas e avisos de qualquer condomínio; não escreve |
| `ADMIN_SINDICO` | Tudo no próprio condomínio: conversa direta, resposta, encerramento, relatório de leitura |
| `MORADOR` · `DONO_ALUGUEL` · `PORTEIRO` | Conversar com a administração, confirmar leitura, ver a própria caixa |
| `CONVIDADO` | Nada — o acesso dele é de visita, e o histórico de conversa sobrevive à visita |

**Acesso negado a conversa é 404, não 403.** Dizer "existe, mas não é sua" já entrega que existe.

---

## Endpoints

Prefixo `/api/comunicacao`.

| Rota | Quem |
|---|---|
| `GET /notificacoes` · `GET /notificacoes/resumo` | O próprio usuário |
| `PATCH /notificacoes/:id/lida` · `POST /notificacoes/lidas` | O próprio usuário |
| `GET /conversas` · `POST /conversas` | Todos, menos convidado |
| `GET /conversas/:id` · `POST /conversas/:id/mensagens` | Participante, ou gestão do condomínio |
| `PATCH /conversas/:id/encerrar` | Só a gestão |
| `DELETE /mensagens/:id` | Só o autor |
| `GET /contatos` | Só a gestão — a listagem que o morador alcança é recortada na unidade dele |
| `GET /avisos` | Avisos ativos, marcados com o que o usuário já leu |
| `POST /avisos/imagem` | Só a gestão — devolve a URL para gravar no aviso |
| `POST /avisos/:avisoId/leitura` | Quem é destinatário do público-alvo |
| `GET /avisos/leituras` · `GET /avisos/:avisoId/leituras` | Só a gestão |
| `POST /interno/notificacoes` | Outro serviço, com `X-Servico-Token` |
| `GET /health` | Público |

### O contador do sino soma duas coisas

`GET /notificacoes/resumo` devolve `naoLidas` **e** `conversasNaoLidas`. Não é redundância: a
primeira mensagem de um morador para a administração **não gera notificação para ninguém** — não
há participante da gestão a quem endereçá-la. Sem a segunda contagem, o síndico não veria nada
acender.

---

## Como subir

```bash
docker exec -i postgres psql -U admin -d postgres < docker/criar-banco-comunicacao.sql
cd services/comunicacao && npm install && npm run migrate up
docker compose -f docker/docker-compose.yml up -d comunicacao
```

O `init-databases.sql` já inclui `mora_comunicacao`, mas **só roda em volume novo** — em máquina
que já subiu a stack antes, o script avulso é obrigatório.

Defina `SERVICO_TOKEN` em `docker/.env` para habilitar a publicação entre serviços. Sem ele o
serviço sobe normalmente e só `/interno` fica indisponível.

---

## Verificação

```bash
npm test              # 11 checagens sem banco: validação e recusa por perfil
npm run verificar     # 45 checagens contra a stack real
```

O que `npm run verificar` cobre, e que vale olhar: síndico de um condomínio pedindo conversa de
outro recebe **404**; morador de outro condomínio confirmando um aviso alheio recebe **404**;
reenvio com a mesma chave **não duplica**; mensagem removida **continua na conversa sem o texto**.

> O limitador de login do auth-api é de 10 tentativas por 15 minutos e, sem `trust proxy`, o balde
> é compartilhado. Rodar o script duas vezes seguidas basta para estourar — ele detecta o 429 e
> diz o que fazer, em vez de parecer que o seed não rodou.

---

## O que ficou de fora

| Item | Por quê |
|---|---|
| Migrar avisos e base de conhecimento do portaria | Já funcionam lá; mover não entrega nada novo |
| `financeiro` publicar aqui em vez de na tabela própria | A rota interna está pronta e verificada, mas o corte exige mudar também a leitura no frontend — senão a notificação passa a viver em dois lugares |
| Preferências de notificação por categoria | Não há requisito que dependa disso ainda |
| E-mail e push | Só in-app. O `financeiro` já tem envio de e-mail próprio |
| Anexos em mensagem | Fora do escopo do RF-13 |
