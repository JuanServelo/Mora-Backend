# comunicacao-service

**Stack:** Java 21 · Spring Boot 3.5 — **Porta:** 8094 — **Banco:** `mora` (o mesmo do portaria)
**Status:** em operação — avisos, base de conhecimento, chat e notificações

---

## Responsabilidade

Concentra o que **leva informação ao morador e traz resposta de volta**: os comunicados da
administração, a base de conhecimento, a conversa e a caixa de notificações de toda a plataforma.

| RF | Requisito | Situação |
|---|---|---|
| 12 | Gerenciar Comunicados e Base de Conhecimento | avisos e artigos migrados do portaria; confirmação de leitura registrada |
| 13 | Gerenciar Mensagens e Notificações | chat e notificações prontos |

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

`migration/fase5-comunicacao-tabelas.sql` cria o que é novo; `avisos` e `artigos_conhecimento`
já existiam. O `ddl-auto: update` do Hibernate acompanha as colunas, e o SQL versionado existe
para o que o JPA não expressa.

| Tabela | Papel |
|---|---|
| `avisos` | Comunicados da administração, com vigência e público-alvo |
| `artigos_conhecimento` | Base de conhecimento e FAQ |
| `aviso_leituras` | Quem confirmou a leitura de qual aviso — `UNIQUE (aviso_id, usuario_id)` |
| `notificacoes` | A caixa de entrada de todo usuário |
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

> **Vazio conhecido, e é o próximo item:** não existe checagem de **perfil**. Hoje qualquer
> usuário autenticado cria, edita, publica e exclui aviso — um morador inclusive. E
> `buscarPorId`, `atualizar`, `publicar`, `encerrar` e `excluir` não comparam o condomínio do
> aviso com o de quem pede, então o síndico do condomínio A alcança o aviso do B sabendo o id.
> Está em `docs/PENDENCIAS.md`.

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
| `GET /avisos/ativos` | **Os vigentes, já marcados com o que o usuário leu** |
| `POST /avisos/{id}/lido` | Registra a confirmação de leitura |
| `GET /avisos/{id}/leituras` | Quantos confirmaram |
| `GET /notificacoes` | A caixa do usuário, paginada |
| `GET /notificacoes/nao-lidas` · `/contador` | Atalhos de leitura |
| `GET /notificacoes/resumo` | **Notificações não lidas + conversas não lidas** |
| `PATCH /notificacoes/{id}/lida` · `/todas-lidas` | Marca como lida |
| `POST /chat/mensagem` · `GET /chat/conversa/{id}` | Mensagem direta entre dois usuários |
| `GET /chat/nao-lidas` · `/contador` | Contadores do chat |
| `GET /actuator/health` | Público |

### `GET /avisos/ativos` junta as duas fontes

O aviso vem de `avisos` e o "já li" de `aviso_leituras`. A junção acontece no serviço, não na
tela: se cada tela cruzasse por conta própria, cada uma cruzaria de um jeito — a lista de avisos e
o popup da tela inicial já discordariam entre si. Uma consulta resolve a lista inteira; N avisos
não podem virar N idas ao banco.

### `GET /notificacoes/resumo` soma duas coisas

Devolve `naoLidas` **e** `conversasNaoLidas`, e não é redundância: a primeira mensagem que um
morador manda para a administração **não gera notificação para ninguém** — não há destinatário
nomeado a quem endereçá-la. Sem a segunda contagem, o síndico não veria nada acender.

---

## Como subir

```bash
docker compose -f docker/docker-compose.yml up -d comunicacao-service
```

O banco `mora` já existe (é o do portaria). As tabelas novas o Hibernate cria ao subir; o SQL de
`migration/` serve para aplicar à mão quando se quiser controlar o momento.

---

## O que ainda falta

Veio do serviço Node e ainda não tem equivalente aqui. Cada item é um passo separado:

| Item | Por que importa |
|---|---|
| **Checagem de perfil** | Um morador cria e apaga aviso hoje |
| **Isolamento por condomínio nas rotas por id** | Síndico do A alcança aviso do B; responder 404, não 403 |
| **Filtro por `publicoAlvo`** | O morador recebe comunicado interno de funcionário |
| **Relatório com denominador** | "8 confirmaram" sozinho não diz nada; 8 de 9 é ótimo, 8 de 80 é problema |
| **`chaveUnica` na notificação** | O fechamento do financeiro reprocessado duplica o aviso de fatura |
| **Rota interna com `X-Servico-Token`** | O fechamento roda por job, sem usuário logado, e um JWT sintético não passa |
| **`origem` e `dados` na notificação** | A tela monta o link de volta a partir deles |
| **Conversas com a administração** | O morador fala com *a administração*, não com uma pessoa: a conversa sobrevive à troca de síndico, e ele não enxerga o síndico em lista nenhuma |
| **Imagem no aviso** | A coluna `imagem_url` já existe na entidade; falta o upload |

---

## O que ficou de fora, por decisão

| Item | Por quê |
|---|---|
| Preferências de notificação por categoria | Não há requisito que dependa disso ainda |
| E-mail e push | Só in-app. O `financeiro` já tem envio de e-mail próprio |
| Anexos em mensagem | Fora do escopo do RF-13 |
