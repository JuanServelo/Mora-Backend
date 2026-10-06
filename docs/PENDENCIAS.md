# Pendências do projeto

O que falta no Mora, medido contra a [especificação](ESPECIFICACAO-PROJETO.md) e
os critérios de avaliação.

| Repositório | Base |
|---|---|
| Backend | `fa5bcf5` + multas, contratos, prestação de contas, painéis e limitador (05/10, ainda sem commit) |
| Frontend | `32f789b` + telas correspondentes (05/10, ainda sem commit) |

---

## 1. Resumo

| Requisitos funcionais | Quantidade |
|---|---|
| ✅ Completos | 13 |
| ⚠️ Parciais | 5 |
| 📋 Não iniciados | 0 |

**72% completos**, ou ~86% contando parcial como meio ponto. A meta do critério
1 é 80%.

### Critérios de avaliação pendentes

| # | Critério | O que falta |
|---|---|---|
| 1 | Pelo menos 80% do escopo | Faltam 5 requisitos parciais (seção 3) |
| 2 | Processo ágil documentado | As sprints estão em `atividades/`, fora do repositório (seção 9) |
| 6 | 2 perfis validados no front **e** no back | `meeting-service` sem autenticação e escrita do portaria sem perfil (seção 2) |

Os critérios 3, 4, 5, 7 e 8 estão atendidos.

---

## 2. Defeitos — prioridade alta

### 2.1 `meeting-service` sem autenticação

O serviço não tem filtro nem leitura de JWT. Qualquer pessoa que alcance a porta
cria reunião, lança ata e vota.

O voto recebe **`usuarioId` no corpo** (`VoteRequestDTO`), então dá para votar
como qualquer pessoa trocando um número.

**Correção:** copiar o `AuthFilter` do `plan-service`, tirar `usuarioId` do DTO
e ler o usuário da claim.

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

### 2.3 Notificações do financeiro não chegam

O financeiro publica fatura fechada e vencida em
`POST /api/comunicacao/interno/notificacoes`, com `X-Servico-Token`. O
`comunicacao-service` **não tem essa rota**, e o `AuthFilter` dele exige Bearer
em tudo que não é `/actuator`. O financeiro registra um `warn` e segue, então a
falha é silenciosa: **o morador não recebe aviso de cobrança**.

**Correção:** criar a rota interna no comunicacao:

- autenticada por `X-Servico-Token`, com comparação em tempo constante, e
  liberada no `AuthFilter`
- sem `SERVICO_TOKEN` configurado, responde **503** (fechada, não aberta)
- coluna `chave_unica` com índice único parcial; reenvio com a mesma chave
  responde **200 com `repetida: true`**
- colunas `origem` e `dados` (JSONB) em `notificacoes`

---

## 3. Requisitos funcionais incompletos

### Parciais

| RF | Requisito | Serviço | O que falta |
|---|---|---|---|
| 7 | Entregas | portaria | **Notificar o destinatário** ao registrar a encomenda |
| 10 | Reservas | portaria | **Antecedência mínima** para solicitar |
| 11 | Assembleias e votações | meeting | **Voto por unidade** (`tb_poll_vote` guarda usuário) e autenticação (2.1) |
| 13 | Mensagens e notificações | comunicacao | **Notificação disparada por evento** (2.3) |
| 14 | Ocorrências e ordens de serviço | `ocorrencias-service` (a criar) | **Ordem de serviço, responsável e prazo.** Hoje só há a reclamação, no `auth-api`. Decidido em 05/10: o RF-14 não fica no `auth-api` |

### Lacunas dentro de requisitos completos

| RF | O que falta | Efeito |
|---|---|---|
| 4 | **Trava por plano no servidor** | Os módulos fora do plano só somem da interface; a API continua respondendo |
| 12 | **Recorte por `publicoAlvo`** | O morador recebe comunicado dirigido a funcionário |
| 12 | **`GET /avisos/leituras`** | A aba de confirmação de leitura de Comunicados fica sem o panorama |
| 12 | **`POST /avisos/imagem`** | O campo de imagem do aviso não envia |
| 18 | **Assembleias nos painéis** | O painel estratégico não traz participação em assembleias: o meeting-service não autentica nem filtra por condomínio (2.1) |
| 18 | **Contagem agregada no banco** | Os painéis contam sobre as listas que as fontes devolvem, no gestao-geral. O RNF-20 pede agregação no banco; exige endpoints de contagem nos serviços Java |

---

## 4. Requisitos não-funcionais

| # | Requisito | O que falta |
|---|---|---|
| RNF-01 | JWT em todo endpoint | `meeting-service` (2.1) |
| RNF-02 | Segredos sem default no repositório | `jwt.secret` com default `changeme-insecure-default` no comunicacao; senha do Postgres em 15 arquivos (seção 6) |
| RNF-03 | Autorização no servidor | Escrita do portaria (2.2), meeting (2.1) e trava por plano (RF-4) |
| RNF-16 | Filtro por condomínio em toda consulta | Escrita do portaria (2.2) e todo o meeting |

---

## 5. Testes

| Serviço | Situação |
|---|---|
| `comunicacao-service` | Nenhum teste automatizado; só `verificar-autorizacao.mjs`, rodado à mão |
| `plan-service` | Nenhum teste |
| `portaria-service` | 2 arquivos — não cobrem a autorização |
| `meeting-service` | 1 arquivo |
| `financeiro` | Contrato de acesso só das rotas de multas, contratos e prestação; as rotas de faturas, taxas e contas de consumo não têm |

O `auth-api` (425 testes) é o único com contrato de acesso cobrindo todas as
rotas. O `gestao-geral` tem os painéis e o acesso a eles cobertos (22 testes).

---

## 6. Repositório e infraestrutura

| Item | Onde | O que fazer |
|---|---|---|
| Senha do Postgres versionada | 15 arquivos: compose, `application.*` de 5 serviços, READMEs e docs | Rotacionar e trocar por placeholder |
| `node_modules/` rastreado | Backend, 5.881 arquivos | Remover do índice e ignorar |
| `logs/plan-service.log` rastreado | Backend | Remover do índice e ignorar |
| `credentials.json` do meeting | Lido do classpath | Montar por volume, como `tokens/`; sem ele o serviço não sobe |
| Código morto | `services/vagas-service`, `services/portaria-frontend`, `services/.idea` | Remover |
| Identidades git duplicadas | Ambos os repositórios | Criar `.mailmap` |
| Chave PIX | Conta Asaas | Cadastrar no painel do Asaas; até lá, só boleto funciona |
| Controllers `async` sem tratamento de erro | `financeiro`, rotas antigas (faturas, cadastros, contas de consumo) | No Express 4, um erro ali deixa a requisição sem resposta e derruba o processo. As rotas novas já usam `utils/assincrono.js`; estender às antigas |
| `ocorrencias-service` | — | Não existe (ver RF-14) |

---

## 7. Documentação a atualizar

| Documento | O que está errado |
|---|---|
| [ESPECIFICACAO-PROJETO.md](ESPECIFICACAO-PROJETO.md) | Fala em 6 perfis (são 7, com `TERCEIRO`); diz que avisos e conhecimento ficam no portaria (estão no comunicacao); tabela de status dos RFs e roadmap desatualizados |
| [HISTORIAS-DE-USUARIO.md](HISTORIAS-DE-USUARIO.md) | Numeração de RF diferente da spec — lá o RF-01 é *Planos*, na spec é *Autenticação* |

---

## 8. Ordem sugerida

### Defeitos

1. Autenticação no `meeting-service` (2.1)
2. Perfil e condomínio na escrita do portaria (2.2)
3. Rota interna de notificações no comunicacao (2.3)

### Escopo, por custo-benefício

4. Voto por unidade (RF-11) — mesmos arquivos do item 1
5. Recorte por `publicoAlvo` (RF-12)
6. Notificar entrega (RF-7) — depende do item 3
7. Antecedência mínima na reserva (RF-10)
8. Ordem de serviço no `ocorrencias-service` (RF-14)

Com os itens 1 a 7 feitos: **17 de 18 completos (94%)**. O 8 fecha os 18.

---

## 9. Decisões pendentes

| Decisão | Contexto |
|---|---|
| Versionar a documentação de sprints | Está em `atividades/Gestao-de-Projetos/` (`.md`), mas `atividades/` inteira está no `.gitignore`. Sugestão: versionar só essa subpasta |
| Trava por plano no servidor | Hoje só a interface consulta o plano |
