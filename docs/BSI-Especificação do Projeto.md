# Especificação de Projeto — Mora

**Bacharelado em Sistemas de Informação | 2026**

**Autores:**
- João Victor Monteiro Tancon
- Juan Rodrigues dos Santos Servelo
- Luana Akemi Sakurada
- Ray Govaski
- Thais Oliveira Amaral

**Orientadores:** Prof. Geucimar Briatore | Profª. Joselaine Valaski

***

## Sumário

1. [Quadro "3 Objetivos"](#1-quadro-3-objetivos)
2. [Quadro "É – Não É – Faz – Não Faz"](#2-quadro-é--não-é--faz--não-faz)
3. [Visão do Produto](#3-visão-do-produto)
4. [Relação de Atores / Usuários](#4-relação-de-atores--usuários)
5. [Relação de Requisitos Funcionais](#5-relação-de-requisitos-funcionais)
6. [Relação de Histórias de Usuário](#6-relação-de-histórias-de-usuário)

***

## 1. Quadro "3 Objetivos"

**Nome do Produto:** Mora

| Objetivo | Descrição |
|----------|-----------|
| 1 | Centralizar a gestão condominial em uma única plataforma SaaS, reunindo dados de cadastro, operação e comunicação. |
| 2 | Automatizar processos essenciais do condomínio, como autenticação, vínculo de moradores, reservas, financeiro e notificações. |
| 3 | Apoiar a tomada de decisão com dados consolidados e relatórios analíticos por meio de dashboards. |

***

## 2. Quadro "É – Não É – Faz – Não Faz"

**Nome do Produto:** Mora

| É | Não é |
|---|-------|
| Uma plataforma SaaS de gestão condominial. | Um sistema de uso geral para qualquer tipo de empresa. |
| Um produto voltado para administradoras, síndicos e usuários do condomínio. | Um sistema apenas financeiro ou apenas de portaria. |
| | Um produto de uso exclusivo de equipe técnica. |

| Faz | Não faz |
|-----|---------|
| Centraliza cadastro, autenticação e perfis de usuários. | Não substitui a atuação humana na tomada de decisão do condomínio. |
| Gerencia operações condominiais como unidades, ocupantes, reservas, finanças, assembleias e comunicados. | Não garante solução automática de problemas e conflitos. |
| Gera dados para dashboards e relatórios analíticos. | Não oferece recursos de entretenimento ou lazer. |

***

## 3. Visão do Produto

**Nome do Produto:** Mora

| Campo | Descrição |
|-------|-----------|
| **Problemas** | A gestão condominial costuma ser espalhada em controles manuais, retrabalho administrativo, baixa integração entre áreas e dificuldade para acompanhar dados operacionais e financeiros. |
| **Expectativas** | Espera-se concentrar em um só sistema a administração do condomínio, reduzir tarefas repetitivas, melhorar o controle de acessos e vínculos de moradores, organizar informações financeiras e disponibilizar indicadores úteis para decisão. |

| Dimensão | Descrição |
|----------|-----------|
| **Cliente-Alvo** | Administradoras, síndicos e usuários do condomínio. |
| **Categoria-Segmento** | Plataforma SaaS de gestão condominial. |
| **Benefício-Chave** | Centralização e automação da gestão do condomínio. |
| **Diferenciado-Chave** | Comunicação integrada + gestão financeira + controle de reservas em uma única plataforma. |
| **Meta-Valor** | Redução de retrabalho, mais controle operacional e melhor suporte à decisão. |

***

## 4. Relação de Atores / Usuários

| # | Layer | Perfil |
|---|-------|--------|
| 1 | Platform | Super Admin |
| 2 | Tenant | Property Manager |
| 3 | Condominium | Syndic / Administrator / Doorman |
| 4 | Unit | Real Estate Agency / Resident Owner / Absent Owner / Lessee / Occupant (adult) / Guest |

***

## 5. Relação de Requisitos Funcionais

**Produto:** Mora

| # | Requisito Funcional | Ator / Usuário | Sprint |
|---|---------------------|----------------|--------|
| 1 | Gerenciar Planos | Super Admin | 2 |
| 2 | Gerenciar Tenants | Super Admin | 2 |
| 3 | Gerenciar Cadastro de Usuário | Usuário com código de convite válido (Syndic, Administrator, Doorman, Real Estate Agency, Resident Owner, Lessee, Occupant) | 1 |
| 4 | Realizar Autenticação de Usuário | Usuário com conta ativa | 1 |
| 5 | Gerenciar Condomínio | Property Manager | 2 |
| 6 | Gerenciar Estrutura do Condomínio | Property Manager | 1 |
| 7 | Gerenciar Vínculos de Ocupantes por Unidade | Property Manager (owner inicial) + Resident Owner / Absent Owner / Lessee (vínculos na unidade) | 2 |
| 8 | Registrar Entradas e Saídas de Acesso | Doorman | 2 |
| 9 | Gerenciar Assembleias e Atas | Syndic, Administrator | 1 |
| 10 | Gerenciar Base de Conhecimento e FAQ | Property Manager, Syndic, Administrator | 1 |
| 11 | Gerenciar Entregas e Encomendas | Doorman | 1 |
| 12 | Gerenciar Avisos e Comunicados | Syndic | 1 |
| 13 | Controlar Vagas de Estacionamento | Doorman, Administrator | 1 |
| 14 | Gerenciar Contratos de Locação | Property Manager, Real Estate Agency, Resident Owner | 3 |
| 15 | Gerar e Gerenciar Faturas por Unidade | Administrator, Property Manager | 3 |
| 16 | Gerenciar Multas | Syndic, Administrator | 3 |
| 17 | Registrar Prestação de Contas do Condomínio | Syndic, Administrator, Property Manager | 3 |
| 18 | Gerenciar Visitantes e Pré-Autorizações | Doorman, Resident Owner, Lessee | 3 |
| 19 | Gerenciar Áreas Comuns e Reservas | Administrator, Resident Owner, Lessee, Occupant | 3r |
| 20 | Controlar Retirada e Devolução de Chaves | Doorman | 4 |
| 21 | Configurar Tipos e Regras de Taxas | Property Manager, Administrator | 4 |
| 22 | Gerenciar Reclamações e Ocorrências | Resident Owner, Lessee, Occupant, Syndic, Administrator | 4 |
| 23 | Gerenciar Votações | Syndic, Administrator, Resident Owner | 4 |
| 24 | Gerenciar Ordens de Serviço e Manutenção | Syndic, Administrator, Resident Owner, Lessee | 4 |
| 25 | Gerenciar Envio de Mensagens | Todos os perfis com conta ativa | 4 |
| 26 | Gerar Relatórios e Dashboards Analíticos | Super Admin (visão global), Contracting Property Manager (visão do portfólio), Syndic / Administrator (visão do condomínio) | 4 |

***

## 6. Relação de Histórias de Usuário

***

### RF01 — Gerenciar Planos SaaS

#### US-01.1 — Cadastro de plano

**COMO** Super Admin  
**POSSO** cadastrar um novo plano SaaS informando nome, limites, preço e módulos ativos  
**PARA** disponibilizar um pacote comercial para contratação na plataforma

**Contexto:** Os planos são cadastrados e gerenciados na tabela `plan`, servindo como catálogo de ofertas da plataforma. Cada plano define o limite de condomínios, o limite de usuários por condomínio, o preço mensal, os módulos ativos e o status de ativação.

**Critérios de Aceitação**

- **CA-01 — Cadastro com sucesso**  
  DADO QUE o Super Admin informa nome, limites de condomínios e usuários por condomínios, preço e módulos ativos  
  QUANDO confirma o cadastro  
  ENTÃO o sistema registra o plano na tabela `plan`, define `is_active = true` e exibe: *"Plano cadastrado com sucesso."*

- **CA-02 — Nome obrigatório vazio**  
  DADO QUE o Super Admin deixe algum campo em branco  
  QUANDO tenta salvar  
  ENTÃO o sistema destaca o campo e exibe: *"Este campo é obrigatório."*

- **CA-03 — Limites inválidos**  
  DADO QUE o Super Admin informa `max_condominiums` ou `max_users_per_condominium` menor ou igual a zero  
  QUANDO tenta salvar  
  ENTÃO o sistema impede a ação e exibe: *"Informe valores maiores que zero."*

- **CA-04 — Preço negativo**  
  DADO QUE o Super Admin informa um preço mensal menor que zero  
  QUANDO tenta salvar  
  ENTÃO o sistema bloqueia a ação e exibe: *"O preço não pode ser negativo."*

***

#### US-01.2 — Edição de plano

**COMO** Super Admin  
**POSSO** editar um plano existente  
**PARA** ajustar limites, módulos e preço conforme a estratégia da plataforma

**Contexto:** A edição altera somente os dados do catálogo de planos. Alterações em um plano ativo podem impactar tenants já contratados, então o sistema deve validar a mudança antes de salvar.

**Critérios de Aceitação**

- **CA-01 — Edição com sucesso**  
  DADO QUE o Super Admin altera dados válidos de um plano  
  QUANDO salva  
  ENTÃO o sistema persiste a alteração e exibe: *"Alteração salva com sucesso."*

- **CA-02 — Nome duplicado**  
  DADO QUE o Super Admin altera o nome do plano para um nome já existente em outro plano  
  QUANDO tenta salvar  
  ENTÃO o sistema impede a ação e exibe: *"Já existe um plano com este nome."*

- **CA-03 — Limite inconsistente com tenants ativos**  
  DADO QUE o Super Admin reduz o limite de condomínios ou usuários para abaixo do uso atual de um tenant contratado  
  QUANDO tenta salvar  
  ENTÃO o sistema bloqueia a alteração e exibe: *"Existem tenants que não atendem a este novo limite."*

***

#### US-01.3 — Ativação e desativação de plano

**COMO** Super Admin  
**POSSO** ativar ou desativar um plano  
**PARA** controlar quais planos podem ser contratados

**Contexto:** O campo `is_active` define se o plano pode ou não ser ofertado para novos tenants. Um plano desativado não deve desaparecer do histórico, apenas ficar indisponível para novas contratações.

**Critérios de Aceitação**

- **CA-01 — Desativação com sucesso**  
  DADO QUE o Super Admin desativa um plano ativo  
  QUANDO confirma a ação  
  ENTÃO o sistema altera `is_active` para `false` e exibe: *"Plano desativado com sucesso."*

- **CA-02 — Plano já desativado**  
  DADO QUE o plano já está inativo  
  QUANDO o Super Admin tentar desativá-lo novamente  
  ENTÃO o sistema exibe: *"Este plano já está desativado."*

- **CA-03 — Reativação com sucesso**  
  DADO QUE o Super Admin reativa um plano desativado  
  QUANDO confirma a ação  
  ENTÃO o sistema altera `is_active` para `true` e exibe: *"Plano ativado com sucesso."*

***

#### US-01.4 — Configuração de módulos do plano

**COMO** Super Admin  
**POSSO** definir quais módulos ficam ativos em um plano  
**PARA** controlar os recursos disponíveis para os tenants contratantes

**Contexto:** A coluna `active_modules` armazena um array JSON com os slugs dos módulos liberados naquele plano. Isso permite habilitar funcionalidade por pacote sem criar muitas colunas booleanas.

**Critérios de Aceitação**

- **CA-01 — Configuração com sucesso**  
  DADO QUE o Super Admin seleciona módulos válidos para o plano  
  QUANDO salva  
  ENTÃO o sistema grava os slugs em `active_modules` e exibe: *"Módulos atualizados com sucesso."*

- **CA-02 — Lista de módulos vazia**  
  DADO QUE o Super Admin tenta salvar sem selecionar nenhum módulo  
  QUANDO confirma  
  ENTÃO o sistema bloqueia e exibe: *"Selecione um módulo."*

- **CA-03 — Módulo inválido**  
  DADO QUE o Super Admin informa um slug de módulo inexistente  
  QUANDO tenta salvar  
  ENTÃO o sistema impede a ação e exibe: *"Módulo inválido."*

***

#### US-01.5 — Consulta de planos

**COMO** Super Admin  
**POSSO** visualizar a lista de planos cadastrados  
**PARA** analisar as opções comerciais disponíveis na plataforma

**Contexto:** A consulta deve mostrar os principais dados do plano para apoiar a gestão do catálogo comercial.

**Critérios de Aceitação**

- **CA-01 — Consulta com sucesso**  
  DADO QUE o Super Admin acessa a listagem de planos  
  QUANDO a tela carrega  
  ENTÃO o sistema exibe nome, limites, preço, módulos ativos e status.

- **CA-02 — Lista vazia**  
  DADO QUE não existem planos cadastrados  
  QUANDO o Super Admin acessa a tela  
  ENTÃO o sistema exibe: *"Nenhum plano cadastrado."*

***

### RF02 — Gerenciar Tenant

#### US-02.1 — Cadastro de novo tenant

**COMO** Super Admin  
**POSSO** cadastrar um novo tenant informando nome, tipo, CNPJ, plano e identificador técnico do schema  
**PARA** provisionar um novo cliente na plataforma Mora

**Contexto:** O tenant representa o cliente contratado da plataforma e é armazenado na tabela `tenant`. O tipo do tenant define sua categoria de operação, enquanto o plano define limites, módulos e preço contratado.

**Critérios de Aceitação**

- **CA-01 — Cadastro com sucesso**  
  DADO QUE o Super Admin informa nome, tipo, plano válido e CNPJ único  
  QUANDO confirma o cadastro  
  ENTÃO o sistema registra o tenant na tabela `tenant`, atribui status `active` e disponibiliza o tenant para uso.

- **CA-02 — Campo vazio**  
  DADO QUE o Super Admin deixe algum campo em branco  
  QUANDO tenta salvar  
  ENTÃO o sistema destaca o campo e exibe: *"Este campo é obrigatório."*

- **CA-03 — Schema_name duplicado**  
  DADO QUE o Super Admin informa um CNPJ já cadastrado em outro tenant  
  QUANDO tenta salvar  
  ENTÃO o sistema impede a ação e exibe: *"Já existe um tenant com este CNPJ."*

- **CA-04 — Plano não selecionado**  
  DADO QUE o Super Admin tenta salvar sem selecionar um plano  
  QUANDO confirma o cadastro  
  ENTÃO o sistema exibe: *"Selecione um plano para continuar."*

***

#### US-02.2 — Edição de tenant

**COMO** Super Admin  
**POSSO** editar dados cadastrais de um tenant, como nome, CNPJ, plano e status  
**PARA** manter a configuração da conta atualizada

**Contexto:** A edição atualiza apenas os dados cadastrais do tenant na tabela `tenant`. A troca de plano deve respeitar os limites e a compatibilidade com a estrutura já cadastrada.

**Critérios de Aceitação**

- **CA-01 — Edição com sucesso**  
  DADO QUE o Super Admin altera um dado válido e consistente com as regras da plataforma  
  QUANDO salva  
  ENTÃO o sistema persiste a alteração e exibe: *"Alteração salva com sucesso."*

- **CA-02 — CNPJ duplicado**  
  DADO QUE o Super Admin altera o CNPJ para um valor já cadastrado em outro tenant  
  QUANDO tenta salvar  
  ENTÃO o sistema impede a ação e exibe: *"Já existe um tenant com este CNPJ."*

***

#### US-02.3 — Provisionamento inicial do tenant

**COMO** Super Admin  
**POSSO** provisionar um tenant recém-cadastrado  
**PARA** disponibilizar sua estrutura inicial de autenticação e operação no ecossistema do Mora

**Contexto:** O tenant é cadastrado no Banco AUTH, onde ficam armazenados seus dados cadastrais, plano e status. Após a confirmação do provisionamento, o sistema publica um evento de domínio (`tenant.provisioned`) para que os serviços responsáveis preparem a estrutura inicial necessária no Banco MORA, mantendo a comunicação entre os dois bancos por mensageria.

**Critérios de Aceitação**

- **CA-01 — Provisionamento com sucesso**  
  DADO QUE o Super Admin confirma o provisionamento de um tenant ativo  
  QUANDO o processo é executado com sucesso  
  ENTÃO o sistema mantém o tenant ativo no Banco AUTH, publica o evento `tenant.provisioned` e disponibiliza o tenant para inicialização das operações no Banco MORA.

- **CA-02 — Tenant já provisionado**  
  DADO QUE o tenant já teve seu provisionamento inicial concluído  
  QUANDO o Super Admin tenta provisioná-lo novamente  
  ENTÃO o sistema impede a ação e exibe: *"Este tenant já foi provisionado."*

- **CA-03 — Falha na criação do schema**  
  DADO QUE ocorre falha no fluxo de mensageria durante o provisionamento  
  QUANDO o processo é interrompido  
  ENTÃO o sistema registra a falha, mantém o tenant sem conclusão do provisionamento operacional e exibe: *"Não foi possível concluir o provisionamento do tenant."*

***

#### US-02.4 — Suspensão de tenant

**COMO** Super Admin  
**POSSO** suspender o acesso de um tenant  
**PARA** bloquear temporariamente sua operação sem apagar seus dados

**Contexto:** A suspensão altera o status do tenant para `suspended`, sem excluir o registro da tabela `tenant`. Isso impede o uso da conta enquanto a suspensão estiver ativa.

**Critérios de Aceitação**

- **CA-01 — Suspensão com sucesso**  
  DADO QUE o Super Admin suspende um tenant ativo  
  QUANDO confirma a ação  
  ENTÃO o sistema altera o status do tenant para `suspended` e exibe: *"Tenant suspenso com sucesso."*

- **CA-02 — Tenant inexistente**  
  DADO QUE o Super Admin tenta suspender um tenant inexistente  
  QUANDO confirma a ação  
  ENTÃO o sistema impede a ação e exibe: *"Tenant não encontrado."*

- **CA-03 — Tenant já suspenso**  
  DADO QUE o tenant já está suspenso  
  QUANDO o Super Admin tenta suspender novamente  
  ENTÃO o sistema exibe: *"Este tenant já está suspenso."*

***

#### US-02.5 — Gerenciamento de plano do tenant

**COMO** Super Admin  
**POSSO** atribuir ou trocar o plano de um tenant  
**PARA** controlar limites, módulos habilitados e preço contratado

**Contexto:** O plano define número máximo de condomínios, usuários por condomínio, preço mensal e módulos ativos. A troca de plano deve validar a compatibilidade com o tipo do tenant e com a estrutura já cadastrada.

**Critérios de Aceitação**

- **CA-01 — Troca de plano com sucesso**  
  DADO QUE o tenant atende às restrições do novo plano  
  QUANDO o Super Admin confirma a alteração  
  ENTÃO o sistema atualiza o `plan_id` do tenant e exibe: *"Plano alterado com sucesso."*

- **CA-02 — Excedente de condomínios**  
  DADO QUE o tenant possui mais condomínios do que o limite do novo plano  
  QUANDO tenta salvar  
  ENTÃO o sistema bloqueia a alteração e exibe: *"O plano selecionado não comporta a quantidade atual de condomínios."*

- **CA-03 — Plano inativo**  
  DADO QUE o Super Admin seleciona um plano desativado  
  QUANDO tenta salvar  
  ENTÃO o sistema impede a ação e exibe: *"Selecione um plano ativo."*

***

#### US-02.6 — Consulta de configuração do tenant

**COMO** Super Admin  
**POSSO** visualizar a configuração completa de um tenant  
**PARA** auditar dados de cadastro, plano, status e provisionamento

**Contexto:** A consulta consolida informações do tenant, do plan e do estado de provisionamento do tenant, permitindo ver a configuração corrente da conta.

**Critérios de Aceitação**

- **CA-01 — Consulta com sucesso**  
  DADO QUE o Super Admin acessa a tela de detalhes de um tenant  
  QUANDO a consulta é carregada  
  ENTÃO o sistema exibe nome, tipo, CNPJ, plano, status e data de criação.

- **CA-02 — Tenant não encontrado**  
  DADO QUE o Super Admin tenta abrir um tenant inexistente  
  QUANDO acessa a tela  
  ENTÃO o sistema exibe: *"Tenant não encontrado."*

***

### RF03 — Gerenciar Dados de Usuário

#### US-03.1 — Ativação de conta via código de convite

**COMO** usuário que recebeu um código de convite (Syndic, Administrator, Doorman, Real Estate Agency, Resident Owner, Lessee, Occupant ou Guest)  
**POSSO** acessar a página de ativação informando esse código  
**PARA** criar minhas credenciais e ter acesso ao sistema com o perfil correto já atribuído

**Contexto:** O usuário recebe o código por e-mail. O remetente varia conforme o perfil: Syndic, Administrator, Doorman, Real Estate Agency e Resident Owner são cadastrados pelo Contracting Property Manager. Lessee é cadastrado pelo Resident Owner ou Absent Owner. Occupant e Guest são cadastrados pelo Resident Owner ou Lessee. O código é de uso único, tem prazo de validade de 48 horas e o perfil já vem definido nele — o usuário não escolhe.

**Critérios de Aceitação**

- **CA-01 — Acesso à página de ativação com código válido**  
  DADO QUE o usuário acessa a URL de ativação e insere um código válido e dentro do prazo  
  QUANDO clica em "Continuar"  
  ENTÃO o sistema exibe o formulário de criação de conta com os campos: nome completo, e-mail, telefone, CPF, senha (mínimo 8 caracteres, ao menos 1 número e 1 letra maiúscula) e confirmação de senha idêntica. O campo de perfil não aparece.

- **CA-02 — Código inválido**  
  DADO QUE o usuário insere um código que não existe no sistema  
  QUANDO clica em "Continuar"  
  ENTÃO o sistema exibe: *"Código inválido. Verifique o código recebido ou solicite um novo ao Administrator do condomínio."* O formulário de cadastro não é exibido.

- **CA-03 — Código expirado**  
  DADO QUE o usuário insere um código que existia mas ultrapassou o prazo de validade  
  QUANDO clica em "Continuar"  
  ENTÃO o sistema exibe: *"Este código expirou. Solicite um novo convite ao Administrator."* O formulário de cadastro não é exibido.

- **CA-04 — Código já utilizado**  
  DADO QUE o usuário insere um código que já foi ativado por outro usuário  
  QUANDO clica em "Continuar"  
  ENTÃO o sistema exibe: *"Este código já foi utilizado."*

***

#### US-03.2 — Preenchimento e salvamento dos dados no primeiro acesso

**COMO** usuário que validou o código de convite  
**POSSO** preencher meus dados pessoais (nome, e-mail, senha) e salvar  
**PARA** concluir minha conta e acessar o sistema imediatamente após a ativação

**Contexto:** Este fluxo ocorre na mesma sessão da validação do código. Após salvar, o usuário é redirecionado diretamente para a página inicial do seu perfil, sem precisar fazer login novamente.

**Critérios de Aceitação**

- **CA-01 — Cadastro bem-sucedido**  
  DADO QUE o usuário preenche nome completo, e-mail válido, senha que atende os requisitos (mínimo 8 caracteres, ao menos 1 número e 1 letra maiúscula) e confirmação de senha idêntica  
  QUANDO clica em "Criar conta"  
  ENTÃO o sistema registra o usuário, invalida o código de convite, inicia a sessão automaticamente e redireciona para a página inicial correspondente ao perfil vinculado ao código.

- **CA-02 — E-mail já cadastrado no condomínio**  
  DADO QUE o usuário informa um e-mail que já pertence a outro usuário ativo no mesmo condomínio  
  QUANDO tenta salvar  
  ENTÃO o sistema bloqueia e exibe: *"Este e-mail já está em uso."*

- **CA-03 — Senhas não coincidem**  
  DADO QUE o usuário preenche "senha" e "confirmação de senha" com valores diferentes  
  QUANDO tenta salvar  
  ENTÃO o sistema destaca os dois campos em vermelho e exibe: *"As senhas não coincidem."*

- **CA-04 — Senha fora dos requisitos mínimos**  
  DADO QUE o usuário insere uma senha com menos de 8 caracteres, sem números ou sem letras maiúsculas  
  QUANDO tenta salvar  
  ENTÃO o sistema exibe abaixo do campo de senha quais requisitos não foram atendidos. Ex: *"A senha precisa ter ao menos 8 caracteres"* / *"Inclua ao menos 1 número"* / *"Inclua ao menos 1 letra maiúscula."*

- **CA-05 — Campo vazio**  
  DADO QUE o usuário deixa algum dado em branco  
  QUANDO tenta salvar  
  ENTÃO o sistema destaca os campos vazios e exibe: *"Este campo é obrigatório."* O salvamento não ocorre.

***

#### US-03.3 — Edição de dados pessoais (após conta ativa)

**COMO** usuário autenticado (qualquer perfil)  
**POSSO** editar meu nome, telefone, e-mail e foto de perfil  
**PARA** manter meus dados atualizados

**Critérios de Aceitação**

- **CA-01 — Atualização de dados com sucesso**  
  DADO QUE o usuário acessa "Meu Perfil" e altera nome, telefone ou e-mail com dados válidos  
  QUANDO clica em "Salvar"  
  ENTÃO o sistema persiste as alterações e exibe: *"Dados atualizados com sucesso."*

- **CA-02 — E-mail duplicado**  
  DADO QUE o usuário tenta alterar o e-mail para um endereço já vinculado a outro usuário do sistema  
  QUANDO tenta salvar  
  ENTÃO o sistema impede e exibe: *"Este e-mail já está em uso por outro perfil."*

- **CA-03 — Upload de foto de perfil: sucesso**  
  DADO QUE o usuário seleciona um arquivo JPG ou PNG com tamanho abaixo de 5 MB  
  QUANDO confirma o upload  
  ENTÃO o sistema substitui a foto anterior, redimensiona a imagem para o padrão do sistema e exibe a nova foto em todos os pontos de exibição do perfil.

- **CA-04 — Upload de foto de perfil: formato inválido**  
  DADO QUE o usuário tenta enviar um arquivo que não seja JPG ou PNG (ex: PDF, GIF, HEIC)  
  QUANDO tenta salvar  
  ENTÃO o sistema bloqueia o envio e exibe: *"Formato não permitido. Envie uma imagem JPG ou PNG."*

- **CA-05 — Campo obrigatório apagado**  
  DADO QUE o usuário apaga o nome ou e-mail (campos obrigatórios) e tenta salvar  
  QUANDO clica em "Salvar"  
  ENTÃO o sistema destaca os campos em vermelho e exibe: *"Este campo é obrigatório."* A operação não é concluída.

***

#### US-03.4 — Cadastro de usuário de nível condomínio pelo Contracting Property Manager

**COMO** Contracting Property Manager  
**POSSO** cadastrar Syndic, Administrator, Doorman, Real Estate Agency ou Resident Owner, associando-os ao condomínio correspondente e enviando o código de convite  
**PARA** dar acesso ao sistema às pessoas que operam o condomínio ou são proprietários de unidades

**Contexto:** O Contracting Property Manager é o único que pode cadastrar perfis de nível condomínio e o perfil Resident Owner. O sistema gera um código de convite de uso único com validade de 48 horas e envia por e-mail. O Resident Owner é obrigatoriamente vinculado a uma unidade no momento do cadastro.

**Critérios de Aceitação**

- **CA-01 — Cadastro e envio de convite**  
  DADO QUE o ator preenche e-mail, seleciona o perfil (Syndic, Administrator, Doorman, Real Estate Agency ou Resident Owner) e, se o perfil for Resident Owner, associa obrigatoriamente a uma unidade disponível  
  QUANDO confirma o cadastro  
  ENTÃO o sistema gera um código de convite de uso único com validade de 48 horas, envia para o e-mail informado e exibe a confirmação.

- **CA-02 — E-mail já cadastrado**  
  DADO QUE o ator tenta cadastrar um e-mail que já pertence a um usuário ativo no condomínio  
  QUANDO tenta confirmar  
  ENTÃO o sistema bloqueia e exibe: *"Já existe um usuário cadastrado com este e-mail."*

- **CA-03 — Unidade já ocupada (para Resident Owner)**  
  DADO QUE o ator tenta vincular um Resident Owner a uma unidade que já possui um Resident Owner ativo  
  QUANDO tenta confirmar  
  ENTÃO o sistema bloqueia e exibe: *"Esta unidade já possui um Resident Owner ativo. Desative o atual antes de cadastrar um novo."*

- **CA-04 — Reenvio de convite expirado**  
  DADO QUE o convite enviado anteriormente expirou e o usuário ainda não ativou a conta  
  QUANDO o ator acessa o cadastro pendente e clica em "Reenviar convite"  
  ENTÃO o sistema invalida o código anterior, gera um novo código com validade de 48 horas e envia para o e-mail cadastrado.

***

#### US-03.5 — Cadastro de usuário de nível unidade pelo Resident Owner, Absent Owner ou Lessee

**COMO** Resident Owner, Absent Owner ou Lessee  
**POSSO** cadastrar usuários vinculados à minha unidade conforme as permissões do meu perfil  
**PARA** registrar quem convive na unidade e conceder acesso ao sistema quando aplicável

**Contexto:** As permissões de cadastro por perfil são:
- **Resident Owner** — pode cadastrar Lessee, Occupant e Guest na sua unidade
- **Absent Owner** — pode cadastrar apenas 1 Lessee na sua unidade
- **Lessee** — pode cadastrar Occupant e Guest na unidade

O ator informa nome completo, CPF e e-mail da pessoa. O sistema envia o e-mail de ativação e, ao ativar, a pessoa deve confirmar os dados informados — se não baterem, a autenticação não é concluída. Menores de 16 anos só podem ser cadastrados como Guest sem acesso.

**Critérios de Aceitação**

- **CA-01 — Cadastro com sucesso**  
  DADO QUE o ator preenche nome completo, CPF e e-mail do novo usuário e o perfil é compatível com suas permissões  
  QUANDO confirma o cadastro  
  ENTÃO o sistema gera um código de convite de uso único com validade de 48 horas e envia para o e-mail informado.

- **CA-02 — Absent Owner tentando cadastrar mais de 1 Lessee**  
  DADO QUE um Absent Owner já possui um Lessee ativo vinculado à sua unidade e tenta cadastrar outro  
  QUANDO tenta confirmar  
  ENTÃO o sistema bloqueia e exibe: *"Você já possui um Lessee vinculado a esta unidade."*

- **CA-03 — Perfil incompatível com o ator**  
  DADO QUE o ator tenta cadastrar um perfil que não está dentro de suas permissões (ex: Lessee tentando cadastrar outro Lessee)  
  QUANDO tenta confirmar  
  ENTÃO o sistema bloqueia e exibe: *"Você não tem permissão para cadastrar este perfil."*

- **CA-04 — E-mail já cadastrado na unidade**  
  DADO QUE o ator tenta cadastrar um e-mail que já pertence a um usuário ativo na mesma unidade  
  QUANDO tenta confirmar  
  ENTÃO o sistema bloqueia e exibe: *"Já existe um usuário cadastrado com este e-mail nesta unidade."*

- **CA-05 — Reenvio de convite expirado**  
  DADO QUE o convite enviado anteriormente expirou e o usuário ainda não ativou a conta  
  QUANDO o ator acessa o cadastro pendente e clica em "Reenviar convite"  
  ENTÃO o sistema invalida o código anterior, gera um novo código com validade de 48 horas e envia para o e-mail cadastrado.

***

#### US-03.6 — Desativação de usuário

**COMO** Contracting Property Manager (para perfis de nível condomínio e Resident Owner) ou Resident Owner / Lessee (para perfis de nível unidade sob sua responsabilidade)  
**POSSO** desativar um usuário que não faz mais parte do condomínio ou da unidade  
**PARA** revogar o acesso sem apagar o histórico de ações desse usuário no sistema

**Contexto:** A desativação de Resident Owner e Absent Owner é feita pelo Contracting Property Manager. A desativação de um Lessee é feita pelo Resident Owner ou Absent Owner que o cadastrou. A desativação de Occupant ou Guest é feita pelo Lessee ou Resident Owner que o cadastrou. Regras de cascata: se um Lessee for desativado, todos os Occupants e Guests vinculados a ele também são desativados. Se um Resident Owner que transferiu responsabilidade financeira for desativado, todos vinculados a ele também são desativados.

**Critérios de Aceitação**

- **CA-01 — Desativação com sucesso**  
  DADO QUE o ator acessa o cadastro de um usuário ativo que está dentro do seu escopo de gestão e confirma a desativação  
  QUANDO confirma a ação  
  ENTÃO o sistema revoga o acesso imediatamente (tokens de sessão existentes são invalidados), mantém o histórico de ações para auditoria e o usuário aparece na listagem como "Inativo".

- **CA-02 — Cascata por desativação de Lessee**  
  DADO QUE o ator desativa um Lessee que possui Occupants ou Guests ativos vinculados a ele  
  QUANDO confirma a ação  
  ENTÃO o sistema desativa o Lessee e desativa automaticamente todos os Occupants e Guests vinculados a esse Lessee, exibindo: *"Lessee desativado. X usuários vinculados também foram desativados."*

- **CA-03 — Tentativa de desativar responsável financeiro com pendências**  
  DADO QUE o ator tenta desativar o responsável financeiro de uma unidade que possui cobranças em aberto  
  QUANDO confirma a ação  
  ENTÃO o sistema exibe um alerta: *"Este usuário é o responsável financeiro da unidade [X] e possui cobranças em aberto. Regularize as pendências antes de prosseguir."* A desativação não ocorre.

- **CA-04 — Ator sem permissão**  
  DADO QUE o ator tenta desativar um usuário que não está dentro do seu escopo de gestão  
  QUANDO confirma a ação  
  ENTÃO o sistema bloqueia e exibe: *"Você não tem permissão para desativar este usuário."*

***

### RF04 — Realizar Autenticação dos Usuários

#### US-04.1 — Login padrão

**COMO** usuário com conta ativa (qualquer perfil)  
**POSSO** fazer login com e-mail e senha  
**PARA** acessar as funcionalidades do meu perfil

**Critérios de Aceitação**

- **CA-01 — Login bem-sucedido**  
  DADO QUE o usuário informa e-mail e senha corretos na tela de login  
  QUANDO clica em "Entrar"  
  ENTÃO o sistema valida as credenciais, inicia uma sessão autenticada e redireciona para a página inicial correspondente ao perfil do usuário (ex: Porteiro vai para o painel de portaria; Morador vai para o painel do morador).

- **CA-02 — Credenciais inválidas**  
  DADO QUE o usuário insere e-mail não cadastrado ou senha incorreta  
  QUANDO clica em "Entrar"  
  ENTÃO o sistema nega o acesso, não inicia sessão e exibe: *"E-mail ou senha inválidos."* O sistema não informa qual dos dois está errado (segurança por obscuridade).

- **CA-03 — Conta inativa**  
  DADO QUE o usuário tenta fazer login com um e-mail de conta desativada pelo Property Manager  
  QUANDO clica em "Entrar"  
  ENTÃO o sistema exibe: *"Esta conta foi desativada."*

- **CA-04 — Conta com convite pendente (nunca ativada)**  
  DADO QUE o usuário tenta fazer login antes de concluir o primeiro acesso via código de convite  
  QUANDO clica em "Entrar"  
  ENTÃO o sistema exibe: *"Seu cadastro ainda não foi concluído. Acesse o link enviado para seu e-mail para criar sua senha."*

***

### RF05 — Gerenciar Condomínio

#### US-05.1 — Cadastro de condomínio

**COMO** Property Manager  
**POSSO** cadastrar um condomínio informando nome, CNPJ, endereço e dados de localização  
**PARA** iniciar a estrutura operacional do cliente no sistema

**Contexto:** O condomínio é a entidade principal do tenant no Banco MORA e é criado ao receber o evento `tenant.provisioned` vindo do Banco AUTH. Após o cadastro, o condomínio servirá de base para prédios, unidades e demais operações.

**Critérios de Aceitação**

- **CA-01 — Cadastro com sucesso**  
  DADO QUE o ator preenche nome, endereço, cidade, estado e CEP válidos  
  QUANDO confirma o cadastro  
  ENTÃO o sistema registra o condomínio na tabela `condominium`, atribui `is_active = true` e disponibiliza o registro para receber prédios.

- **CA-02 — Campo obrigatório vazio**  
  DADO QUE o ator deixa nome, endereço, cidade, estado ou CEP em branco  
  QUANDO tenta salvar  
  ENTÃO o sistema destaca o campo e exibe: *"Este campo é obrigatório."*

- **CA-03 — Estado inválido**  
  DADO QUE o ator informa um estado diferente de duas letras  
  QUANDO tenta salvar  
  ENTÃO o sistema impede a ação e exibe: *"Informe um estado válido."*

- **CA-04 — CEP inválido**  
  DADO QUE o ator informa um CEP fora do formato esperado  
  QUANDO tenta salvar  
  ENTÃO o sistema bloqueia a ação e exibe: *"Informe um CEP válido."*

***

#### US-05.2 — Edição de condomínio

**COMO** Property Manager  
**POSSO** editar os dados cadastrais de um condomínio  
**PARA** manter as informações do empreendimento atualizadas

**Contexto:** A edição permite corrigir dados cadastrais já registrados sem alterar a identidade estrutural do condomínio. O sistema deve preservar o histórico de alterações para rastreabilidade, se essa regra existir no serviço.

**Critérios de Aceitação**

- **CA-01 — Edição com sucesso**  
  DADO QUE o ator altera nome, endereço, cidade, estado ou CEP com dados válidos  
  QUANDO salva  
  ENTÃO o sistema persiste a alteração e exibe: *"Alteração salva com sucesso."*

- **CA-02 — Campo obrigatório vazio**  
  DADO QUE o ator apaga um campo obrigatório  
  QUANDO tenta salvar  
  ENTÃO o sistema destaca o campo e exibe: *"Este campo é obrigatório."*

- **CA-03 — Dados inválidos**  
  DADO QUE o ator informa estado ou CEP em formato inválido  
  QUANDO tenta salvar  
  ENTÃO o sistema impede a ação e exibe a mensagem de validação correspondente.

***

#### US-05.3 — Ativação ou suspensão de condomínio

**COMO** Property Manager  
**POSSO** ativar ou suspender um condomínio  
**PARA** controlar se ele pode continuar operando no sistema

**Contexto:** O campo `is_active` indica se o condomínio está disponível para uso. A suspensão não apaga o registro, apenas bloqueia sua operação enquanto necessário.

**Critérios de Aceitação**

- **CA-01 — Suspensão com sucesso**  
  DADO QUE o ator suspende um condomínio ativo  
  QUANDO confirma a ação  
  ENTÃO o sistema altera `is_active` para `false` e impede novas operações naquele condomínio.

- **CA-02 — Ativação com sucesso**  
  DADO QUE o ator reativa um condomínio suspenso  
  QUANDO confirma a ação  
  ENTÃO o sistema altera `is_active` para `true` e libera o condomínio para uso normal.

- **CA-03 — Condomínio inexistente**  
  DADO QUE o ator tenta alterar um condomínio que não existe  
  QUANDO confirma a ação  
  ENTÃO o sistema exibe: *"Condomínio não encontrado."*

***

### RF06 — Gerenciar Estruturas do Condomínio

#### US-06.1 — Cadastro de building

**COMO** Property Manager  
**POSSO** cadastrar um novo building (prédio) dentro da página do condomínio, informando nome/identificação, andar inicial e andar final  
**PARA** criar a estrutura física daquele condomínio e disponibilizá-la para receber unidades

**Contexto:** Um condomínio pode ter múltiplos buildings. Ao cadastrar um building dentro da página do condomínio, o sistema vincula automaticamente esse building ao condomínio atualmente selecionado. O range de andares serve para validar o andar das unidades vinculadas a esse prédio.

**Critérios de Aceitação**

- **CA-01 — Cadastro com sucesso**  
  DADO QUE o ator está na página de um condomínio e preenche o nome/identificação do prédio e informa um range de andares válido  
  QUANDO confirma o cadastro  
  ENTÃO o sistema registra o building na tabela `building` já vinculado automaticamente ao condominium atual, atribui status "active" e o disponibiliza para receber unidades.

- **CA-02 — Duplicidade de nome**  
  DADO QUE o ator informa um nome já existente para outro prédio no mesmo condomínio  
  QUANDO tenta salvar  
  ENTÃO o sistema impede a ação e exibe: *"Já existe um prédio com este nome neste condomínio."*

- **CA-03 — Campo obrigatório vazio**  
  DADO QUE o ator deixa o campo de nome em branco  
  QUANDO tenta salvar  
  ENTÃO o sistema destaca o campo e exibe: *"Este campo é obrigatório."*

- **CA-04 — Range de andares inválido**  
  DADO QUE o ator informa um andar inicial maior que o andar final  
  QUANDO tenta salvar  
  ENTÃO o sistema impede a ação e exibe: *"Informe um range de andares válido."*

***

#### US-06.2 — Cadastro de Unidade Habitacional

**COMO** Property Manager  
**POSSO** cadastrar uma nova unit (apartamento) dentro da página de um building, informando numeração e andar  
**PARA** criar o registro da unidade na tabela `unit` já vinculado automaticamente ao building selecionado e habilitá-lo para receber ocupantes e veículos

**Contexto:** A unidade é sempre vinculada a um building existente por meio do campo `building_id`. Os dados principais da unidade são `number`, `floor` e `fraction_ideal`, sendo a fração ideal obrigatória e maior que zero. O vínculo com o building ocorre automaticamente a partir da página do building em que o cadastro é realizado, sem uso de schema específico por tenant.

**Critérios de Aceitação**

- **CA-01 — Cadastro com sucesso**  
  DADO QUE o ator acessa a página de um building ativo e informa a numeração e o andar  
  QUANDO salva  
  ENTÃO o sistema cria o registro vinculado automaticamente ao building atual e o libera para associação.

- **CA-02 — Duplicidade de unidade no bloco**  
  DADO QUE o ator informa uma numeração já existente no mesmo building  
  QUANDO tenta salvar  
  ENTÃO o sistema impede a ação e exibe: *"Já existe uma unidade com esta numeração neste prédio."*

- **CA-04 — Andar fora do range do building**  
  DADO QUE o ator informa um andar que não pertence ao range cadastrado para o building  
  QUANDO tenta salvar  
  ENTÃO o sistema impede a ação e exibe: *"O andar informado não pertence ao building selecionado."*

***

#### US-06.3 — Edição de Bloco ou Unidade

**COMO** Property Manager  
**POSSO** editar o nome de um bloco ou a numeração/andar de uma unidade  
**PARA** corrigir dados cadastrados incorretamente

**Contexto:** A edição respeita as mesmas regras de unicidade do cadastro.

**Critérios de Aceitação**

- **CA-01 — Edição com sucesso**  
  DADO QUE o ator altera o nome do building ou a numeração da unidade para um valor válido e não duplicado  
  QUANDO salva  
  ENTÃO o sistema persiste a alteração e exibe: *"Alteração salva com sucesso."*

- **CA-02 — Nome duplicado na edição**  
  DADO QUE o ator altera o nome do building para um nome já existente no mesmo condomínio  
  QUANDO salva  
  ENTÃO o sistema impede a ação e exibe: *"Já existe um bloco com este nome neste condomínio."*

- **CA-03 — Numeração duplicada na edição**  
  DADO QUE o ator altera a numeração da unidade para uma numeração já existente no mesmo bloco  
  QUANDO salva  
  ENTÃO o sistema impede a ação e exibe: *"Já existe uma unidade com esta numeração neste bloco."*

- **CA-04 — Campo obrigatório vazio na edição**  
  DADO QUE o ator apaga o nome do bloco ou a numeração da unidade  
  QUANDO tenta salvar  
  ENTÃO o sistema destaca o campo e exibe: *"Este campo é obrigatório."*