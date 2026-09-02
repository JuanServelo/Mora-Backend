# 📊 Relatório de Análise — Microsserviços do Mora

**Projeto:** Mora — Backend de gestão condominial
**Data:** 19/08/2026
**Escopo:** avaliação individual dos 5 microsserviços
**Tecnologias:** Node.js 20 · Express 4 · Sequelize 6 · Java 21 · Spring Boot 3.2/3.5 · PostgreSQL 16 · Consul 1.15 · Traefik 2.9 · Docker Compose

> Complementa [ARQUITETURA-E-FLUXOS.md](ARQUITETURA-E-FLUXOS.md), que descreve o funcionamento.
> Este documento **pontua** cada serviço e diz o que fazer para melhorar.
>
> Metodologia: 5 categorias por serviço, com pesos — Segurança 25%, Qualidade de Código 25%,
> Organização 20%, Velocidade 15%, Banco de Dados 15%.

---

## 🏆 Placar geral

| # | Serviço | 🔐 Seg | 🧹 Qual | 📁 Org | ⚡ Vel | 🗄️ BD | **Final** | Nível |
|---|---|---|---|---|---|---|---|---|
| 1 | **auth-api** | 7.0 | 7.0 | 6.5 | 6.0 | 6.5 | **6.7** | Regular/Bom |
| 2 | **meeting** | 2.0 | 7.0 | 7.5 | 5.5 | 4.5 | **5.3** | Regular |
| 3 | **plan-service** | 1.5 | 6.0 | 7.0 | 5.0 | 4.5 | **4.7** | Ruim |
| 4 | **portaria-service** | 2.5 | 5.5 | 6.5 | 3.5 | 3.5 | **4.4** | Ruim |
| 5 | **vagas-service** | 1.5 | 4.5 | 4.0 | 3.0 | 3.0 | **3.2** | Ruim |

**Média do backend: 4.9/10**

O padrão é claro e consistente: **a arquitetura e a organização do código são de nível
razoável a bom (6–7,5), mas a segurança de 4 dos 5 serviços está entre 1,5 e 2,5.** O
`auth-api` construiu um modelo de autorização sofisticado que nenhum outro serviço consome —
e é isso que derruba a média.

```
Segurança     ███░░░░░░░  2.9  ← gargalo
Velocidade    ████▌░░░░░  4.6
Banco         ████▍░░░░░  4.4
Qualidade     ██████░░░░  6.0
Organização   ██████▎░░░  6.3
```

---

# 1. 🥇 auth-api — **6.7/10**

`Node.js 20 · Express · Sequelize` — 37 arquivos, ~4.265 linhas

O serviço mais maduro do repositório e o único que efetivamente protege seus dados. É também
o mais complexo em regra de negócio.

## 🔐 Segurança — 7.0/10

**✅ Pontos positivos**
- `bcryptjs` com cost 10, aplicado via hook `beforeSave` — impossível salvar senha em claro por engano
- `defaultScope` do modelo `User` exclui `senha`, `resetToken` e `resetTokenExpira` de toda consulta; acesso exige scope explícito (`withPassword`) — excelente padrão
- Aborta o boot se `JWT_SECRET` não estiver definido (`server.js:36`) — único serviço que faz isso
- Revogação de token via `tokenVersion`, comparada a cada request
- `express-rate-limit` em login, forgot-password, reset-password e validação de convite
- `helmet` + CORS restrito por `FRONTEND_URL`, com `credentials: true`
- Matriz de autorização granular com 11 perfis e delegação hierárquica (`PERMISSOES_CADASTRO`)
- Convite com expiração de 48h, código único de 12 chars, estados controlados
- Upload com `memoryStorage` e limite de 5 MB, reprocessado por `sharp`
- Login Google não cria conta — exige convite prévio e valida status

**⚠️ Problemas encontrados**
- CPF em texto plano em `users.cpf` e `invites.cpfPrecadastro`, sem criptografia nem retenção
- `express-session` reutiliza o `JWT_SECRET` como secret de sessão (`server.js:99`) — dois
  domínios criptográficos compartilhando chave
- `cookie: { secure: false }` fixo — sem alternância por ambiente
- `multer` sem `fileFilter` de mimetype (mitigado pelo reprocessamento via `sharp`, mas a
  validação deveria ser explícita)
- `multer@1.4.5-lts` — a linha 1.x está descontinuada
- Herda o `JWT_SECRET` default do `docker-compose.yml` quando `.env` não existe

**🔧 Recomendações**
1. Separar `SESSION_SECRET` do `JWT_SECRET` e definir `cookie.secure` por `NODE_ENV`
2. Criptografar CPF em repouso (`pgcrypto` ou envelope encryption na aplicação) e definir prazo de descarte
3. Adicionar `fileFilter` restringindo a `image/jpeg|png|webp` no multer; migrar para multer 2.x
4. Rodar `npm audit` no CI e falhar o build em vulnerabilidade `high`

## 🧹 Qualidade de Código — 7.0/10

**✅ Pontos positivos**
- Separação real de camadas: `routes/` → `services/` → `models/`, com `middleware/`, `utils/` e `constants/`
- Regra de negócio isolada em `services/` (`inviteService`, `occupantService`, `userManagementService`)
- Testes reais com Jest + Supertest (`auth.test.js`, `occupants.test.js`) — **único serviço com testes de integração**
- Formato de resposta consistente (`{ sucesso, mensagem }`) em todas as rotas
- Validadores e setters nos modelos (normalização de e-mail para lowercase, trim de CPF)
- Colunas legadas explicitamente marcadas com `@deprecated` / `Legado` no código
- Uso de `sequelize.transaction()` no fluxo crítico de ativação de conta

**⚠️ Problemas encontrados**
- **Sem linter e sem formatter** — nenhum `.eslintrc`, nenhum Prettier
- Cobertura baixa: 2 arquivos de teste para 37 de código; `inviteService.js` (360 linhas) e
  `userManagementService.js` (256) não têm teste unitário
- `routes/portaria.js` mistura camadas: middlewares inline, raw SQL e mapeamento no mesmo arquivo
- Migrações ad-hoc executadas no boot do processo (`migrate-rf03`, `migrate-rf07`,
  `migrate-condominios`, `migrate-portaria`) — imperativas, não idempotentes por design, sem ordem garantida
- `auth.js` com 359 linhas concentrando login, OAuth, perfil, foto e recuperação de senha
- Sem tipagem (nem TypeScript nem JSDoc sistemático)

**🔧 Recomendações**
1. Adicionar ESLint (`airbnb-base`) + Prettier e rodar no CI — ganho imediato, custo baixo
2. Quebrar `routes/auth.js` em `auth`, `profile` e `password-recovery`
3. Cobrir `inviteService` e `userManagementService` com testes unitários — são o coração da regra de negócio
4. Substituir os scripts `migrate-*.js` por migrations versionadas do `sequelize-cli`
5. Extrair a lógica de `routes/portaria.js` para `services/portariaService.js`

## 📁 Organização — 6.5/10

**✅** Estrutura idiomática Express, `constants/perfis.js` centralizando todo o domínio de
autorização, Dockerfile enxuto com `npm ci --omit=dev`, `.gitignore` cobrindo `.env` e `credentials.json`.

**⚠️** Sem README próprio; sem CI/CD (nenhum `.github/workflows`); `uploads/avatars` versionado
no repositório; `package.json` de teste com caminho relativo frágil (`../../node_modules/jest/bin/jest.js`);
Dockerfile roda como root, sem `USER node`.

**🔧**
1. Criar `services/auth-api/README.md` com variáveis de ambiente e rotas
2. Adicionar `USER node` ao Dockerfile e `.dockerignore`
3. Remover `uploads/` do versionamento e montar como volume
4. Pipeline mínima: `lint → test → build`

## ⚡ Velocidade — 6.0/10

**✅** Rate limiting; índices declarados no `init-databases.sql` para `email`, `googleId`,
`condominioId`, `unidadeId`; query de último acesso otimizada com `DISTINCT ON` (evita N+1 real).

**⚠️** Nenhuma listagem paginada exceto um `limit: 50` isolado em `portaria.js:267`;
`User.findByPk` a cada request autenticado sem cache; sem Redis nem cache em memória;
`GET /guests` carrega todos os convidados do condomínio de uma vez.

**🔧**
1. Paginação (`limit`/`offset` + total) em `/users`, `/invites` e `/guests`
2. Cache curto (30–60s) do usuário autenticado, invalidado por `tokenVersion`
3. Índice composto em `registros_acesso("condominioId", "usuarioId", "createdAt" DESC)` — o `DISTINCT ON` depende dele

## 🗄️ Banco de Dados — 6.5/10

**✅** Sequelize com validações declarativas; associações completas em `models/index.js`;
índices explícitos; transação no fluxo de ativação; raw SQL parametrizado via `replacements`
(sem risco de SQLi); `defaultScope` protegendo colunas sensíveis.

**⚠️** Sem migrations versionadas; duplo modelo ativo (`role` deprecated + `perfil`,
`bloco`/`apartamento`/`vaga` strings + `unidadeId`); `reclamacoes.interactions` é JSON não
estruturado (histórico não consultável); `condominioId` como `STRING(50)` sem FK para `condominios`.

**🔧**
1. Adotar `sequelize-cli` migrations e congelar o schema atual como baseline
2. Migrar dados de `bloco`/`apartamento` para `unidadeId` e dropar as colunas legadas
3. Normalizar `interactions` em tabela `reclamacao_interacoes`
4. Criar FK real `users.condominioId → condominios.id`

---

# 2. 🥈 meeting — **5.3/10**

`Java 21 · Spring Boot 3.2.4` — 40 arquivos, ~1.648 linhas

**Tecnicamente o melhor código Java do repositório.** Perde quase 2 pontos inteiros na nota
final por não ter absolutamente nenhuma camada de autenticação.

## 🔐 Segurança — 2.0/10

**✅** CORS restrito a `localhost:5173` e `localhost:3000`; `credentials.json` e `tokens/`
corretamente no `.gitignore`.

**⚠️ Problemas encontrados**
- **Zero autenticação.** Nenhum filtro, nenhum `spring-boot-starter-security`, nenhuma
  verificação de token. Qualquer requisição cria, edita ou apaga reuniões e atas
- Porta `8091` publicada diretamente no host
- **Atas são documentos oficiais do condomínio** (`decisoesTomadas`, `topicosDiscutidos`) e
  estão totalmente editáveis sem identificação
- Votos em enquetes (`PollVote.usuarioId`) aceitos sem provar identidade — **qualquer um vota
  como qualquer morador**, quantas vezes quiser
- `show-sql=true` ativo — vaza estrutura e valores nos logs
- Credenciais OAuth do Google carregadas sem escopo mínimo documentado

**🔧 Recomendações**
1. **Prioridade máxima:** adicionar filtro JWT lendo o mesmo `JWT_SECRET`, espelhando o
   `AuthFilter` do portaria — mas com rejeição real (`401`), não silenciosa
2. Restringir criação de reunião/ata aos perfis `*_SYNDIC` e `ADMINISTRATOR`
3. Constraint `UNIQUE(poll_id, usuario_id)` em `tb_poll_vote` para impedir voto duplicado
4. Desligar `show-sql` fora de dev (`application-dev.properties`)

## 🧹 Qualidade de Código — 7.0/10

**✅ Pontos positivos**
- **`@Transactional` usado com consistência** — 8 em `MeetingService`, 7 em `PollService`, 3 em `AtaService`
- **MapStruct** para mapeamento entidade↔DTO — o único serviço que evita mapeamento manual
- `GlobalExceptionHandler` centralizado em `infra/`
- 11 DTOs, enums bem definidos (`MeetingStatus`, `AttendanceStatus`, `PollStatus`)
- `FetchType.LAZY` em todos os relacionamentos — decisão correta e deliberada
- Swagger/OpenAPI configurado (`springdoc`) — única documentação de API do projeto
- HikariCP tunado com comentários explicando cada parâmetro

**⚠️** Apenas smoke test (`MeetingApplicationTests`); `@Valid` em só 3 arquivos;
`MeetingService` (244 linhas) acopla regra de negócio à chamada da API do Google, sem interface
de abstração — impossível testar sem rede.

**🔧**
1. Extrair a integração Google para `MeetingProvider` (interface + impl), permitindo mock em teste
2. Testes de `PollService` (apuração de votos é lógica com risco real de erro)
3. `@Valid` em todos os `@RequestBody`

## 📁 Organização — 7.5/10 — *melhor nota de organização do projeto*

**✅** Pacotes completos e coerentes: `config`, `controller`, `dto`, `entity`, `enums`,
`infra`, `mapper`, `repository`, `service`. Dockerfile multi-stage. Swagger. Prefixo `tb_`
consistente nas tabelas.

**⚠️** Sem README; Spring Boot **3.2.4** enquanto portaria e vagas usam 3.5.6; sem CI.

**🔧**
1. Alinhar em Spring Boot 3.5.6 (ou criar um POM pai com BOM compartilhado)
2. README documentando o setup das credenciais Google

## ⚡ Velocidade — 5.5/10

**✅** Único serviço com pool de conexões explicitamente configurado; `LAZY` em todos os relacionamentos.

**⚠️** Sem paginação (listar reuniões carrega o histórico inteiro); `show-sql=true` custa I/O;
chamada síncrona à API do Google dentro do fluxo de criação, **sem timeout, retry ou circuit
breaker** — se o Google demorar, o request trava; sem índice em `dataHoraInicio`, que é o
filtro natural.

**🔧**
1. `Pageable` em `GET /meetings`, ordenado por `dataHoraInicio DESC`
2. Timeout + retry com backoff na chamada Google; considerar criação assíncrona do link
3. Índice em `tb_meetings(dataHoraInicio)` e em `tb_poll_vote(poll_id)`

## 🗄️ Banco de Dados — 4.5/10

**✅** `@Transactional` consistente; `LAZY` correto; `UNIQUE` em `googleEventId` e no
`meeting_id` da ata (garante 1:1); collection tables bem modeladas; `orphanRemoval` nas opções de enquete.

**⚠️** `ddl-auto=update` sem migrations; **nenhuma tabela tem `condominioId`** — reuniões são
globais entre condomínios; IDs de pessoa como `Long`, sem correspondência com `users.id`; sem
índices; `tb_poll_vote` sem constraint de unicidade por votante.

**🔧**
1. Adicionar `condominioId` a `tb_meetings` e propagar por herança lógica às demais
2. `UNIQUE(poll_id, usuario_id)` em `tb_poll_vote`
3. Flyway com baseline do schema atual

---

# 3. 🥉 plan-service — **4.7/10**

`Java 21 · Spring Boot 3.2.4` — 12 arquivos, ~534 linhas

O menor serviço, e o de escopo mais limpo. Mas guarda os **parâmetros comerciais do SaaS** sem
nenhuma proteção — o que é desproporcionalmente grave para o tamanho dele.

## 🔐 Segurança — 1.5/10 — *a nota mais baixa do projeto*

**⚠️ Problemas encontrados**
- **Zero autenticação**, porta `8093` exposta
- O serviço define `monthlyPrice`, `maxCondominiums`, `maxUsersPerCondominium` e
  `activeModules` — **os limites comerciais e o preço do produto**. Um `PUT /plans/{id}` sem
  token altera o preço do plano ou libera módulos pagos para todos os clientes
- `show-sql=true` em produção
- Não existe trilha de auditoria de quem alterou um plano

**🔧 Recomendações**
1. **Isolar o serviço da rede pública.** Remover a publicação da porta no `docker-compose.yml`;
   deixá-lo acessível apenas pela rede interna `microservices-net`
2. Escrita restrita a `CONTRACTING_PROPERTY_MANAGER` via filtro JWT; leitura autenticada
3. Tabela de auditoria (`plan_id`, `campo`, `valorAnterior`, `valorNovo`, `alteradoPor`, `em`)
4. Desligar `show-sql`

## 🧹 Qualidade de Código — 6.0/10

**✅** `Plan.java` é a entidade mais bem escrita do repositório: `@PrePersist`/`@PreUpdate`
para timestamps, `@Builder.Default`, `nullable` e `precision` explícitos em cada coluna,
`BigDecimal(10,2)` correto para dinheiro, `unique` no nome. `GlobalExceptionHandler`,
`@Transactional` (6 usos), mapper dedicado, escopo coeso.

**⚠️** **Nenhum teste** — nem smoke test de contexto; `@Valid` em apenas 1 arquivo; só 3 DTOs.

**🔧**
1. Testes de `PlanService` — são poucas regras, cobertura alta é barata aqui
2. `@Valid` + Bean Validation em `PlanRequestDTO` (`@Positive` em `maxCondominiums`, `@DecimalMin` em `monthlyPrice`)

## 📁 Organização — 7.0/10

**✅** Mesma estrutura de pacotes do `meeting` (`config`/`controller`/`dto`/`entity`/`enums`/
`infra`/`mapper`/`repository`/`service`) — consistência entre serviços é um acerto real.
Dockerfile presente. Escopo pequeno e bem delimitado.

**⚠️** Sem README; Spring Boot 3.2.4; sem CI; sem seed — **o SaaS sobe sem nenhum plano cadastrado**.

**🔧**
1. Seed com os planos padrão (Básico/Pro/Enterprise) no boot ou via `data.sql`
2. Alinhar versão do Spring Boot

## ⚡ Velocidade — 5.0/10

**✅** Volume de dados naturalmente pequeno — poucos planos, raramente alterados.

**⚠️** `@ElementCollection(fetch = EAGER)` em `activeModules` carrega a tabela de módulos em
toda consulta; **dado quase-imutável e lido com altíssima frequência (toda checagem de limite)
sem nenhum cache** — é o caso de cache mais óbvio do sistema inteiro; sem paginação; `show-sql=true`.

**🔧**
1. `@Cacheable` (Caffeine ou Redis) em `findAll`/`findById`, invalidado na escrita
2. Manter `EAGER` (correto aqui pelo volume), mas medir após o cache

## 🗄️ Banco de Dados — 4.5/10

**✅** Schema declarado com rigor (nullable, precision, unique); `@Transactional`; timestamps
por callback de ciclo de vida.

**⚠️** `ddl-auto=update` sem migrations; **não existe entidade de assinatura/contrato** — há
o catálogo de planos, mas nada liga um `condominio` a um `plan`. Os limites do SaaS, portanto,
**não são aplicáveis**: não há como saber qual plano cada condomínio tem; tabela sem seed.

**🔧**
1. **Criar a entidade `Subscription`** (`condominioId`, `planId`, `dataInicio`, `dataFim`,
   `status`) — sem ela o `plan-service` é um catálogo decorativo
2. Implementar a checagem efetiva de `maxUsersPerCondominium` no fluxo de convite do `auth-api`
3. Flyway + seed

---

# 4. portaria-service — **4.4/10**

`Java 21 · Spring Boot 3.5.6` — 94 arquivos, ~4.285 linhas, 19 controllers

O maior e mais central dos serviços Java. Tem a melhor infraestrutura de código do lado Java
(exceções de domínio, DTOs, validação ampla) e, ao mesmo tempo, o problema de segurança mais
enganoso do projeto: **parece protegido e não está**.

## 🔐 Segurança — 2.5/10

**✅** Único serviço Java com infraestrutura de JWT (`AuthFilter`, `JwtUtil`, `JwtClaims`,
`AuthContext`); `@Valid` em 14 arquivos; CORS restrito; exceção `AcessoNegadoException` definida.

**⚠️ Problemas encontrados**
- **O `AuthFilter` não bloqueia nada.** Token inválido cai em `catch (Exception ignored)` e a
  requisição segue. A rejeição foi delegada aos services — e **`AuthContext.get()` é chamado
  em 1 de 19 services** (`VeiculoService.java:54`). Os outros 18 controllers
  (`Morador`, `Visitante`, `Funcionario`, `Entrega`, `Chave`, `Bloco`, `Apartamento`, `Turno`,
  `Aviso`, `ArtigoConhecimento`, `Vaga`, `Carro`, `AreaComum`, `Usuario`…) não consultam contexto algum
- Porta `8090` publicada no host; o único frontend do repositório aponta direto para ela **sem
  header `Authorization`**
- `jwt.secret: ${JWT_SECRET:-changeme-insecure-default}` — default aceito silenciosamente
- Dados pessoais expostos sem token: CPF de moradores, funcionários e visitantes; documento de
  visitantes; histórico de entrada/saída
- Sem `condominioId` em `moradores`, `funcionarios`, `visitantes`, `entregas`, `chaves`,
  `turnos`, `carros`, `veiculos` — **vazamento entre condomínios mesmo se a auth for corrigida**

**🔧 Recomendações**
1. **Fazer o `AuthFilter` rejeitar.** Trocar o `catch (Exception ignored)` por resposta `401`,
   com allowlist explícita de rotas públicas (`/actuator/health`)
2. Adotar `spring-boot-starter-security` com `@PreAuthorize` por perfil, em vez de checagem
   manual espalhada pelos services
3. Remover o default de `jwt.secret` — falhar o boot se ausente, como faz o `auth-api`
4. Propagar `condominioId` e aplicar filtro de tenant no repositório (Hibernate `@Filter`)
5. Despublicar a porta 8090; expor só via Traefik

## 🧹 Qualidade de Código — 5.5/10

**✅ Pontos positivos**
- Camadas completas: `controller`/`service`/`repository`/`dto`/`exception`/`security`/`config`
- 19 DTOs, com subpacote (`dto/chave`) — melhor cobertura de DTOs do projeto
- Exceções de domínio próprias: `RecursoNaoEncontradoException`, `OperacaoInvalidaException`,
  `AcessoNegadoException` + `GlobalExceptionHandler`
- `@Valid` em 14 arquivos — a melhor cobertura de validação entre os serviços Java
- `BlocoServiceTest.java` — único teste unitário de regra de negócio em Java no repositório

**⚠️ Problemas encontrados**
- **Zero `@Transactional` em todo o serviço.** Operações multi-passo (registrar entrada de
  veículo + atualizar vaga; criar apartamento + vagas em cascata) rodam sem atomicidade
- `Carro` e `Veiculo` são a mesma entidade duplicada, ambas com `placa` UNIQUE em tabelas
  distintas — a mesma placa pode ter estados de acesso contraditórios
- `VeiculoService.java` com 352 linhas
- Teste órfão em `src/main/test/` — diretório inválido no Maven, nunca executado
- Tipagem frágil: `Entrega.status` é `String` com default `"PENDENTE"` enquanto todo o resto
  usa enum; `Vaga.tipo` é `String` com o comentário `// Coberta, Descoberta, etc`
- Escopo inflado: o serviço "portaria" acumula avisos, base de conhecimento, áreas comuns e
  turnos — responsabilidades que não são de portaria

**🔧 Recomendações**
1. Adicionar `@Transactional` em todos os métodos de escrita dos services — correção pontual, impacto alto
2. Fundir `Carro` em `Veiculo` (migration de dados + remoção da entidade)
3. Converter `Entrega.status` e `Vaga.tipo` em enums
4. Apagar `src/main/test/`
5. Considerar extrair `Aviso` + `ArtigoConhecimento` para um `comunicacao-service`

## 📁 Organização — 6.5/10

**✅** Melhor organização de pacotes entre os serviços de domínio; Dockerfile multi-stage
(builder Maven → runtime); tags do Traefik declaradas na config; `application-local.yml` separado.

**⚠️** Sem README apesar de ser o maior serviço; `src/main/test/` inválido; sem CI;
19 controllers sem agrupamento por subdomínio; Dockerfile usa `21-jdk` no runtime (deveria ser
`21-jre`, imagem ~200 MB menor) e roda como root; `mvn package -DskipTests` no build — os
testes que existem nunca rodam.

**🔧**
1. README com as rotas dos 19 controllers e as variáveis de ambiente
2. Trocar runtime para `eclipse-temurin:21-jre` + `USER`
3. Remover `-DskipTests` ou rodar os testes em etapa de CI

## ⚡ Velocidade — 3.5/10

**⚠️ Problemas encontrados**
- **Nenhuma paginação em nenhum dos 19 controllers** — `findAll()` direto em todas as listagens
- **Cascata de `FetchType.EAGER`**: `Veiculo` → `Vaga` → `Apartamento` → `Bloco`. Listar
  veículos dispara a cadeia inteira por registro — N+1 estrutural, não acidental
- Sem índice nas tabelas criadas pelo `ddl-auto` (`visitantes`, `entregas`, `veiculos`,
  `chaves`, `turnos`) — as consultas centrais (`status = DENTRO`, `status = PENDENTE`) são full scan
- Pool de conexões no default do Spring Boot
- Sem cache em dados quase-estáticos (`blocos`, `apartamentos`, `areas_comuns`)

**🔧 Recomendações**
1. `Pageable` em todos os endpoints de listagem — maior ganho isolado de performance do projeto
2. Trocar `EAGER` por `LAZY` + `@EntityGraph` nas consultas que realmente precisam do relacionamento
3. Índices em `visitantes(status)`, `entregas(status, apartamento)`, `veiculos(status, placa)`,
   `moradores(apartamento_id)`
4. Cache de leitura em `blocos` / `apartamentos` / `areas_comuns`

## 🗄️ Banco de Dados — 3.5/10

**✅** Spring Data JPA bem usado; `UNIQUE(nome, condominioId)` em `blocos` (o único acerto de
tenancy no schema); herança via `@MappedSuperclass` bem aplicada; `unique` em CPF e placa.

**⚠️** `ddl-auto: update` **com drift já materializado** — `Bloco.sigla` e três colunas de
`AreaComum` existem na entidade e não no `init-databases.sql`; zero `@Transactional`; sem
índices; `areas_comuns.nome` UNIQUE **global** (dois condomínios não podem ter "Salão de
Festas"); relacionamentos por string livre sem FK (`Entrega.bloco`, `Entrega.apartamento`,
`Turno.funcionario`, `Chave.responsavelId`); `condominioId` ausente na maioria das tabelas.

**🔧 Recomendações**
1. **Flyway com baseline do schema atual** e `ddl-auto: validate` daí em diante
2. `UNIQUE(nome, "condominioId")` em `areas_comuns`
3. Converter `Entrega.bloco`/`apartamento` em FKs reais para `blocos`/`apartamentos`
4. Índices conforme §Velocidade

---

# 5. vagas-service — **3.2/10**

`Java 21 · Spring Boot 3.5.6` — 33 arquivos, ~1.586 linhas

A nota mais baixa. Não por ser mal escrito — o código é razoável — mas porque **o serviço
inteiro é uma duplicação do domínio do `portaria-service`**, opera valores financeiros sem
qualquer autorização, e carrega erros de cópia não revisados.

## 🔐 Segurança — 1.5/10

**✅** CORS restrito a `localhost:5173`.

**⚠️ Problemas encontrados**
- **Zero autenticação**, porta `8092` exposta
- **O serviço movimenta dinheiro.** `AluguelVaga` tem `valorTotal` e `valorPenalidade` em
  `BigDecimal`. Sem token, qualquer requisição cria aluguel em nome de terceiros, **aprova ou
  recusa a solicitação alheia**, e define o valor cobrado
- `solicitanteId` e `proprietarioId` chegam no corpo da requisição, sem validação contra
  identidade autenticada — não há como provar quem pediu o quê
- Nenhum `condominioId` — vagas e aluguéis são globais entre condomínios
- CPF em texto plano na cópia de `moradores`

**🔧 Recomendações**
1. Filtro JWT com rejeição real, antes de qualquer outra coisa
2. **Derivar `solicitanteId` do token**, nunca aceitá-lo do corpo da requisição
3. Autorizar aprovar/recusar apenas ao `proprietarioId` do registro
4. Trilha de auditoria nas transições de estado do aluguel (é registro financeiro)

## 🧹 Qualidade de Código — 4.5/10

**✅** `GlobalExceptionHandler` + exceções de domínio; `@Transactional` em `AluguelService`
(6 usos); 10 DTOs; `BigDecimal(10,2)` correto para valores monetários; enums bem definidos
(`StatusAluguel`, `ModalidadeAluguel`, `TipoVaga`).

**⚠️ Problemas encontrados**
- **`Apartamento`, `Bloco`, `Morador` e `Vaga` são cópias literais das entidades do
  `portaria-service`**, incluindo as crases em `@Column(name = "\`blocoId\`")` — copy-paste sem adaptação
- Teste órfão chamado `PortariaServiceApplicationTests.java` **dentro do vagas-service**, em
  `src/main/test/` (nunca executado) — evidência direta da cópia
- `AluguelService` (299 linhas) e `AluguelController` (214 linhas)
- `@Valid` em apenas 2 arquivos — o serviço aceita datas e valores praticamente sem validação
- Apenas smoke test

**🔧 Recomendações**
1. Remover as entidades duplicadas e consumir bloco/apartamento/morador do `portaria-service`
   via cliente HTTP (ou fundir os dois serviços)
2. Apagar `src/main/test/` e renomear a classe de teste
3. `@Valid` + validação de invariantes: `dataFim > dataInicio`, `valorTotal >= 0`, sobreposição
   de períodos na mesma vaga
4. Extrair a máquina de estados `PENDENTE → APROVADO/RECUSADO` para uma classe própria e testá-la

## 📁 Organização — 4.0/10

**✅** Estrutura de pacotes correta (`config`/`controller`/`dto`/`exception`/`model`/`repository`/`service`);
Dockerfile presente.

**⚠️** A existência do serviço duplica o domínio do `portaria-service` (`Vaga` e
`vagas_estacionamento` existem nos dois, em bancos diferentes, sem sincronização);
`seed-estruturas-fisicas.sql` é **byte a byte idêntico** ao do portaria, incluindo o `\c mora;`
— **popula o banco errado**; sem README; sem CI.

**🔧**
1. **Decisão arquitetural primeiro:** fundir com `portaria-service` ou definir contrato de
   consumo. Manter duas cópias de `vagas_estacionamento` não é sustentável
2. Corrigir ou remover o seed
3. Alinhar o Dockerfile (JRE em runtime, usuário não-root)

## ⚡ Velocidade — 3.0/10

**⚠️** `FetchType.EAGER` em 4 entidades, formando a cadeia `AluguelVaga → Vaga → Apartamento
→ Bloco` — listar aluguéis carrega toda a estrutura física por registro; sem paginação;
**nenhum índice em `alugueis_vagas` e `disponibilidade_vagas`**, embora consulta por período
(`dataInicio`/`dataFim`) seja exatamente a operação central do serviço; sem cache; pool default.

**🔧**
1. Índices compostos em `alugueis_vagas(vaga_id, data_inicio, data_fim)`,
   `alugueis_vagas(status)` e `disponibilidade_vagas(vaga_id, ativa, data_inicio, data_fim)`
2. `LAZY` + `@EntityGraph`
3. Paginação em `GET /alugueis`

## 🗄️ Banco de Dados — 3.0/10

**✅** `BigDecimal(10,2)` para dinheiro (correto e frequentemente errado por aí);
`@Transactional` presente; enums persistidos como `STRING`; `nullable = false` nos campos críticos.

**⚠️** Banco `vagas_db` com cópia não sincronizada de `blocos`, `apartamentos`, `moradores` e
`vagas_estacionamento` do banco `mora` — **divergência garantida ao longo do tempo**;
`ddl-auto: update` sem migrations; sem índices; seed apontando para o banco errado; sem
`condominioId`; `solicitanteId`/`proprietarioId` são `UUID` sem correspondência com o
`users.id` (INTEGER) do `auth_db`; **sem constraint impedindo aluguéis sobrepostos na mesma vaga**.

**🔧 Recomendações**
1. Eliminar as tabelas espelho — a fonte de verdade da estrutura física é o banco `mora`
2. Constraint de exclusão por período (`EXCLUDE USING gist` com `daterange`) para impedir
   duplo aluguel da mesma vaga
3. Padronizar o identificador de pessoa com o do `auth_db`
4. Flyway + índices

---

# 🚀 Próximos passos prioritários

Ordenado por impacto sobre a nota agregada e sobre o risco real.

### 1. **[CRÍTICO]** Rotacionar segredos e remover os defaults do `docker-compose.yml`
`POSTGRES_PASSWORD=Vibers@2112` e `JWT_SECRET=uma-chave-secreta-...` estão commitados. Como
todos os serviços validam HS256 com segredo compartilhado, **quem lê o repositório forja um
token com qualquer perfil**. Custo: baixo. Impacto: elimina a vulnerabilidade mais direta do projeto.

### 2. **[CRÍTICO]** Autenticação real nos 4 serviços Java
Criar um módulo compartilhado `mora-security` (filtro JWT + `@PreAuthorize`) e aplicá-lo em
`portaria`, `meeting`, `vagas` e `plan`. No `portaria-service`, o trabalho é menor do que
parece: a infraestrutura existe, falta **rejeitar** em vez de ignorar. Isso sozinho move a
média de segurança de 2,9 para ~7 e a nota geral do backend de 4,9 para ~6,3.

### 3. **[CRÍTICO]** Despublicar as portas dos serviços internos
Remover `ports:` de `meeting`, `vagas-service` e `plan-service` no compose — o acesso deve ser
só via Traefik pela rede `microservices-net`. Custo: 5 minutos. Reduz drasticamente a superfície
enquanto o item 2 é implementado.

### 4. **[ALTO]** Decidir a fonte única de verdade e eliminar a duplicação
`vagas-service` mantém cópia de `blocos`, `apartamentos`, `moradores` e `vagas_estacionamento`;
`auth-api` e `portaria-service` implementam controle de acesso em paralelo; `Carro` e `Veiculo`
coexistem. Enquanto isso persistir, qualquer correção precisa ser feita em dois lugares.

### 5. **[ALTO]** Flyway em todos os serviços, com baseline do schema atual
Trocar `ddl-auto: update` por `validate`. O drift já é observável (`Bloco.sigla`,
colunas de `AreaComum`). Quanto mais tarde, mais caro.

### 6. **[ALTO]** `@Transactional` no `portaria-service`
Zero ocorrências no maior serviço Java do projeto. Operações multi-passo rodam sem
atomicidade. Correção mecânica, risco baixo, impacto alto em integridade.

### 7. **[ALTO]** Propagar `condominioId` e aplicar filtro de tenant
Sem isso, o produto não é multi-condomínio de verdade — mesmo com autenticação corrigida,
um síndico do condomínio A enxerga entregas e visitantes do B.

### 8. **[MÉDIO]** Paginação e correção do `FetchType.EAGER`
Nenhum endpoint Java pagina; as cadeias `Veiculo → Vaga → Apartamento → Bloco` e
`AluguelVaga → Vaga → Apartamento → Bloco` garantem N+1. Maior ganho de performance por
esforço no projeto.

### 9. **[MÉDIO]** Índices nas tabelas geradas por `ddl-auto`
`visitantes(status)`, `entregas(status, apartamento)`, `alugueis_vagas(vaga_id, data_inicio, data_fim)`,
`tb_meetings(dataHoraInicio)`.

### 10. **[MÉDIO]** Criar a entidade `Subscription` no `plan-service`
Sem o vínculo `condominio → plano`, os limites do SaaS (`maxCondominiums`,
`maxUsersPerCondominium`) são inaplicáveis e o serviço é um catálogo decorativo.

### 11. **[MÉDIO]** CI mínima e ESLint no `auth-api`
Nenhum `.github/workflows`, nenhum linter. Pipeline `lint → test → build` trava regressões
antes do merge — e os Dockerfiles usam `-DskipTests`, então hoje nada é verificado.

### 12. **[BAIXO]** Padronizar versões, Dockerfiles e READMEs
Spring Boot 3.2.4 vs 3.5.6; runtime `jdk` em vez de `jre`; containers como root; nenhum serviço
tem README próprio; corrigir o seed do `vagas-service`.

---

## 📌 Leitura final

O projeto tem **fundação arquitetural sólida** — separação por domínio, camadas bem definidas,
DTOs, exceções de domínio, MapStruct, service discovery, gateway. O modelo de 11 perfis com
matriz de delegação hierárquica no `auth-api` é genuinamente bem pensado e acima do esperado
para o porte do projeto.

O problema não é falta de capacidade técnica: é que **a segurança foi resolvida em um serviço
e nunca propagada aos outros quatro**, e que **o domínio foi duplicado em vez de compartilhado**.
São dois problemas de integração, não de código.

A boa notícia é que a nota agregada é sensível a poucas correções concentradas: os itens 1 a 3
da lista acima são de baixo esforço e movem o backend de **4,9 para cerca de 6,3**. Os itens
4 a 7 levariam a algo próximo de **7,5**.

---

*Relatório gerado pelo Project Analyzer · complementa [ARQUITETURA-E-FLUXOS.md](ARQUITETURA-E-FLUXOS.md)*
