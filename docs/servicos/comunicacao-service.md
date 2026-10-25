# comunicacao-service

**Stack:** Java 21 · Spring Boot 3.5 — **Porta:** 8094 — **Banco:** `mora` (o mesmo do portaria)
**Status:** em operação — avisos, base de conhecimento, chat e notificações

---

## Responsabilidade

Concentra o que **leva informação ao morador e traz resposta de volta**: os comunicados da
administração, a base de conhecimento, a conversa e a caixa de notificações de toda a plataforma.

| RF | Requisito | Situação |
|---|---|---|
| 12 | Gerenciar Comunicados e Base de Conhecimento | avisos por público-alvo, confirmação de leitura com relatório, imagem no aviso |
| 13 | Gerenciar Mensagens e Notificações | conversas, notificações e rota interna para os outros serviços publicarem |

---

## Como este serviço nasceu duas vezes

Dois trabalhos paralelos construíram o mesmo serviço: **este, em Java**, que migrou avisos e
artigos para fora do `portaria-service`, e um em Node na porta 3003, com conversas, confirmação
de leitura com relatório e notificações deduplicadas.

Os dois registravam **o mesmo prefixo no Traefik** (`/api/comunicacao`), então não podiam
coexistir. A decisão foi **seguir com este** e trazer para dentro dele o que o outro fazia. O
serviço Node saiu da árvore; ele continua legível no commit `0786c66`, e o
`docs/servicos/comunicacao-service.md` daquele commit descreve o desenho dele em detalhe.

O que veio junto na integração, e por quê, está marcado abaixo.

---

## Onde os dados moram

O serviço usa o banco `mora`, **o mesmo do `portaria-service`**. Não é acidente: as tabelas
`avisos` e `artigos_conhecimento` já estavam lá, criadas pelo portaria, e migrar o esquema não
entregaria nada que o usuário não tenha hoje. O portaria deixou de ter controller, model e
repository desses assuntos; quem responde por eles agora é este serviço.

A consequência boa é que `aviso_leituras` referencia `avisos` **dentro do mesmo banco**. O
desenho anterior, em Node, tinha banco próprio e a referência atravessava bancos sem FK — a
integridade era da aplicação, e uma consulta ao portaria antes de cada gravação era o que impedia
um UUID inventado de entrar na tabela.

### Tabelas

O esquema é do Flyway (`src/main/resources/db/migration`), e o Hibernate só confere
(`ddl-auto: validate`). O `V1` cria as tabelas, o `V2` traz o que o JPA não expressa (o índice
único parcial de `chave_unica`, que impede notificar duas vezes o mesmo evento, os índices das
listagens e o `ON DELETE CASCADE` das conversas), e o `V3` corrige o `CHECK` de `publico_alvo`,
que não aceitava `SINDICO`. O Hibernate cria o `CHECK` de um enum uma vez e não o atualiza, então
valor novo em enum pede migração. Os scripts que eram aplicados à mão (fase5 a fase8) ficaram em
`db/historico`. Detalhes em [`../MIGRACOES.md`](../MIGRACOES.md).

| Tabela | Papel |
|---|---|
| `avisos` | Comunicados da administração, com vigência e público-alvo |
| `artigos_conhecimento` | Base de conhecimento e FAQ |
| `aviso_leituras` | Quem confirmou a leitura de qual aviso — `UNIQUE (aviso_id, usuario_id)` |
| `notificacoes` | A caixa de entrada de todo usuário; `origem`, `chave_unica` e `dados` (JSONB) para o que vem de outros serviços |
| `chat_mensagens` | Mensagens diretas entre dois usuários |

**`UNIQUE (aviso_id, usuario_id)` importa:** confirmar duas vezes é a mesma confirmação, e a data
que vale é a da primeira. Devolver a segunda como nova faria o relatório mostrar o morador lendo o
aviso toda vez que abrisse a tela.

---

## Autorização

`AuthFilter` valida o JWT emitido pelo auth-api e **recusa** token ausente ou inválido, em vez de
seguir adiante sem identidade — que é como o `AuthFilter` do portaria virou um buraco. O segredo é
o mesmo do auth-api, porque é ele quem emite.

`CondominioUtils.condominioIdEfetivo()` resolve o escopo pela claim do token, nunca pelo corpo ou
pela query. O Admin Geral (condomínio `default`) enxerga todos os condomínios.

### Quem pode o quê

As regras vivem em `security/Autorizacao.java`, e as checagens ficam nos **services**, não nos
controllers. Não é organização: espalhadas pelos controllers elas divergem — a checagem entra em
quatro rotas, esquece a quinta, e a quinta é a que alguém encontra. No serviço, um endpoint novo
não tem como contornar.

| | `ADMIN_SINDICO` | `ADMIN_GERAL` | morador, dono, porteiro | convidado, terceiro |
|---|---|---|---|---|
| Publicar, editar, excluir aviso ou artigo | ✅ | ❌ | ❌ | ❌ |
| Ver rascunho e relatório de leitura | ✅ | ✅ | ❌ | ❌ |
| Ler o que está publicado | ✅ | ✅ | ✅ (só o do seu público) | ❌ |
| Confirmar leitura, conversar | ✅ | ✅ | ✅ | ❌ |

**O Admin Geral não escreve, e isso é deliberado.** Ele opera a plataforma: acompanha qualquer
condomínio, mas não publica comunicado em nome de nenhum. Há também um motivo mecânico —
`condominioIdEfetivo()` devolve `null` para ele, então um aviso criado pelo Admin Geral nasceria
sem condomínio e não apareceria para ninguém.

**`CONVIDADO` e `TERCEIRO` não leem nada.** Existem no cadastro para serem registrados na
portaria, não para receber o comunicado interno do condomínio.

### Quem recebe cada aviso

Uma regra só, `security/PublicoAlvoRegra`, decide quem é destinatário. A mesma regra filtra
`/avisos/ativos`, recusa a confirmação de leitura (404) e calcula o denominador do relatório.

| Público | Destinatários |
|---|---|
| `TODOS` | morador, dono, porteiro |
| `MORADORES` | morador, dono |
| `FUNCIONARIOS` | porteiro |
| `SINDICO` | síndico |

A gestão recebe todos os avisos, com `destinatario: false`: ela publica o comunicado, não é a
quem ele se dirige, e o Início não o mostra a ela como "não lido".

### O condomínio é verificado em toda rota por id

`buscarPorId`, `atualizar`, `publicar`, `encerrar`, `excluir` e a confirmação de leitura comparam
o condomínio do recurso com o de quem pede. **A recusa é 404, não 403:** dizer "existe, mas não é
seu" já confirma que existe, e id de aviso é fácil de varrer.

A ordem também importa — **perfil primeiro, existência depois**. Invertida, um morador descobriria
quais ids existem pela diferença entre 404 e 403 antes de levar o 403.

---

## Endpoints

Prefixo `/api/comunicacao` no Traefik, que o remove antes de encaminhar. Em desenvolvimento o
frontend fala pelo proxy `/comunicacao-api` do Vite, direto na 8094.

| Rota | O que faz |
|---|---|
| `GET/POST/PUT/DELETE /artigos` | Base de conhecimento e FAQ |
| `GET /avisos` · `GET /avisos/{id}` | Comunicados do condomínio |
| `POST /avisos` · `PUT /avisos/{id}` | Publicação e edição |
| `PATCH /avisos/{id}/publicar` · `/encerrar` | Muda o estado sem reescrever o aviso |
| `GET /avisos/ativos` | **Os vigentes do público do usuário, já marcados com o que ele leu** |
| `POST /avisos/{id}/lido` | Registra a confirmação de leitura |
| `GET /avisos/leituras` | Panorama da gestão: avisos vigentes com o total de confirmações |
| `GET /avisos/{id}/leituras` | Relatório "x de y": quem deveria ler e quem já confirmou |
| `POST /avisos/imagem` | Upload da imagem do aviso (gestão); devolve a `url` |
| `GET /uploads/avisos/{arquivo}` | Imagem do aviso. Público: a tag `<img>` não manda token |
| `GET/POST /conversas` · `POST /conversas/{id}/mensagens` · `PATCH /conversas/{id}/encerrar` | Conversas com a administração |
| `POST /interno/notificacoes` | Publicação por outro serviço, com `X-Servico-Token` (abaixo) |
| `GET /notificacoes` | A caixa do usuário, paginada |
| `GET /notificacoes/nao-lidas` · `/contador` | Atalhos de leitura |
| `GET /notificacoes/resumo` | **Notificações não lidas + conversas não lidas** |
| `PATCH /notificacoes/{id}/lida` · `/todas-lidas` | Marca como lida |
| `POST /chat/mensagem` · `GET /chat/conversa/{id}` | Mensagem direta entre dois usuários |
| `GET /chat/nao-lidas` · `/contador` | Contadores do chat |
| `GET /actuator/health` | Público |
| `GET /swagger-ui/index.html` · `GET /v3/api-docs` | Documentação da API. Pública; as chamadas feitas por ela exigem token |

### `GET /avisos/ativos` junta as duas fontes

O aviso vem de `avisos` e o "já li" de `aviso_leituras`. A junção acontece no serviço, não na
tela: se cada tela cruzasse por conta própria, cada uma cruzaria de um jeito — a lista de avisos e
o popup da tela inicial já discordariam entre si. Uma consulta resolve a lista inteira; N avisos
não podem virar N idas ao banco.

### `GET /notificacoes/resumo` soma duas coisas

Devolve `naoLidas` **e** `conversasNaoLidas`, e não é redundância: a primeira mensagem que um
morador manda para a administração **não gera notificação para ninguém** — não há destinatário
nomeado a quem endereçá-la. Sem a segunda contagem, o síndico não veria nada acender.

### `POST /interno/notificacoes` é a porta dos outros serviços

O fechamento do mês no financeiro roda como job, **sem usuário logado**, então não há JWT para
repassar, e um JWT fabricado não passaria. A credencial é um segredo compartilhado,
`SERVICO_TOKEN`, no cabeçalho `X-Servico-Token`. A rota fica fora do `AuthFilter`.

- **Sem token configurado, responde 503.** Uma variável esquecida no deploy fecha a rota em vez
  de abri-la.
- **O token é comparado em tempo constante** (`MessageDigest.isEqual`). Com `equals`, o tempo
  de resposta entregaria quantos caracteres acertaram.
- **Reenvio com a mesma `chaveUnica` responde 200 com `repetida: true`**, não erro: erro faria o
  publicador tentar para sempre. Duas publicações simultâneas esbarram no índice único, e a
  segunda também volta como repetida.

### A imagem gravada é o re-encode, nunca o arquivo recebido

`ImagemAvisoService` decodifica com `ImageIO` e grava de novo, em PNG (com transparência) ou JPEG.
Um texto renomeado para `.png` é recusado porque não decodifica, não porque o cabeçalho foi
conferido (qualquer um falsifica). E um script embutido não sobrevive, porque só os pixels são
regravados. O nome é sorteado no servidor, e há limite de 5 MB e de 4000 px por lado.

---

## Como subir

```bash
docker compose -f docker/docker-compose.yml up -d comunicacao-service
```

Precisa de `JWT_SECRET` (32 caracteres ou mais, o mesmo do auth-api), e o serviço recusa subir
sem ele. `SERVICO_TOKEN` habilita a rota interna.

O banco `mora` já existe (é o do portaria). As tabelas novas o Hibernate cria ao subir; o SQL de
`migration/` serve para aplicar à mão quando se quiser controlar o momento. O `fase8` precisa ser
aplicado uma vez, porque o Hibernate não cria o índice parcial nem amplia o `CHECK`.

## Testes

```bash
docker run --rm -v "$PWD/services/comunicacao-service:/app" -v mora-m2:/root/.m2 -w /app   maven:3.9-eclipse-temurin-21 mvn -B -q test
```

26 testes JUnit (não precisa de Maven local): regra de público-alvo, token e deduplicação da rota
interna, validação de imagem, relatório de leitura e segredo do JWT. Os fluxos ponta a ponta, com
a stack no ar, estão no teste de API em `tests-api/`.

---

## O que ainda falta

Tudo o que veio do serviço Node já tem equivalente aqui: público-alvo, relatório com
denominador, `chaveUnica`, rota interna, `origem` e `dados`, conversas com a administração e
imagem no aviso.

| Item | Por que importa |
|---|---|
| **`@RequestBody Aviso` ao criar e editar** | A rota recebe a entidade crua. O serviço sobrescreve o condomínio, mas o autor só é preenchido quando vem vazio: o síndico pode publicar com outro nome de autor. Um DTO resolveria os dois |

---

## O que ficou de fora, por decisão

| Item | Por quê |
|---|---|
| Preferências de notificação por categoria | Não há requisito que dependa disso ainda |
| E-mail e push | Só in-app. O `financeiro` já tem envio de e-mail próprio |
| Anexos em mensagem | Fora do escopo do RF-13 |
