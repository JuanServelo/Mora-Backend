# Pendências do projeto

O que falta no Mora, medido contra a [especificação](ESPECIFICACAO-PROJETO.md) e
os critérios de avaliação.

| Repositório | Base |
|---|---|
| Backend | `fa5bcf5` + multas, contratos, prestação de contas, painéis e limitador (05/10, ainda sem commit) |
| Frontend | `32f789b` + telas correspondentes (05/10, ainda sem commit) |
| Atualização | 18/10, branch `feat/pendencias-joao`: rota interna de notificações, público-alvo e leitura de avisos, agregação no banco, limpeza do repositório |

---

## 1. Resumo

| Requisitos funcionais | Quantidade |
|---|---|
| ✅ Completos | 14 |
| ⚠️ Parciais | 4 |
| 📋 Não iniciados | 0 |

**78% completos**, ou ~89% contando parcial como meio ponto. A meta do critério
1 é 80%.

### Critérios de avaliação pendentes

| # | Critério | O que falta |
|---|---|---|
| 1 | Pelo menos 80% do escopo | Faltam 4 requisitos parciais (seção 3) |
| 2 | Processo ágil documentado | As sprints estão em `atividades/`, fora do repositório (seção 9) |
| 6 | 2 perfis validados no front **e** no back | Meeting corrigido e testado; validar na stack. Escrita do portaria ainda requer revisão de perfil (seção 2) |

Os critérios 3, 4, 5, 7 e 8 estão atendidos.

---

## 2. Defeitos — prioridade alta

### 2.1 `meeting-service` — corrigido no código; validar atualização

JWT, perfis, escopo de condomínio, identidade pela claim e voto por unidade foram
implementados com testes de integração e concorrência. Credenciais Google não entram
no JAR ou na imagem; a integração é opcional. Antes da publicação, conferir os registros
antigos sem condomínio e as votações com votos legados. Procedimento em
[meeting-service](servicos/meeting-service.md).

### 2.2 Portaria: escrita sem perfil e com o condomínio vindo do corpo

**12 serviços sem checagem de perfil:** `Aluguel`, `Apartamento`, `AreaComun`,
`Bloco`, `Entrega`, `FuncionamentoAreaComum`, `Funcionario`, `Morador`, `Turno`,
`Usuario`, `Vaga` e `Visitante`. Um morador autenticado cadastra bloco,
apartamento, vaga e funcionário.

**12 rotas recebem a entidade crua** (`@RequestBody Bloco`, `Vaga`,
`Funcionario`, `Morador`…), com o `condominioId` dentro. O `BlocoService` usa o
valor do corpo como veio: **um morador do condomínio A cria um bloco no
condomínio B**.

**Correção:** checar perfil nos 12 serviços, como `ReservaService` e
`VeiculoService` já fazem, e sobrescrever `condominioId` com
`CondominioUtils.condominioIdEfetivo()` antes de salvar.

### ~~2.3 Notificações do financeiro não chegam~~ — resolvido

O `comunicacao-service` ganhou `POST /interno/notificacoes`, autenticada por
`X-Servico-Token` (503 sem token configurado, reenvio com a mesma `chave_unica`
responde 200 com `repetida: true`). O financeiro também chamava o caminho errado
(com o prefixo do gateway, indo direto ao container), o que dava 404. Coberto
pelo teste de API em `tests-api/`: a multa aplicada chega ao sino do morador, uma
vez só.

---

## 3. Requisitos funcionais incompletos

### Parciais

| RF | Requisito | Serviço | O que falta |
|---|---|---|---|
| 7 | Entregas | portaria | **Notificar o destinatário** ao registrar a encomenda |
| 10 | Reservas | portaria | **Antecedência mínima** para solicitar |
| 11 | Assembleias e votações | meeting | **Voto por unidade** (`tb_poll_vote` guarda usuário) e autenticação (2.1) |
| 14 | Ocorrências e ordens de serviço | `ocorrencias-service` (a criar) | **Ordem de serviço, responsável e prazo.** Hoje só há a reclamação, no `auth-api`. Decidido em 05/10: o RF-14 não fica no `auth-api` |

### Lacunas dentro de requisitos completos

| RF | O que falta | Efeito |
|---|---|---|
| 4 | **Trava por plano no servidor** | Os módulos fora do plano só somem da interface; a API continua respondendo |
| 18 | **Assembleias nos painéis** | O painel estratégico não traz participação em assembleias: o meeting-service não autentica nem filtra por condomínio (2.1) |
| 18 | **Contagem agregada do portaria** | Ocorrências, leitura de avisos e multas já chegam agregadas no banco (RNF-20). Só entregas, reservas e presença ainda são contadas no gestao-geral: falta um endpoint de contagem no portaria |

---

## 4. Requisitos não-funcionais

| # | Requisito | O que falta |
|---|---|---|
| RNF-01 | JWT em todo endpoint | `meeting-service` (2.1) |
| RNF-02 | Segredos sem default no repositório | ✅ Resolvido nos arquivos: o compose exige `POSTGRES_PASSWORD` e `JWT_SECRET`, e nenhum serviço tem default. A senha antiga segue no histórico do Git |
| RNF-03 | Autorização no servidor | Escrita do portaria (2.2), meeting (2.1) e trava por plano (RF-4) |
| RNF-16 | Filtro por condomínio em toda consulta | Escrita do portaria (2.2) e todo o meeting |

---

## 5. Testes

| Serviço | Situação |
|---|---|
| `comunicacao-service` | 26 testes JUnit: público-alvo, token da rota interna e deduplicação, validação de imagem, relatório de leitura, segredo do JWT |
| `plan-service` | Nenhum teste |
| `portaria-service` | 2 arquivos — não cobrem a autorização |
| `meeting-service` | 1 arquivo |
| `financeiro` | Contrato de acesso só das rotas de multas, contratos e prestação; as rotas de faturas, taxas e contas de consumo não têm |

O `auth-api` (441 testes) é o único com contrato de acesso cobrindo todas as
rotas. O `gestao-geral` tem os painéis e o acesso a eles cobertos (23 testes).

**Teste de API ponta a ponta:** `tests-api/`, collection Postman rodada pelo
Newman contra a stack no ar: 50 requisições e 98 asserções. Cobre auth-api,
comunicacao, financeiro e gestao-geral, e apaga o que criou.

---

## 6. Repositório e infraestrutura

| Item | Onde | O que fazer |
|---|---|---|
| `credentials.json` do meeting | Lido do classpath | Montar por volume, como `tokens/`; sem ele o serviço não sobe |
| Chave PIX | Conta Asaas | Cadastrar no painel do Asaas; até lá, só boleto funciona |
| `ASAAS_WEBHOOK_TOKEN` | `docker/.env` e painel do Asaas | Gerar e configurar nos dois lados; vazio, o webhook não confere quem chama |
| `ocorrencias-service` | — | Não existe (ver RF-14) |

Resolvidos em 18/10: senha do Postgres e `JWT_SECRET` fora dos arquivos;
`node_modules/`, `logs/` e `services/meeting/target` fora do índice; código morto
(`vagas-service`, `portaria-frontend`, `.idea`) removido; `.mailmap`; tratamento
de erro `async` nas rotas antigas do financeiro; `auth-api` registrado no Consul.

---

## 7. Documentação a atualizar

| Documento | O que está errado |
|---|---|
| [ESPECIFICACAO-PROJETO.md](ESPECIFICACAO-PROJETO.md) | Fala em 6 perfis (são 7, com `TERCEIRO`); diz que avisos e conhecimento ficam no portaria (estão no comunicacao); tabela de status dos RFs e roadmap desatualizados |
| [HISTORIAS-DE-USUARIO.md](HISTORIAS-DE-USUARIO.md) | Numeração de RF diferente da spec — lá o RF-01 é *Planos*, na spec é *Autenticação* |

---

## 8. Ordem sugerida

### Defeitos

1. Validar atualização do `meeting-service` na stack (2.1)
2. Perfil e condomínio na escrita do portaria (2.2)

### Escopo, por custo-benefício

3. Voto por unidade — implementado; validar atualização e histórico legado com o item 1
4. Notificar entrega (RF-7) — a rota interna do comunicacao já existe
5. Antecedência mínima na reserva (RF-10)
6. Ordem de serviço no `ocorrencias-service` (RF-14)

Com os itens 1 a 5 feitos: **17 de 18 completos (94%)**. O 6 fecha os 18.

---

## 9. Decisões pendentes

| Decisão | Contexto |
|---|---|
| Versionar a documentação de sprints | Está em `atividades/Gestao-de-Projetos/` (`.md`), mas `atividades/` inteira está no `.gitignore`. Sugestão: versionar só essa subpasta |
| Trava por plano no servidor | Hoje só a interface consulta o plano |
