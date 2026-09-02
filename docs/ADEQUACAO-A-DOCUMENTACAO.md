# O que falta em cada serviço para atender à documentação

Comparação entre o que a [especificação](ESPECIFICACAO-PROJETO.md) define e o que o código faz
hoje, serviço a serviço. Cada item cita o requisito ou a regra que o exige.

**Estado em 27/08/2026.** Os números vieram de consulta ao código e ao banco, não de estimativa.

---

## Panorama

| Serviço | Itens | Bloqueadores |
|---|---|---|
| [portaria-service](#portaria-service) | 8 | 2 |
| [auth-api](#auth-api) | 4 | — |
| [plan-service](#plan-service) | 6 | 2 |
| [meeting-service](#meeting-service) | 6 | 2 |
| [vagas-service](#vagas-service) | 1 | — |
| [gestao-geral](#gestao-geral) | 3 | — |
| [Serviços a criar](#serviços-a-criar) | 3 | — |

**Bloqueador** = impede que um requisito seja considerado atendido, ou expõe dado de outro
condomínio.

---

## Transversal — vale para todos

Três exigências da especificação não são atendidas por nenhum serviço, ou quase nenhum.

### 1. Rejeitar JWT inválido — RNF-01

> *"Todo endpoint de domínio exige JWT válido; token ausente ou inválido retorna 401."*

| Serviço | Situação |
|---|---|
| `auth-api` | ✅ rejeita |
| `gestao-geral` | ✅ rejeita |
| `portaria-service` | ⚠️ tem filtro, mas **engole a exceção** — `catch (Exception ignored)` na linha 29 do `AuthFilter`, e a requisição segue |
| `meeting-service` | ❌ nenhum filtro |
| `plan-service` | ❌ nenhum filtro |
| `vagas-service` | ❌ nenhum filtro |

**O que fazer:** replicar o middleware do [`gestao-geral`](servicos/gestao-geral.md), que
responde 401 sem exceção. Nos serviços Java, um `OncePerRequestFilter` registrado por
`FilterRegistrationBean` — sem introduzir `spring-boot-starter-security`, que nenhum serviço usa.

### 2. Aplicar o filtro por condomínio — RNF-16

> *"Toda consulta de domínio aplica o filtro por condomínio."*

As colunas existem em **23 de 23 tabelas raiz**. O filtro é aplicado em **4 dos 15 services** do
`portaria-service` — `Bloco`, `Apartamento`, `AreaComum` e `Aviso`. Nos demais serviços, nenhum.

Na prática: um porteiro do condomínio A enxerga entregas, chaves, veículos e visitantes de todos
os condomínios.

**O que fazer:** derivar o `condominioId` da claim do JWT e aplicá-lo no repositório. Em JPA,
`@Filter` do Hibernate ativado por interceptor evita repetir a cláusula em cada consulta.

Depende do item 1: sem rejeitar token inválido, não há de onde tirar o `condominioId` com
confiança.

### 3. Paginação — RNF-18

> *"Endpoints de listagem são paginados, com no máximo 50 itens por página."*

**Zero** serviços usam `Pageable`. Todas as listagens são `findAll`.

---

## portaria-service

O maior serviço, e o com mais itens em aberto.

| # | O que falta | Por quê | Peso |
|---|---|---|---|
| 1 | **Filtro JWT rejeitar** | RNF-01. O `AuthContext` é consultado em 1 dos 15 services | 🔴 |
| 2 | **Filtro por condomínio** nos 11 services restantes | RNF-16 | 🔴 |
| 3 | Tabela `reservas` | RF-10 exige solicitação, aprovação, conflito e taxa. Hoje só há o cadastro de áreas comuns — **a tabela não existe** | 🟡 |
| 4 | Pré-autorização de visitante | RF-6 prevê que o morador autorize com validade e a guarita veja a lista do dia | 🟡 |
| 5 | Destinatário de encomenda vinculado ao cadastro | RF-7 prevê notificação ao morador; hoje o campo é texto livre | 🟡 |
| 6 | Fundir `carros` em `veiculos` | Duas tabelas com `placa` única separada — a mesma placa pode ter status contraditório | 🟡 |
| 7 | `@Transactional` | RNF-22. **Zero ocorrências** no serviço inteiro; operações multi-passo rodam sem atomicidade | 🟡 |
| 8 | Mover `avisos` e `artigos_conhecimento` | Pertencem ao `comunicacao-service` (RF-12) | 🟢 |

**Por onde começar:** 1 e 2, nessa ordem — são os dois bloqueadores e o segundo depende do
primeiro.

---

## auth-api

O serviço mais aderente à especificação. O que falta é limpeza e migração.

| # | O que falta | Por quê | Peso |
|---|---|---|---|
| 1 | Unificar o controle de acesso | RF-6 está implementado duas vezes: `registros_acesso` aqui e `visitantes`/`veiculos` no portaria. Os dois não se conversam | 🟡 |
| 2 | Mover `reclamacoes` | Pertence ao `ocorrencias-service` (RF-14) | 🟡 |
| 3 | Remover colunas de legado | `role`, `bloco`, `apartamento`, `vaga`, `entradaPermitida` seguem no schema sem uso | 🟢 |
| 4 | Cifrar CPF em repouso | RNF-10. Hoje em texto plano em `users` e `invites` | 🟡 |

---

## plan-service

Parado desde 15/06, com 2 commits. É o serviço com a lacuna funcional mais grave.

| # | O que falta | Por quê | Peso |
|---|---|---|---|
| 1 | **Entidade de assinatura** | RF-4 exige "assinatura do condomínio e aplicação dos limites contratados". Sem ela **não há como saber qual plano cada condomínio tem** — `maxCondominiums`, `maxUsersPerCondominium` e `activeModules` estão cadastrados e não são aplicados em lugar nenhum | 🔴 |
| 2 | **Filtro JWT** | RNF-01. Este serviço define preço e limites do produto, e está aberto | 🔴 |
| 3 | Verificação de limite no `auth-api` | Ao cadastrar condomínio e ao emitir convite | 🟡 |
| 4 | Trilha de auditoria | RNF-28 exige registro de quem alterou plano e assinatura | 🟡 |
| 5 | Seed de planos | A plataforma sobe sem nenhum plano cadastrado | 🟢 |
| 6 | Testes | Único serviço sem nem teste de contexto | 🟢 |

---

## meeting-service

Tecnicamente o melhor código Java do projeto — 18 usos de `@Transactional`, MapStruct, `LAZY`
consistente. O que falta é segurança.

| # | O que falta | Por quê | Peso |
|---|---|---|---|
| 1 | **Filtro JWT** | RNF-01. Sem ele, qualquer requisição cria, edita ou apaga assembleia e ata — documentos oficiais do condomínio | 🔴 |
| 2 | **Unicidade do voto por unidade** | RF-11 define um voto por unidade. `tb_poll_vote` não tem constraint — a regra não está no banco | 🔴 |
| 3 | Filtro por condomínio | RNF-16 | 🟡 |
| 4 | Timeout e retry na chamada ao Google | Se o Google demorar, a criação da assembleia trava | 🟡 |
| 5 | Paginação | RNF-18 | 🟢 |
| 6 | `show-sql` desligado | RNF-26. Ativo hoje | 🟢 |

---

## vagas-service

Um item, que é o serviço inteiro.

| # | O que falta | Por quê |
|---|---|---|
| 1 | **Fundir no `portaria-service`** | A especificação lista 8 serviços e não o inclui. O `portaria-service` já é dono de `vagas_estacionamento` |

**O que a fusão envolve:** mover `disponibilidade_vagas` e `alugueis_vagas` para o banco `mora`,
descartar as 4 tabelas espelho (`blocos`, `apartamentos`, `moradores`,
`vagas_estacionamento`), descontinuar `vagas_db`, e corrigir o seed que aponta para o banco
errado.

Enquanto existir, herda os itens transversais: sem filtro JWT, e o `solicitanteId` vem no corpo
da requisição em vez do token — num serviço que movimenta valores.

---

## gestao-geral

Recém-criado, já aderente ao que a especificação define para ele. O que falta é escopo.

| # | O que falta | Por quê |
|---|---|---|
| 1 | Painéis do síndico | RF-18 prevê operacional, financeiro e estratégico. Só o do Admin Geral existe |
| 2 | Consumir `portaria-service` | Estrutura física e taxa de ocupação — previsto no contrato, ainda não implementado |
| 3 | Consumir `plan-service` | Assinaturas e limites — depende do item 1 do `plan-service` |

Os painéis do síndico dependem de requisitos ainda não implementados: o financeiro precisa do
`financeiro-service`, o operacional precisa do `ocorrencias-service` e do registro de leitura de
avisos.

---

## Serviços a criar

Três serviços da especificação não têm nenhuma linha de código.

| Serviço | RFs | Domínio já existe? |
|---|---|---|
| [`ocorrencias-service`](servicos/ocorrencias-service.md) | 14 | Metade — `reclamacoes` está no `auth-api` |
| [`comunicacao-service`](servicos/comunicacao-service.md) | 12, 13 | Metade — `avisos` e `artigos_conhecimento` estão no portaria |
| [`financeiro-service`](servicos/financeiro-service.md) | 15, 16, 17 | Nada |

Os dois primeiros começam por uma **migração**, não do zero: as tabelas existem e mudam de casa.
O terceiro é inteiramente novo — e é o maior, com 3 RFs e integração de pagamento.

Antes de implementar, cada um tem uma seção **"Em aberto"** no próprio documento com o que ainda
precisa ser decidido: qual gateway de pagamento, canal das notificações, categorias de
ocorrência configuráveis ou fixas.

---

## Ordem sugerida

A especificação já traz um roadmap; esta é a leitura dele pelo lado do código.

| Fase | O que | Onde |
|---|---|---|
| **1** | Filtro JWT que rejeita | portaria, meeting, plan, vagas |
| **2** | Filtro por condomínio nas consultas | todos os serviços de domínio |
| **3** | Entidade de assinatura e aplicação de limites | plan-service + auth-api |
| **4** | Fusão do `vagas-service` e de `carros` em `veiculos` | portaria-service |
| **5** | Tabela de reservas e pré-autorização de visitante | portaria-service |
| **6** | `ocorrencias-service` — migração de `reclamacoes` | novo |
| **7** | `comunicacao-service` — migração de avisos e conhecimento | novo |
| **8** | `financeiro-service` | novo |
| **9** | Painéis do síndico | gestao-geral |

As fases 1 e 2 vêm antes de tudo: são as que fecham o isolamento entre clientes, que é a
premissa de um sistema multi-condomínio.

---

## Referências

- [ESPECIFICACAO-PROJETO.md](ESPECIFICACAO-PROJETO.md) — requisitos e RNFs citados aqui
- [servicos/](servicos/) — documentação detalhada de cada serviço
