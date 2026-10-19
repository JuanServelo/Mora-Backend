# Pendências por microsserviço

O que ainda falta em cada serviço do Mora, e quem responde por ele.

- **Base:** `main` remota em 14/10/2026 — backend `cf52aee`, frontend `47c2093`. Atualizado em 18/10/2026 com a branch `feat/pendencias-joao`, que fecha os itens de João Victor.
- **Responsável:** quem mais alterou o serviço na `main`, contando linhas alteradas (sem merges, `node_modules`, `target/` e `package-lock.json`). Identidades duplicadas foram agrupadas na mesma pessoa.
- **Legenda:** 🔴 defeito (funciona errado hoje) · 🟡 falta (escopo da especificação) · ⚪ dívida (limpeza ou risco) · ✅ resolvido

---

## Resumo

| Serviço | Responsável | Apoio | Pendências | Prioridade |
|---|---|---|---|---|
| `meeting-service` | **Luana Akemi** | João Victor | 🔴 3 · 🟡 1 · ⚪ 1 | **1 — crítico** |
| `portaria-service` | **Juan Servelo** | Ray Govaski | 🔴 2 · 🟡 2 · ⚪ 1 | **2** |
| `comunicacao-service` | **João Victor** | Juan Servelo | ✅ todas | — |
| `plan-service` | **Luana Akemi** | João Victor | 🔴 1 · 🟡 1 · ⚪ 2 | 4 |
| `ocorrencias-service` | **RAY** | — | 🟡 1 (não existe) | 5 |
| `financeiro` | **João Victor** | — | ⚪ 2 (configuração no Asaas) | 6 |
| `auth-api` | **João Victor** | Juan Servelo | ✅ todas | — |
| `gestao-geral` | **João Victor** | — | ⚪ 1 (depende do meeting) | — |
| Infra e repositório | **João Victor** | Juan Servelo | ✅ todas | — |

### Como cada um chegou a responsável

| Serviço | Linhas alteradas por pessoa |
|---|---|
| `meeting-service` | Luana Akemi 2.728 · João Victor 107 · Juan Servelo 14 |
| `portaria-service` | Juan Servelo 15.275 · Ray Govaski 604 · João Victor 235 |
| `comunicacao-service` | João Victor 1.910 · Juan Servelo 1.478 |
| `plan-service` | Luana Akemi 1.475 · João Victor 382 |
| `financeiro` | João Victor 5.934 |
| `auth-api` | João Victor 8.802 · Juan Servelo 1.350 |
| `gestao-geral` | João Victor 1.571 |
| `docker/` | João Victor 719 · Juan Servelo 323 · Luana Akemi 78 |

Thais Amaral, que desenvolveu o `vagas-service` (depois fundido ao portaria), saiu da equipe e não aparece como responsável nem como apoio.

---

## meeting-service — Luana Akemi

O mais crítico: mantém em risco o critério obrigatório 6 (perfis validados no front **e** no back).

| | Pendência | Detalhe |
|---|---|---|
| 🔴 | **Sem autenticação** | Não há filtro de JWT. Qualquer pessoa cria reunião, lança ata e vota |
| 🔴 | **Voto falsificável** | `VoteRequestDTO` recebe `usuarioId` no corpo: dá para votar como outra pessoa trocando um número |
| 🔴 | **Não filtra por condomínio** | Um condomínio enxerga reuniões de outro |
| 🟡 | **RF-11: voto por unidade** | `tb_poll_vote` guarda o usuário, não a unidade |
| ⚪ | `credentials.json` dentro da imagem | Lido do classpath: sem o arquivo o serviço não sobe, e quem tem a imagem tem a credencial |

**Como resolver:** copiar o `AuthFilter` do `plan-service`, ler o usuário da claim em vez do corpo, e trocar o voto para `unidade_id` — os três mexem nos mesmos arquivos.

---

## portaria-service — Juan Servelo

| | Pendência | Detalhe |
|---|---|---|
| 🔴 | **Escrita sem checagem de perfil** | 12 serviços: Aluguel, Apartamento, AreaComun, Bloco, Entrega, FuncionamentoAreaComum, Funcionario, Morador, Turno, Usuario, Vaga, Visitante. Um morador cadastra bloco, vaga e funcionário |
| 🔴 | **Condomínio vem do corpo na escrita** | As rotas recebem a entidade crua; um usuário grava no condomínio de outro, e sem o campo o registro fica sem condomínio |
| 🟡 | **RF-7: aviso de entrega** | O `EntregaService` não avisa o morador quando a encomenda chega |
| 🟡 | **RF-10: antecedência mínima** | A reserva não exige antecedência |
| ✅ | Swagger liberado | Respondia 401 e, liberado, 500 (springdoc 2.0.2 no Boot 3.5). Corrigido na `feat/pendencias-joao`: filtro libera `/swagger-ui` e `/v3/api-docs`, springdoc 2.8.9 |

**Como resolver:** o padrão já existe no próprio serviço — `ReservaService` e `VeiculoService` checam perfil. Estender aos 12, e sobrescrever `condominioId` com `CondominioUtils.condominioIdEfetivo()` antes de salvar. O RF-7 depende da rota interna do comunicacao.

---

## comunicacao-service — João Victor

| | Pendência | Como ficou |
|---|---|---|
| ✅ | **Rota `/interno/notificacoes`** | Autenticada por `X-Servico-Token` (comparação em tempo constante); sem token configurado responde 503. Reenvio com a mesma `chave_unica` devolve 200 com `repetida: true`, sem duplicar. Migração `fase8-notificacoes-servico.sql` |
| ✅ | **Avisos filtram por `publicoAlvo`** | Uma regra só (`PublicoAlvoRegra`) decide quem recebe; quem não é destinatário não vê nem confirma leitura (404) |
| ✅ | **Campo `destinatario`** | A gestão vê todos os avisos, com `destinatario: false` |
| ✅ | `GET /avisos/leituras` e `GET /avisos/{id}/leituras` | Panorama com confirmações contadas numa consulta agrupada; relatório "x de y" com o denominador do auth-api (sem ele, `total: null`) |
| ✅ | `POST /avisos/imagem` | Grava o re-encode da imagem, nunca os bytes recebidos; nome sorteado; limite de 5 MB e 4000 px |
| ✅ | `jwt.secret` sem default | O serviço recusa subir sem `JWT_SECRET` ou com menos de 32 caracteres |
| ✅ | Testes e Swagger | 26 testes JUnit; Swagger em `/swagger-ui/index.html` (springdoc atualizado para 2.8.9) |

---

## plan-service — Luana Akemi

| | Pendência | Detalhe |
|---|---|---|
| 🔴 | **Duas tabelas de assinatura em paralelo** | `tb_subscriptions` e `tb_assinaturas`. O front lê só a segunda: nos condomínios `qa-*` todo item com módulo some do menu |
| 🟡 | **RF-4: trava por plano só no front** | Nenhum serviço consulta o plano; quem chama a API direto usa módulo não contratado |
| ⚪ | Fora do Consul | Não aparecia no catálogo na última checagem |
| ⚪ | Sem testes; Swagger bloqueado | Swagger responde 401 |

---

## ocorrencias-service — a definir

| | Pendência | Detalhe |
|---|---|---|
| 🟡 | **Serviço não existe (RF-14)** | Falta ordem de serviço, responsável e prazo. Hoje só existe a reclamação, no `auth-api`. Decidido: o RF-14 não fica no `auth-api` |

---

## financeiro — João Victor

| | Pendência | Detalhe |
|---|---|---|
| ⚪ | **Chave PIX não cadastrada** | Configuração da conta no painel do Asaas; até lá só boleto funciona |
| ⚪ | `ASAAS_WEBHOOK_TOKEN` vazio | O webhook não confere quem chama. Gerar o token, pôr no `docker/.env` e no painel de webhooks do Asaas |
| ✅ | Rotas antigas com tratamento de erro `async` | `protegerRotas` em faturas, cadastros, contas de consumo, contexto, gateway e webhooks |
| ✅ | Notificações chegando ao comunicacao | A chamada levava o prefixo do gateway (`/api/comunicacao`) indo direto ao container, e dava 404 |
| ✅ | `GET /admin/multas/por-mes` | Multas aplicadas, canceladas e valor por mês, agregados em SQL |

---

## auth-api — João Victor

| | Pendência | Como ficou |
|---|---|---|
| ✅ | Registro no Consul | Anuncia seus prefixos ao Traefik, **menos `/api/portaria`**, que no gateway é do portaria-service |
| ✅ | `GET /api/reclamacoes/estatisticas` | Situação atual, abertas e resolvidas por mês, por categoria e tempo médio de resolução, em SQL |

---

## gestao-geral — João Victor

| | Pendência | Detalhe |
|---|---|---|
| ⚪ | Painel sem assembleias | Depende do meeting-service autenticar e filtrar por condomínio |
| ✅ | Contagens no banco (RNF-20) | Ocorrências, leitura de avisos e multas chegam agregadas pelo dono do dado; a leitura de avisos virou uma chamada só. Só o portaria ainda é contado no gestao-geral (falta a ele um endpoint de contagem) |

---

## Infra e repositório — João Victor

| | Pendência | Como ficou |
|---|---|---|
| ✅ | Senha do Postgres e `JWT_SECRET` versionados | O compose exige os dois no `docker/.env`; os serviços Java não têm mais default. A senha antiga continua no histórico do Git |
| ✅ | `node_modules/`, logs e `target/` no índice | Fora do Git; `.gitignore` cobre `logs/` |
| ✅ | Código morto | `vagas-service`, `portaria-frontend` e `services/.idea` removidos; `package.json` da raiz sem workspaces (o seed tem dependências próprias em `scripts/`) |
| ✅ | Identidades duplicadas | `.mailmap` junta cada pessoa numa linha em `git shortlog` |
| ✅ | Teste de API | `tests-api/`: collection Postman rodada pelo Newman, 50 requisições e 98 asserções ponta a ponta |

---

## Ordem para a defesa

1. **meeting-service** — autenticação, voto pela claim, filtro por condomínio *(Luana Akemi)*
2. **portaria-service** — perfil e condomínio na escrita *(Juan Servelo)*
3. **RF-7 e RF-10** no portaria — aviso de entrega e antecedência *(Juan Servelo)*. A rota interna de notificações do comunicacao, de que o RF-7 depende, já existe
4. **Swagger liberado** no meeting e no plan — usado na apresentação *(Luana Akemi)*. Comunicacao e portaria já respondem

Os itens 1 e 2 tiram do risco o critério obrigatório 6. O 3 dá folga ao critério de 80% do escopo.
