# Histórias de Usuário — Mora

Uma história por requisito funcional, no formato **Como / Quero / Para**, com critérios de
aceite em **Dado / Quando / Então**.

Complementa a [Especificação do Projeto](ESPECIFICACAO-PROJETO.md). No documento original esta
seção estava vazia — sem ela, nenhum requisito tinha definição de pronto.

Legenda de status: ✅ Implementado · ⚠️ Parcial · 📋 Planejado

---

## RF-01 — Gerenciar Planos ⚠️

> **Como** Super Admin
> **Quero** cadastrar e manter os planos comerciais da plataforma
> **Para** definir os limites e os módulos disponíveis para cada faixa de contratação

**Critérios de aceite**

1. **Dado** que sou Super Admin, **quando** cadastro um plano com nome, preço mensal, limite de condomínios, limite de usuários por condomínio e módulos ativos, **então** o plano é criado e fica disponível para contratação.
2. **Dado** que existe um plano com o nome informado, **quando** tento cadastrar outro com o mesmo nome, **então** recebo erro de nome duplicado.
3. **Dado** que um plano possui assinaturas ativas, **quando** tento desativá-lo, **então** o sistema impede e informa quantos tenants o utilizam.
4. **Dado** que altero o preço ou os limites de um plano, **quando** salvo, **então** a alteração é registrada em trilha de auditoria com autor e data.
5. **Dado** que não sou Super Admin, **quando** acesso a gestão de planos, **então** recebo 403.

---

## RF-02 — Gerenciar Tenants (clientes) 📋

> **Como** Super Admin
> **Quero** cadastrar um cliente já contratado e emitir o acesso do gestor
> **Para** colocar o cliente em operação logo após o fechamento comercial

**Critérios de aceite**

1. **Dado** que sou Super Admin, **quando** cadastro um cliente informando nome, CNPJ, tipo (`ADMINISTRADORA` ou `CONDOMINIO_DIRETO`), plano e e-mail do gestor, **então** o sistema cria o tenant, a assinatura e o convite do gestor em uma única transação.
2. **Dado** que o tipo escolhido é `CONDOMINIO_DIRETO`, **quando** o tenant é criado, **então** o limite de condomínios é fixado em 1, independentemente do plano contratado.
3. **Dado** que o CNPJ informado já existe, **quando** confirmo o cadastro, **então** recebo erro e nenhum registro é criado.
4. **Dado** que o cadastro foi concluído, **quando** o gestor recebe o e-mail, **então** o convite é válido por 48 horas e concede o perfil `CONTRACTING_PROPERTY_MANAGER` ou `CONTRACTING_SYNDIC` conforme o tipo do tenant.
5. **Dado** que um tenant está inadimplente além da tolerância, **quando** o Super Admin o suspende, **então** o status vai para `SUSPENDED_READONLY` e os usuários passam a ter apenas leitura, com aviso em tela.
6. **Dado** que um tenant suspenso regulariza a situação, **quando** o Super Admin reativa, **então** o status volta para `ACTIVE` e a escrita é restabelecida.
7. **Dado** que um tenant é cancelado, **quando** o prazo de retenção expira, **então** os dados são expurgados e o evento fica registrado.

---

## RF-03 — Gerenciar Cadastro de Usuário ✅

> **Como** usuário convidado
> **Quero** ativar minha conta a partir do código recebido
> **Para** acessar o sistema com o perfil que me foi atribuído

**Critérios de aceite**

1. **Dado** que recebi um código de convite válido, **quando** informo o código e defino uma senha, **então** minha conta é ativada com o perfil, condomínio e unidade definidos no convite.
2. **Dado** que o código expirou (mais de 48 horas), **quando** tento ativá-lo, **então** recebo mensagem orientando solicitar novo convite ao administrador.
3. **Dado** que o código já foi utilizado ou revogado, **quando** tento ativá-lo, **então** recebo mensagem específica para cada situação.
4. **Dado** que meu perfil exige unidade, **quando** o convite é emitido sem `unidadeId`, **então** a emissão é recusada.
5. **Dado** que o limite de usuários do plano foi atingido, **quando** um novo convite é emitido, **então** o sistema recusa e informa o limite.

---

## RF-04 — Realizar Autenticação de Usuário ✅

> **Como** usuário com conta ativa
> **Quero** entrar no sistema por e-mail e senha ou pela conta Google
> **Para** acessar as funcionalidades do meu perfil

**Critérios de aceite**

1. **Dado** que minha conta está ativa, **quando** informo e-mail e senha corretos, **então** recebo um token contendo identificador, perfil, condomínio e tenant.
2. **Dado** que informo credenciais inválidas, **quando** tento entrar, **então** recebo mensagem genérica que não revela se o e-mail existe.
3. **Dado** que minha conta está inativa ou pendente de ativação, **quando** tento entrar, **então** o acesso é recusado com a razão correspondente.
4. **Dado** que excedi as tentativas permitidas, **quando** tento novamente, **então** sou bloqueado temporariamente pelo rate limiting.
5. **Dado** que uso login Google e minha conta não foi criada por convite, **quando** autentico no Google, **então** o acesso é recusado — o login social não cria conta.
6. **Dado** que solicitei recuperação de senha, **quando** uso o link recebido, **então** defino nova senha e todos os tokens anteriores são invalidados.

---

## RF-05 — Gerenciar Condomínio ⚠️

> **Como** Property Manager
> **Quero** cadastrar e manter os condomínios da minha carteira
> **Para** organizar a operação de cada um separadamente

**Critérios de aceite**

1. **Dado** que sou gestor de um tenant, **quando** cadastro um condomínio com nome, CNPJ e endereço completo (CEP, logradouro, número, complemento, bairro, cidade, UF), **então** o condomínio é criado vinculado ao meu tenant.
2. **Dado** que o limite de condomínios do plano foi atingido, **quando** tento cadastrar outro, **então** recebo erro informando o limite contratado.
3. **Dado** que meu tenant é do tipo `CONDOMINIO_DIRETO` e já possui um condomínio, **quando** tento cadastrar outro, **então** a operação é recusada.
4. **Dado** que o condomínio foi criado, **quando** a transação conclui, **então** um evento é publicado e os serviços de domínio recebem o novo condomínio.
5. **Dado** que sou gestor de outro tenant, **quando** tento acessar este condomínio, **então** recebo 403.

---

## RF-06 — Gerenciar Estrutura do Condomínio ✅

> **Como** Property Manager ou Administrador
> **Quero** cadastrar blocos, apartamentos e áreas comuns
> **Para** refletir a estrutura física real do condomínio no sistema

**Critérios de aceite**

1. **Dado** um condomínio cadastrado, **quando** crio um bloco com nome, sigla, número de andares e apartamentos por andar, **então** o bloco é criado vinculado ao condomínio.
2. **Dado** que já existe um bloco com o mesmo nome no condomínio, **quando** tento criar outro, **então** recebo erro de duplicidade — a unicidade é por condomínio, não global.
3. **Dado** um bloco cadastrado, **quando** crio um apartamento com número, andar, quartos, área e fração ideal, **então** o apartamento é criado e o número é único dentro do bloco.
4. **Dado** que cadastro uma área comum, **quando** informo tipo, capacidade, se pode ser reservada e a taxa de locação, **então** a área fica disponível para reserva conforme configurado.
5. **Dado** que dois condomínios diferentes possuem uma área chamada "Salão de Festas", **quando** ambos são cadastrados, **então** os dois são aceitos.
6. **Dado** que um apartamento possui moradores vinculados, **quando** tento excluí-lo, **então** o sistema impede e sugere a inativação.

---

## RF-07 — Gerenciar Vínculos de Ocupantes por Unidade ✅

> **Como** proprietário residente
> **Quero** cadastrar inquilinos, ocupantes e convidados da minha unidade
> **Para** que tenham acesso ao sistema e à portaria conforme o vínculo

**Critérios de aceite**

1. **Dado** que sou `RESIDENT_OWNER` de uma unidade, **quando** convido um `LESSEE`, `OCCUPANT` ou `GUEST` para ela, **então** o convite é emitido com a unidade já vinculada.
2. **Dado** que sou `LESSEE`, **quando** convido alguém, **então** só posso emitir convites para `OCCUPANT` e `GUEST`.
3. **Dado** que a unidade já possui um `RESIDENT_OWNER` ativo, **quando** tento cadastrar outro, **então** a operação é recusada.
4. **Dado** que um ocupante deixa a unidade, **quando** encerro o vínculo, **então** o usuário é inativado e seus tokens deixam de ser aceitos.
5. **Dado** que tento convidar alguém para uma unidade que não é minha, **quando** submeto o convite, **então** recebo 403.

---

## RF-08 — Registrar Entradas e Saídas de Acesso ⚠️

> **Como** porteiro
> **Quero** registrar a entrada e a saída de moradores, funcionários e visitantes
> **Para** manter o controle de quem está no condomínio a cada momento

**Critérios de aceite**

1. **Dado** que estou na guarita, **quando** busco uma pessoa por nome, unidade ou documento, **então** vejo o resultado apenas do meu condomínio.
2. **Dado** que uma pessoa está fora, **quando** registro a entrada, **então** o status passa a `DENTRO` e o horário fica registrado com o porteiro responsável.
3. **Dado** que uma pessoa está dentro, **quando** tento registrar nova entrada sem saída, **então** o sistema alerta sobre a inconsistência.
4. **Dado** que consulto o painel da portaria, **quando** filtro por "dentro agora", **então** vejo moradores, funcionários e visitantes em uma única lista.
5. **Dado** que o `auth-api` está indisponível, **quando** registro um acesso, **então** a operação conclui usando os dados replicados no `user_cache`.

---

## RF-09 — Gerenciar Assembleias e Atas ✅

> **Como** síndico
> **Quero** convocar assembleias e registrar as atas
> **Para** documentar formalmente as decisões do condomínio

**Critérios de aceite**

1. **Dado** que sou síndico, **quando** crio uma assembleia com título, descrição, data e hora de início e fim, **então** ela é criada e os convidados são notificados.
2. **Dado** que a assembleia é online, **quando** confirmo a criação, **então** o sistema gera o link do Google Meet e o vincula ao evento.
3. **Dado** que a integração com o Google falha, **quando** crio a assembleia, **então** ela é criada mesmo assim e o link pode ser adicionado depois.
4. **Dado** que a assembleia foi realizada, **quando** registro a ata com tópicos discutidos, decisões tomadas e presentes, **então** a ata é publicada e fica disponível aos moradores.
5. **Dado** que a assembleia já possui ata, **quando** tento registrar outra, **então** a operação é recusada — a relação é de uma ata por assembleia.
6. **Dado** que sou convidado, **quando** avalio a assembleia, **então** posso registrar nota e comentário uma única vez.

---

## RF-10 — Gerenciar Base de Conhecimento e FAQ ✅

> **Como** administrador
> **Quero** publicar artigos com regras, manuais e orientações
> **Para** reduzir dúvidas recorrentes dos moradores

**Critérios de aceite**

1. **Dado** que sou administrador, **quando** crio um artigo com título, conteúdo e categoria, **então** ele é salvo como rascunho, não visível aos moradores.
2. **Dado** um artigo em rascunho, **quando** o publico, **então** ele passa a ser visível para os usuários do condomínio.
3. **Dado** que sou morador, **quando** consulto a base de conhecimento, **então** vejo apenas artigos publicados do meu condomínio.
4. **Dado** que busco por título ou filtro por categoria, **quando** submeto a busca, **então** recebo os resultados paginados.
5. **Dado** que sou morador, **quando** tento criar ou editar um artigo, **então** recebo 403.

---

## RF-11 — Gerenciar Entregas e Encomendas ⚠️

> **Como** porteiro
> **Quero** registrar as encomendas recebidas e a retirada pelo morador
> **Para** controlar o que está retido na portaria e avisar o destinatário

**Critérios de aceite**

1. **Dado** que recebo uma encomenda, **quando** seleciono o morador destinatário no cadastro e informo remetente e descrição, **então** a entrega é registrada com status `PENDENTE`.
2. **Dado** que a entrega foi registrada, **quando** a transação conclui, **então** o destinatário recebe notificação de encomenda disponível.
3. **Dado** que o morador retira a encomenda, **quando** registro a retirada informando quem retirou, **então** o status passa a `RETIRADA` com data e recebedor.
4. **Dado** que consulto encomendas pendentes, **quando** filtro por bloco ou apartamento, **então** vejo apenas as não retiradas, ordenadas por data de recebimento.
5. **Dado** que uma encomenda está pendente há mais de 7 dias, **quando** o painel é consultado, **então** ela aparece destacada.

---

## RF-12 — Gerenciar Avisos e Comunicados ⚠️

> **Como** síndico
> **Quero** publicar avisos direcionados e acompanhar quem leu
> **Para** comprovar que a comunicação chegou aos moradores

**Critérios de aceite**

1. **Dado** que sou síndico, **quando** publico um aviso com título, mensagem, período de exibição e público-alvo (todos, moradores ou funcionários), **então** ele passa a ser exibido aos destinatários no período definido.
2. **Dado** que sou morador, **quando** abro um aviso, **então** o sistema registra minha leitura com data e hora.
3. **Dado** que sou síndico, **quando** consulto um aviso publicado, **então** vejo o percentual de leitura e a relação de quem já leu.
4. **Dado** que o período de exibição terminou, **quando** um morador acessa a lista, **então** o aviso não aparece entre os ativos, mas permanece no histórico.
5. **Dado** que um aviso é publicado, **quando** a transação conclui, **então** os destinatários recebem notificação.

---

## RF-13 — Controlar Vagas de Estacionamento ⚠️

> **Como** administrador
> **Quero** gerenciar as vagas e permitir que moradores aluguem vagas entre si
> **Para** aproveitar as vagas ociosas do condomínio

**Critérios de aceite**

1. **Dado** que sou administrador, **quando** cadastro uma vaga com número, localização e tipo, **então** ela é criada e o número é único no condomínio.
2. **Dado** que sou proprietário de uma vaga, **quando** publico disponibilidade informando período, **então** a vaga aparece como disponível para outros moradores nesse intervalo.
3. **Dado** que solicito o aluguel de uma vaga disponível, **quando** confirmo período e modalidade, **então** a solicitação é criada como `PENDENTE` e o proprietário é notificado.
4. **Dado** que sou o proprietário da vaga, **quando** aprovo ou recuso a solicitação, **então** o status é atualizado e o solicitante é notificado; na recusa, informo o motivo.
5. **Dado** que existe aluguel aprovado para uma vaga em determinado período, **quando** outra solicitação sobrepõe esse intervalo, **então** o sistema recusa por conflito.
6. **Dado** que não sou o proprietário, **quando** tento aprovar uma solicitação, **então** recebo 403.

---

## RF-14 — Gerenciar Contratos de Locação 📋

> **Como** imobiliária ou proprietário
> **Quero** registrar o contrato de locação da unidade
> **Para** formalizar o vínculo do inquilino e a responsabilidade financeira

**Critérios de aceite**

1. **Dado** que sou proprietário de uma unidade, **quando** registro um contrato com locatário, vigência, valor e responsável financeiro, **então** o contrato é criado como `ATIVO`.
2. **Dado** que o contrato foi criado, **quando** a transação conclui, **então** o locatário é convidado como `LESSEE` da unidade automaticamente.
3. **Dado** que existe contrato ativo para a unidade, **quando** tento registrar outro no mesmo período, **então** o sistema recusa por sobreposição.
4. **Dado** que o contrato define o inquilino como responsável financeiro, **quando** faturas da unidade são geradas, **então** são emitidas em nome dele.
5. **Dado** que o contrato chega ao fim da vigência, **quando** o job diário executa, **então** o status passa a `ENCERRADO` e o vínculo do `LESSEE` é inativado.

---

## RF-15 — Gerar e Gerenciar Faturas por Unidade 📋

> **Como** administrador
> **Quero** gerar as faturas mensais das unidades e acompanhar os pagamentos
> **Para** controlar a arrecadação e a inadimplência do condomínio

**Critérios de aceite**

1. **Dado** que as regras de taxa estão configuradas, **quando** o fechamento mensal executa, **então** o sistema gera uma fatura por unidade ativa, com os itens de taxa, multas e reservas do período.
2. **Dado** que o condomínio usa rateio por fração ideal, **quando** as faturas são geradas, **então** o valor de cada unidade é proporcional à sua fração.
3. **Dado** que o condomínio usa valor fixo, **quando** as faturas são geradas, **então** todas as unidades recebem o mesmo valor de taxa condominial.
4. **Dado** que uma fatura foi emitida, **quando** o morador acessa, **então** vê o detalhamento dos itens e as opções de pagamento por PIX ou boleto.
5. **Dado** que o gateway confirma o pagamento, **quando** o webhook é recebido, **então** a fatura passa a `PAGA` e o morador é notificado.
6. **Dado** que a fatura vence sem pagamento, **quando** o job diário executa, **então** ela passa a `EM_ATRASO` e entra no cálculo de inadimplência.
7. **Dado** que o mesmo webhook é recebido duas vezes, **quando** é processado, **então** a segunda ocorrência é ignorada por idempotência.

---

## RF-16 — Gerenciar Multas 📋

> **Como** síndico
> **Quero** aplicar multas por infração à convenção
> **Para** fazer valer as regras de convivência do condomínio

**Critérios de aceite**

1. **Dado** que sou síndico, **quando** aplico uma multa a uma unidade informando motivo, valor e data da infração, **então** a multa é criada como `APLICADA` e o responsável é notificado.
2. **Dado** que a multa decorre de uma ocorrência registrada, **quando** a aplico a partir dela, **então** a multa fica vinculada à ocorrência de origem.
3. **Dado** que recebi uma multa, **quando** apresento recurso dentro do prazo, **então** a multa passa a `EM_RECURSO` e o síndico é notificado.
4. **Dado** que analiso um recurso, **quando** o defiro ou indefiro, **então** a multa passa a `CANCELADA` ou volta a `APLICADA`, com justificativa registrada.
5. **Dado** que existe multa aplicada e não cancelada, **quando** a fatura da unidade é gerada, **então** a multa é incluída como item.

---

## RF-17 — Registrar Prestação de Contas 📋

> **Como** síndico
> **Quero** registrar receitas e despesas e publicar a prestação de contas
> **Para** dar transparência à gestão financeira do condomínio

**Critérios de aceite**

1. **Dado** que sou síndico, **quando** registro um lançamento com tipo, categoria, valor, data e comprovante, **então** ele é gravado na competência correspondente.
2. **Dado** que a competência foi encerrada, **quando** publico a prestação de contas, **então** ela fica visível aos moradores com receitas, despesas e saldo do período.
3. **Dado** que uma competência foi publicada, **quando** tento alterar um lançamento dela, **então** a operação é recusada — exige estorno registrado.
4. **Dado** que sou morador, **quando** consulto a prestação de contas, **então** vejo as competências publicadas do meu condomínio, com a composição das despesas.
5. **Dado** que consulto uma competência, **quando** os dados são carregados, **então** o saldo apresentado é a diferença entre receitas realizadas e despesas do período.

---

## RF-18 — Gerenciar Visitantes e Pré-Autorizações ⚠️

> **Como** morador
> **Quero** pré-autorizar visitantes com prazo de validade
> **Para** que entrem sem precisar da minha confirmação no interfone

**Critérios de aceite**

1. **Dado** que sou morador, **quando** pré-autorizo um visitante informando nome, documento e período de validade, **então** a autorização é criada vinculada à minha unidade.
2. **Dado** que sou porteiro, **quando** abro a lista de autorizados do dia, **então** vejo os visitantes com autorização vigente para o meu condomínio.
3. **Dado** que um visitante pré-autorizado chega, **quando** confirmo a entrada na lista, **então** o acesso é registrado sem necessidade de contatar o morador.
4. **Dado** que a validade da autorização expirou, **quando** o visitante chega, **então** ele não aparece na lista e o porteiro deve seguir o fluxo de visitante não autorizado.
5. **Dado** que um visitante entrou, **quando** consulto o histórico, **então** vejo quem autorizou, quem liberou e os horários de entrada e saída.
6. **Dado** que sou morador, **quando** cancelo uma autorização antes do uso, **então** o visitante deixa imediatamente de constar na lista da guarita.

---

## RF-19 — Gerenciar Áreas Comuns e Reservas ⚠️

> **Como** morador
> **Quero** reservar áreas comuns do condomínio
> **Para** utilizá-las em data e horário garantidos

**Critérios de aceite**

1. **Dado** que uma área comum permite reserva, **quando** solicito para uma data e horário livres, **então** a reserva é criada como `PENDENTE` ou `CONFIRMADA`, conforme a área exija aprovação.
2. **Dado** que já existe reserva confirmada para a área no mesmo horário, **quando** solicito, **então** o sistema recusa por conflito e sugere horários livres.
3. **Dado** que a área exige antecedência mínima, **quando** solicito dentro desse prazo, **então** a reserva é recusada com a regra informada.
4. **Dado** que já atingi o limite de reservas por mês da minha unidade, **quando** solicito outra, **então** a reserva é recusada informando o limite.
5. **Dado** que a área possui taxa de locação, **quando** a reserva é confirmada, **então** um item de cobrança é lançado na próxima fatura da unidade.
6. **Dado** que a reserva exige aprovação, **quando** o síndico aprova ou recusa, **então** o morador é notificado; na recusa, com o motivo.
7. **Dado** que cancelo uma reserva dentro do prazo previsto na política, **quando** confirmo, **então** a cobrança correspondente é estornada.

---

## RF-20 — Controlar Retirada e Devolução de Chaves ✅

> **Como** porteiro
> **Quero** registrar a retirada e a devolução de chaves
> **Para** saber a todo momento quem está com cada chave

**Critérios de aceite**

1. **Dado** que uma chave está disponível, **quando** registro a retirada informando o responsável, **então** a chave passa a indisponível com data e responsável registrados.
2. **Dado** que uma chave está retirada, **quando** outra pessoa tenta retirá-la, **então** o sistema recusa e informa quem está com ela.
3. **Dado** que a chave é devolvida, **quando** registro a devolução, **então** ela volta a ficar disponível e o ciclo é encerrado no histórico.
4. **Dado** que consulto o painel de chaves, **quando** filtro por indisponíveis, **então** vejo cada chave com responsável e há quanto tempo está retirada.
5. **Dado** que uma chave está retirada há mais de 24 horas, **quando** o painel é consultado, **então** ela aparece destacada.

---

## RF-21 — Configurar Tipos e Regras de Taxas 📋

> **Como** administrador
> **Quero** configurar os tipos de taxa e a forma de rateio
> **Para** que as faturas sejam geradas conforme a convenção do condomínio

**Critérios de aceite**

1. **Dado** que sou administrador, **quando** cadastro um tipo de taxa com nome, valor base e periodicidade, **então** ele fica disponível para compor as faturas.
2. **Dado** que configuro o rateio do condomínio, **quando** escolho entre valor fixo por unidade e proporcional à fração ideal, **então** a regra passa a valer para as próximas gerações de fatura.
3. **Dado** que escolho rateio por fração ideal, **quando** existem unidades sem fração cadastrada, **então** o sistema alerta e impede a geração até a regularização.
4. **Dado** que altero uma regra de taxa, **quando** já existem faturas emitidas na competência, **então** a alteração vale apenas a partir da competência seguinte.
5. **Dado** que uma taxa é extraordinária, **quando** a configuro com número de parcelas, **então** ela é lançada nas faturas pelo número de competências definido.

---

## RF-22 — Gerenciar Reclamações e Ocorrências ⚠️

> **Como** morador
> **Quero** registrar reclamações e acompanhar o andamento
> **Para** que os problemas do condomínio sejam tratados e eu saiba em que pé estão

**Critérios de aceite**

1. **Dado** que sou morador, **quando** registro uma ocorrência com categoria, descrição e anexo opcional, **então** ela é criada com número de protocolo e status `PENDENTE`.
2. **Dado** que registrei uma ocorrência, **quando** o síndico responde, **então** a interação entra no histórico e eu sou notificado.
3. **Dado** que sou síndico, **quando** altero o status para `EM_ANALISE` ou `RESOLVIDO`, **então** a mudança fica registrada com autor, data e justificativa.
4. **Dado** que sou morador, **quando** consulto as ocorrências, **então** vejo apenas as minhas; o síndico vê todas as do condomínio.
5. **Dado** que uma ocorrência exige intervenção técnica, **quando** o síndico abre uma ordem de serviço a partir dela, **então** as duas ficam vinculadas.
6. **Dado** que consulto uma ocorrência, **quando** os dados carregam, **então** vejo o tempo decorrido desde a abertura.

---

## RF-23 — Gerenciar Votações ⚠️

> **Como** síndico
> **Quero** abrir votações e apurar o resultado
> **Para** que as decisões sejam tomadas com participação dos condôminos

**Critérios de aceite**

1. **Dado** que sou síndico, **quando** crio uma votação com título, descrição e opções, **então** ela é criada com status `ABERTA` e vinculada ou não a uma assembleia.
2. **Dado** que uma votação está aberta, **quando** voto pela minha unidade, **então** o voto é registrado uma única vez por unidade.
3. **Dado** que minha unidade já votou, **quando** outro morador da mesma unidade tenta votar, **então** o sistema recusa informando que o voto já foi exercido.
4. **Dado** que a votação está aberta, **quando** consulto os resultados, **então** vejo apenas o total de votos, sem o detalhamento por opção.
5. **Dado** que sou síndico, **quando** encerro a votação, **então** o resultado é apurado, publicado e não aceita mais votos.
6. **Dado** que a votação está vinculada a uma assembleia com ata, **quando** a ata é publicada, **então** o resultado da votação consta nela.

---

## RF-24 — Gerenciar Ordens de Serviço e Manutenção 📋

> **Como** síndico
> **Quero** abrir e acompanhar ordens de serviço com prazo e responsável
> **Para** garantir que a manutenção do condomínio seja executada e medida

**Critérios de aceite**

1. **Dado** que sou síndico, **quando** abro uma ordem de serviço informando descrição, prioridade, prazo e responsável, **então** ela é criada com status `ABERTA`.
2. **Dado** que existe uma ocorrência registrada, **quando** abro uma OS a partir dela, **então** as duas ficam vinculadas e o morador que abriu a ocorrência é notificado.
3. **Dado** que a OS é preventiva, **quando** a abro sem ocorrência de origem, **então** ela é criada normalmente como manutenção programada.
4. **Dado** que uma OS está em execução, **quando** o responsável registra andamento, **então** a interação entra no histórico com data e autor.
5. **Dado** que o prazo da OS venceu sem conclusão, **quando** o job diário executa, **então** ela passa a `ATRASADA` e o síndico é notificado.
6. **Dado** que a OS é concluída, **quando** registro o encerramento com custo e observações, **então** o status passa a `CONCLUIDA` e ela entra no indicador de OS no prazo.

---

## RF-25 — Gerenciar Envio de Mensagens 📋

> **Como** morador
> **Quero** conversar diretamente com a administração e a portaria
> **Para** resolver assuntos pontuais sem abrir uma ocorrência formal

**Critérios de aceite**

1. **Dado** que sou usuário ativo, **quando** inicio uma conversa com a administração ou a portaria, **então** a conversa é criada e a mensagem entregue.
2. **Dado** que recebo uma mensagem, **quando** ela é entregue, **então** recebo notificação e a conversa aparece com indicador de não lida.
3. **Dado** que abro uma conversa, **quando** os dados carregam, **então** vejo o histórico completo em ordem cronológica, paginado.
4. **Dado** que sou morador, **quando** listo minhas conversas, **então** vejo apenas aquelas de que participo.
5. **Dado** que um usuário é inativado, **quando** tento enviar mensagem a ele, **então** o envio é recusado.
6. **Dado** que envio uma mensagem, **quando** o destinatário a lê, **então** o remetente vê a confirmação de leitura.

---

## RF-26 — Gerar Relatórios e Dashboards Analíticos 📋

> **Como** síndico, administrador ou gestor de carteira
> **Quero** acompanhar indicadores consolidados do condomínio ou da carteira
> **Para** tomar decisões com base em dados e identificar tendências

**Critérios de aceite**

1. **Dado** que sou síndico, **quando** acesso o Painel Estratégico, **então** vejo saldo do mês, inadimplência, participação em assembleia, ocorrências dos últimos 30 dias e receita de reservas do meu condomínio.
2. **Dado** que sou administrador, **quando** acesso o Painel Financeiro, **então** vejo receita prevista e realizada, valores em atraso com aging por faixa, despesas, multas e saldo do período.
3. **Dado** que sou porteiro ou síndico, **quando** acesso o Painel Operacional, **então** vejo ocorrências pendentes, tempo médio de resposta, leitura de comunicados, OS no prazo e acessos do dia.
4. **Dado** que sou Property Manager, **quando** acesso o Painel da Carteira, **então** vejo inadimplência e receita consolidadas, ocorrências abertas, OS críticas atrasadas e taxa de ocupação, com abertura por condomínio.
5. **Dado** que o ETL executa às 02h00, **quando** acesso os painéis, **então** os indicadores financeiros refletem o fechamento do dia anterior e os operacionais são atualizados a cada 6 horas.
6. **Dado** que meu perfil é de condomínio, **quando** tento acessar o Painel da Carteira, **então** recebo 403.
7. **Dado** que exporto um relatório, **quando** confirmo, **então** recebo o arquivo apenas com dados do meu escopo de acesso.

---

## RF-27 — Gerenciar Notificações 📋

> **Como** usuário do sistema
> **Quero** ser notificado dos eventos que me dizem respeito
> **Para** não precisar consultar o sistema para descobrir o que mudou

**Critérios de aceite**

1. **Dado** que um evento relevante ocorre (encomenda recebida, fatura próxima do vencimento, assembleia convocada, visitante autorizado, ocorrência respondida), **quando** ele é publicado, **então** os destinatários recebem notificação.
2. **Dado** que possuo notificações não lidas, **quando** acesso o sistema, **então** vejo o contador e a lista, ordenada da mais recente.
3. **Dado** que abro uma notificação, **quando** ela é exibida, **então** é marcada como lida e me leva ao item de origem.
4. **Dado** que configuro minhas preferências, **quando** desativo uma categoria, **então** deixo de receber notificações daquele tipo, exceto as classificadas como obrigatórias.
5. **Dado** que o mesmo evento é entregue duas vezes ao serviço, **quando** é processado, **então** apenas uma notificação é criada.
6. **Dado** que uma notificação tem mais de 90 dias, **quando** o job de limpeza executa, **então** ela é removida.

---

## RF-28 — Gerenciar Funcionários e Turnos ⚠️

> **Como** síndico ou administrador
> **Quero** cadastrar os funcionários e controlar a jornada de trabalho
> **Para** acompanhar a presença da equipe do condomínio

**Critérios de aceite**

1. **Dado** que sou síndico, **quando** cadastro um funcionário com nome, CPF, cargo, matrícula e horário previsto, **então** ele é criado vinculado ao condomínio.
2. **Dado** que um funcionário inicia o expediente, **quando** o registro de entrada é feito, **então** o horário é gravado no turno do dia.
3. **Dado** que o funcionário encerra o expediente, **quando** o registro de saída é feito, **então** o horário é gravado e o turno é fechado.
4. **Dado** que consulto um turno, **quando** os dados carregam, **então** vejo todas as entradas e saídas do período e o total de horas.
5. **Dado** que a matrícula informada já existe no condomínio, **quando** cadastro o funcionário, **então** recebo erro de duplicidade.
6. **Dado** que um funcionário é desligado, **quando** o inativo, **então** ele deixa de aparecer nas listas operacionais e o histórico de turnos é preservado.

---

## Resumo

| Status | Quantidade | Requisitos |
|---|---|---|
| ✅ Implementado | 7 | 3, 4, 6, 7, 9, 10, 20 |
| ⚠️ Parcial | 11 | 1, 5, 8, 11, 12, 13, 18, 19, 22, 23, 28 |
| 📋 Planejado | 10 | 2, 14, 15, 16, 17, 21, 24, 25, 26, 27 |
| **Total** | **28** | |
