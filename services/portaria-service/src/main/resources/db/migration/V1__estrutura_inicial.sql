-- =============================================================================
-- V1: estrutura inicial do portaria-service (banco `mora`)
--
-- Até aqui as tabelas nasciam do Hibernate (`ddl-auto: update`), e o que ele
-- não faz (índice parcial, CHECK de enum que ganhou valor novo) ia por SQL
-- avulso, aplicado à mão em cada máquina. Daqui em diante quem cria e evolui o
-- banco é o Flyway, e o Hibernate só confere (`ddl-auto: validate`).
--
-- O conteúdo é o esquema que o Hibernate gera hoje num banco vazio, lido do
-- catálogo do Postgres. Conferido: aplicado num banco vazio, o resultado é
-- idêntico, coluna a coluna, constraint a constraint.
--
-- Três tabelas (blocos, areas_comuns, funcionario_jornadas) o Hibernate não
-- conseguia criar: a unicidade por condomínio citava `condominioId` sem aspas,
-- o Postgres procurava `condominioid` e recusava o CREATE TABLE inteiro. Em
-- banco novo blocos e areas_comuns só existiam porque o init-databases.sql as
-- criava; funcionario_jornadas simplesmente faltava. Aqui a coluna vai entre
-- aspas e as três nascem com o resto.
--
-- Idempotente de propósito (IF NOT EXISTS): o banco já existe nas máquinas de
-- quem roda o projeto, e o `mora` é dividido com o comunicacao-service. Lá esta
-- versão só cria o que faltar e passa a constar no histórico do Flyway.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Cada CREATE TABLE abaixo vem seguido de um ALTER TABLE ... ADD COLUMN IF NOT
-- EXISTS. Num banco novo ele não faz nada. Num banco que já existia, cria a
-- coluna que a entidade ganhou e o banco ainda não tem: era o que o
-- `ddl-auto: update` fazia na subida, e sem isso o `validate` recusaria subir
-- o serviço num banco atrasado. Entram sem NOT NULL, porque a tabela pode ter
-- linhas.
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS alugueis_vagas (
    id varchar(255) NOT NULL,
    data_fim date NOT NULL,
    data_inicio date NOT NULL,
    valor_penalidade numeric(10,2),
    valor_total numeric(10,2),
    atualizado_em timestamp(6),
    criado_em timestamp(6),
    proprietario_id uuid NOT NULL,
    solicitante_id uuid NOT NULL,
    motivo_recusa varchar(500),
    "condominioId" varchar(255),
    modalidade varchar(255) NOT NULL,
    status varchar(255) NOT NULL,
    vaga_id varchar(255) NOT NULL,
    CONSTRAINT alugueis_vagas_modalidade_check CHECK (modalidade IN ('DIARIA', 'MENSAL')),
    CONSTRAINT alugueis_vagas_status_check CHECK (status IN ('PENDENTE', 'APROVADO', 'RECUSADO', 'CANCELADO', 'CONCLUIDO')),
    CONSTRAINT alugueis_vagas_pkey PRIMARY KEY (id)
);
ALTER TABLE alugueis_vagas
    ADD COLUMN IF NOT EXISTS data_fim date,
    ADD COLUMN IF NOT EXISTS data_inicio date,
    ADD COLUMN IF NOT EXISTS valor_penalidade numeric(10,2),
    ADD COLUMN IF NOT EXISTS valor_total numeric(10,2),
    ADD COLUMN IF NOT EXISTS atualizado_em timestamp(6),
    ADD COLUMN IF NOT EXISTS criado_em timestamp(6),
    ADD COLUMN IF NOT EXISTS proprietario_id uuid,
    ADD COLUMN IF NOT EXISTS solicitante_id uuid,
    ADD COLUMN IF NOT EXISTS motivo_recusa varchar(500),
    ADD COLUMN IF NOT EXISTS "condominioId" varchar(255),
    ADD COLUMN IF NOT EXISTS modalidade varchar(255),
    ADD COLUMN IF NOT EXISTS status varchar(255),
    ADD COLUMN IF NOT EXISTS vaga_id varchar(255);

CREATE TABLE IF NOT EXISTS apartamentos (
    id uuid NOT NULL,
    andar integer NOT NULL,
    "areaMxComTotal" double precision,
    ativo boolean NOT NULL,
    quartos integer,
    "atualizadoEm" timestamp(6),
    "criadoEm" timestamp(6),
    "blocoId" uuid NOT NULL,
    "condominioId" varchar(255),
    morador_responsavel_id varchar(255),
    numero varchar(255) NOT NULL,
    observacoes varchar(255),
    CONSTRAINT apartamentos_pkey PRIMARY KEY (id)
);
ALTER TABLE apartamentos
    ADD COLUMN IF NOT EXISTS andar integer,
    ADD COLUMN IF NOT EXISTS "areaMxComTotal" double precision,
    ADD COLUMN IF NOT EXISTS ativo boolean,
    ADD COLUMN IF NOT EXISTS quartos integer,
    ADD COLUMN IF NOT EXISTS "atualizadoEm" timestamp(6),
    ADD COLUMN IF NOT EXISTS "criadoEm" timestamp(6),
    ADD COLUMN IF NOT EXISTS "blocoId" uuid,
    ADD COLUMN IF NOT EXISTS "condominioId" varchar(255),
    ADD COLUMN IF NOT EXISTS morador_responsavel_id varchar(255),
    ADD COLUMN IF NOT EXISTS numero varchar(255),
    ADD COLUMN IF NOT EXISTS observacoes varchar(255);

CREATE TABLE IF NOT EXISTS areas_comuns (
    id varchar(255) NOT NULL,
    area double precision,
    ativo boolean NOT NULL,
    "capacidadeMaxima" integer,
    exige_aprovacao boolean NOT NULL,
    funcionamento_a_revisar boolean,
    "podeReservar" boolean,
    taxalocacao double precision,
    "atualizadoEm" timestamp(6),
    "criadoEm" timestamp(6),
    "condominioId" varchar(255),
    descricao varchar(255),
    informacoeslimpeza varchar(255),
    localizacao varchar(255),
    modo_funcionamento varchar(255) NOT NULL,
    nome varchar(255) NOT NULL,
    observacoes varchar(255),
    politicacancelamento varchar(255),
    tipo varchar(255) NOT NULL,
    CONSTRAINT areas_comuns_modo_funcionamento_check CHECK (modo_funcionamento IN ('SEM_RESTRICAO', 'HORARIOS_DEFINIDOS')),
    CONSTRAINT areas_comuns_pkey PRIMARY KEY (id),
    CONSTRAINT "areas_comuns_nome_condominioId_key" UNIQUE (nome, "condominioId")
);
ALTER TABLE areas_comuns
    ADD COLUMN IF NOT EXISTS area double precision,
    ADD COLUMN IF NOT EXISTS ativo boolean,
    ADD COLUMN IF NOT EXISTS "capacidadeMaxima" integer,
    ADD COLUMN IF NOT EXISTS exige_aprovacao boolean,
    ADD COLUMN IF NOT EXISTS funcionamento_a_revisar boolean,
    ADD COLUMN IF NOT EXISTS "podeReservar" boolean,
    ADD COLUMN IF NOT EXISTS taxalocacao double precision,
    ADD COLUMN IF NOT EXISTS "atualizadoEm" timestamp(6),
    ADD COLUMN IF NOT EXISTS "criadoEm" timestamp(6),
    ADD COLUMN IF NOT EXISTS "condominioId" varchar(255),
    ADD COLUMN IF NOT EXISTS descricao varchar(255),
    ADD COLUMN IF NOT EXISTS informacoeslimpeza varchar(255),
    ADD COLUMN IF NOT EXISTS localizacao varchar(255),
    ADD COLUMN IF NOT EXISTS modo_funcionamento varchar(255),
    ADD COLUMN IF NOT EXISTS nome varchar(255),
    ADD COLUMN IF NOT EXISTS observacoes varchar(255),
    ADD COLUMN IF NOT EXISTS politicacancelamento varchar(255),
    ADD COLUMN IF NOT EXISTS tipo varchar(255);

CREATE TABLE IF NOT EXISTS areas_comuns_horarios (
    id varchar(255) NOT NULL,
    abertura time(6) without time zone,
    fechado boolean NOT NULL,
    fechamento time(6) without time zone,
    area_comum_id varchar(255) NOT NULL,
    dia_semana varchar(255) NOT NULL,
    CONSTRAINT areas_comuns_horarios_dia_semana_check CHECK (dia_semana IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY')),
    CONSTRAINT areas_comuns_horarios_pkey PRIMARY KEY (id),
    CONSTRAINT uk_area_dia UNIQUE (area_comum_id, dia_semana)
);
ALTER TABLE areas_comuns_horarios
    ADD COLUMN IF NOT EXISTS abertura time(6) without time zone,
    ADD COLUMN IF NOT EXISTS fechado boolean,
    ADD COLUMN IF NOT EXISTS fechamento time(6) without time zone,
    ADD COLUMN IF NOT EXISTS area_comum_id varchar(255),
    ADD COLUMN IF NOT EXISTS dia_semana varchar(255);

CREATE TABLE IF NOT EXISTS blocos (
    id uuid NOT NULL,
    andares integer,
    "apartamentosPorAndar" integer,
    ativo boolean NOT NULL,
    "atualizadoEm" timestamp(6),
    "criadoEm" timestamp(6),
    "condominioId" varchar(255),
    descricao varchar(255),
    nome varchar(255) NOT NULL,
    sigla varchar(255),
    CONSTRAINT blocos_pkey PRIMARY KEY (id),
    CONSTRAINT "blocos_nome_condominioId_key" UNIQUE (nome, "condominioId")
);
ALTER TABLE blocos
    ADD COLUMN IF NOT EXISTS andares integer,
    ADD COLUMN IF NOT EXISTS "apartamentosPorAndar" integer,
    ADD COLUMN IF NOT EXISTS ativo boolean,
    ADD COLUMN IF NOT EXISTS "atualizadoEm" timestamp(6),
    ADD COLUMN IF NOT EXISTS "criadoEm" timestamp(6),
    ADD COLUMN IF NOT EXISTS "condominioId" varchar(255),
    ADD COLUMN IF NOT EXISTS descricao varchar(255),
    ADD COLUMN IF NOT EXISTS nome varchar(255),
    ADD COLUMN IF NOT EXISTS sigla varchar(255);

CREATE TABLE IF NOT EXISTS chaves (
    id varchar(255) NOT NULL,
    disponivel boolean NOT NULL,
    "criadoEm" timestamp(6),
    "condominioId" varchar(255),
    descricao varchar(255),
    local_id varchar(255),
    local_nome varchar(255),
    nome_chave varchar(255) NOT NULL,
    nome_normalizado varchar(255),
    tipo_local varchar(255),
    CONSTRAINT chaves_tipo_local_check CHECK (tipo_local IN ('BLOCO', 'AREA_COMUM')),
    CONSTRAINT chaves_pkey PRIMARY KEY (id)
);
ALTER TABLE chaves
    ADD COLUMN IF NOT EXISTS disponivel boolean,
    ADD COLUMN IF NOT EXISTS "criadoEm" timestamp(6),
    ADD COLUMN IF NOT EXISTS "condominioId" varchar(255),
    ADD COLUMN IF NOT EXISTS descricao varchar(255),
    ADD COLUMN IF NOT EXISTS local_id varchar(255),
    ADD COLUMN IF NOT EXISTS local_nome varchar(255),
    ADD COLUMN IF NOT EXISTS nome_chave varchar(255),
    ADD COLUMN IF NOT EXISTS nome_normalizado varchar(255),
    ADD COLUMN IF NOT EXISTS tipo_local varchar(255);

CREATE TABLE IF NOT EXISTS disponibilidade_vagas (
    id varchar(255) NOT NULL,
    ativa boolean NOT NULL,
    data_fim date NOT NULL,
    data_inicio date NOT NULL,
    criado_em timestamp(6),
    proprietario_id uuid NOT NULL,
    "condominioId" varchar(255),
    vaga_id varchar(255) NOT NULL,
    CONSTRAINT disponibilidade_vagas_pkey PRIMARY KEY (id)
);
ALTER TABLE disponibilidade_vagas
    ADD COLUMN IF NOT EXISTS ativa boolean,
    ADD COLUMN IF NOT EXISTS data_fim date,
    ADD COLUMN IF NOT EXISTS data_inicio date,
    ADD COLUMN IF NOT EXISTS criado_em timestamp(6),
    ADD COLUMN IF NOT EXISTS proprietario_id uuid,
    ADD COLUMN IF NOT EXISTS "condominioId" varchar(255),
    ADD COLUMN IF NOT EXISTS vaga_id varchar(255);

CREATE TABLE IF NOT EXISTS entregas (
    id varchar(255) NOT NULL,
    retirada boolean NOT NULL,
    atualizado_em timestamp(6),
    criado_em timestamp(6),
    data_recebimento timestamp(6),
    data_retirada timestamp(6),
    destinatario_id bigint,
    morador_id uuid,
    apartamento varchar(255),
    bloco varchar(255),
    "condominioId" varchar(255),
    descricao varchar(255),
    destinatario varchar(255),
    destinatario_nome varchar(255),
    nome_entregador varchar(255),
    observacoes varchar(255),
    recebedor_nome varchar(255),
    remetente varchar(255),
    status varchar(255),
    CONSTRAINT entregas_pkey PRIMARY KEY (id)
);
ALTER TABLE entregas
    ADD COLUMN IF NOT EXISTS retirada boolean,
    ADD COLUMN IF NOT EXISTS atualizado_em timestamp(6),
    ADD COLUMN IF NOT EXISTS criado_em timestamp(6),
    ADD COLUMN IF NOT EXISTS data_recebimento timestamp(6),
    ADD COLUMN IF NOT EXISTS data_retirada timestamp(6),
    ADD COLUMN IF NOT EXISTS destinatario_id bigint,
    ADD COLUMN IF NOT EXISTS morador_id uuid,
    ADD COLUMN IF NOT EXISTS apartamento varchar(255),
    ADD COLUMN IF NOT EXISTS bloco varchar(255),
    ADD COLUMN IF NOT EXISTS "condominioId" varchar(255),
    ADD COLUMN IF NOT EXISTS descricao varchar(255),
    ADD COLUMN IF NOT EXISTS destinatario varchar(255),
    ADD COLUMN IF NOT EXISTS destinatario_nome varchar(255),
    ADD COLUMN IF NOT EXISTS nome_entregador varchar(255),
    ADD COLUMN IF NOT EXISTS observacoes varchar(255),
    ADD COLUMN IF NOT EXISTS recebedor_nome varchar(255),
    ADD COLUMN IF NOT EXISTS remetente varchar(255),
    ADD COLUMN IF NOT EXISTS status varchar(255);

CREATE TABLE IF NOT EXISTS funcionario_auditoria (
    id varchar(255) NOT NULL,
    criado_em timestamp(6),
    valor_anterior varchar(1000),
    valor_novo varchar(1000),
    auth_user_id varchar(255) NOT NULL,
    autor_id varchar(255),
    autor_nome varchar(255),
    campo varchar(255) NOT NULL,
    "condominioId" varchar(255),
    funcionario_nome varchar(255),
    CONSTRAINT funcionario_auditoria_pkey PRIMARY KEY (id)
);
ALTER TABLE funcionario_auditoria
    ADD COLUMN IF NOT EXISTS criado_em timestamp(6),
    ADD COLUMN IF NOT EXISTS valor_anterior varchar(1000),
    ADD COLUMN IF NOT EXISTS valor_novo varchar(1000),
    ADD COLUMN IF NOT EXISTS auth_user_id varchar(255),
    ADD COLUMN IF NOT EXISTS autor_id varchar(255),
    ADD COLUMN IF NOT EXISTS autor_nome varchar(255),
    ADD COLUMN IF NOT EXISTS campo varchar(255),
    ADD COLUMN IF NOT EXISTS "condominioId" varchar(255),
    ADD COLUMN IF NOT EXISTS funcionario_nome varchar(255);

CREATE TABLE IF NOT EXISTS funcionario_jornadas (
    id varchar(255) NOT NULL,
    ciclo_inicio date,
    ciclo_tamanho integer,
    atualizado_em timestamp(6),
    criado_em timestamp(6),
    observacoes varchar(500),
    auth_user_id varchar(255) NOT NULL,
    "condominioId" varchar(255),
    nome varchar(255),
    situacao varchar(255) NOT NULL,
    tipo_jornada varchar(255) NOT NULL,
    CONSTRAINT funcionario_jornadas_situacao_check CHECK (situacao IN ('ATIVO', 'SUSPENSO', 'AFASTADO', 'DEMITIDO')),
    CONSTRAINT funcionario_jornadas_tipo_jornada_check CHECK (tipo_jornada IN ('SEMANAL_FIXA', 'ESCALA')),
    CONSTRAINT funcionario_jornadas_pkey PRIMARY KEY (id),
    CONSTRAINT uk_jornada_funcionario UNIQUE (auth_user_id, "condominioId")
);
ALTER TABLE funcionario_jornadas
    ADD COLUMN IF NOT EXISTS ciclo_inicio date,
    ADD COLUMN IF NOT EXISTS ciclo_tamanho integer,
    ADD COLUMN IF NOT EXISTS atualizado_em timestamp(6),
    ADD COLUMN IF NOT EXISTS criado_em timestamp(6),
    ADD COLUMN IF NOT EXISTS observacoes varchar(500),
    ADD COLUMN IF NOT EXISTS auth_user_id varchar(255),
    ADD COLUMN IF NOT EXISTS "condominioId" varchar(255),
    ADD COLUMN IF NOT EXISTS nome varchar(255),
    ADD COLUMN IF NOT EXISTS situacao varchar(255),
    ADD COLUMN IF NOT EXISTS tipo_jornada varchar(255);

CREATE TABLE IF NOT EXISTS funcionario_turno_dias (
    id varchar(255) NOT NULL,
    fim time(6) without time zone,
    folga boolean NOT NULL,
    inicio time(6) without time zone,
    posicao integer,
    dia_semana varchar(255),
    jornada_id varchar(255) NOT NULL,
    CONSTRAINT funcionario_turno_dias_dia_semana_check CHECK (dia_semana IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY')),
    CONSTRAINT funcionario_turno_dias_pkey PRIMARY KEY (id)
);
ALTER TABLE funcionario_turno_dias
    ADD COLUMN IF NOT EXISTS fim time(6) without time zone,
    ADD COLUMN IF NOT EXISTS folga boolean,
    ADD COLUMN IF NOT EXISTS inicio time(6) without time zone,
    ADD COLUMN IF NOT EXISTS posicao integer,
    ADD COLUMN IF NOT EXISTS dia_semana varchar(255),
    ADD COLUMN IF NOT EXISTS jornada_id varchar(255);

CREATE TABLE IF NOT EXISTS funcionarios (
    id varchar(255) NOT NULL,
    ativo boolean NOT NULL,
    horario_entrada time(6) without time zone,
    horario_saida time(6) without time zone,
    atualizado_em timestamp(6),
    criado_em timestamp(6),
    cargo varchar(255) NOT NULL,
    "condominioId" varchar(255),
    cpf varchar(255) NOT NULL,
    email varchar(255),
    matricula varchar(255),
    nome varchar(255) NOT NULL,
    telefone varchar(255),
    CONSTRAINT funcionarios_pkey PRIMARY KEY (id),
    CONSTRAINT funcionarios_cpf_key UNIQUE (cpf)
);
ALTER TABLE funcionarios
    ADD COLUMN IF NOT EXISTS ativo boolean,
    ADD COLUMN IF NOT EXISTS horario_entrada time(6) without time zone,
    ADD COLUMN IF NOT EXISTS horario_saida time(6) without time zone,
    ADD COLUMN IF NOT EXISTS atualizado_em timestamp(6),
    ADD COLUMN IF NOT EXISTS criado_em timestamp(6),
    ADD COLUMN IF NOT EXISTS cargo varchar(255),
    ADD COLUMN IF NOT EXISTS "condominioId" varchar(255),
    ADD COLUMN IF NOT EXISTS cpf varchar(255),
    ADD COLUMN IF NOT EXISTS email varchar(255),
    ADD COLUMN IF NOT EXISTS matricula varchar(255),
    ADD COLUMN IF NOT EXISTS nome varchar(255),
    ADD COLUMN IF NOT EXISTS telefone varchar(255);

CREATE TABLE IF NOT EXISTS liberacoes_excepcionais (
    id varchar(255) NOT NULL,
    data date NOT NULL,
    hora_fim time(6) without time zone NOT NULL,
    hora_inicio time(6) without time zone NOT NULL,
    autorizado_em timestamp(6),
    utilizada_em timestamp(6),
    motivo varchar(500) NOT NULL,
    observacoes varchar(500),
    auth_user_id varchar(255) NOT NULL,
    autorizado_por_id varchar(255),
    autorizado_por_nome varchar(255),
    "condominioId" varchar(255),
    funcionario_nome varchar(255),
    status varchar(255),
    CONSTRAINT liberacoes_excepcionais_status_check CHECK (status IN ('AGUARDANDO', 'UTILIZADA', 'EXPIRADA', 'CANCELADA')),
    CONSTRAINT liberacoes_excepcionais_pkey PRIMARY KEY (id)
);
ALTER TABLE liberacoes_excepcionais
    ADD COLUMN IF NOT EXISTS data date,
    ADD COLUMN IF NOT EXISTS hora_fim time(6) without time zone,
    ADD COLUMN IF NOT EXISTS hora_inicio time(6) without time zone,
    ADD COLUMN IF NOT EXISTS autorizado_em timestamp(6),
    ADD COLUMN IF NOT EXISTS utilizada_em timestamp(6),
    ADD COLUMN IF NOT EXISTS motivo varchar(500),
    ADD COLUMN IF NOT EXISTS observacoes varchar(500),
    ADD COLUMN IF NOT EXISTS auth_user_id varchar(255),
    ADD COLUMN IF NOT EXISTS autorizado_por_id varchar(255),
    ADD COLUMN IF NOT EXISTS autorizado_por_nome varchar(255),
    ADD COLUMN IF NOT EXISTS "condominioId" varchar(255),
    ADD COLUMN IF NOT EXISTS funcionario_nome varchar(255),
    ADD COLUMN IF NOT EXISTS status varchar(255);

CREATE TABLE IF NOT EXISTS moradores (
    id varchar(255) NOT NULL,
    ativo boolean NOT NULL,
    atualizado_em timestamp(6),
    criado_em timestamp(6),
    apartamento_id uuid,
    bloco_id uuid,
    "condominioId" varchar(255),
    cpf varchar(255) NOT NULL,
    email varchar(255),
    nome varchar(255) NOT NULL,
    telefone varchar(255),
    CONSTRAINT moradores_pkey PRIMARY KEY (id),
    CONSTRAINT moradores_cpf_key UNIQUE (cpf)
);
ALTER TABLE moradores
    ADD COLUMN IF NOT EXISTS ativo boolean,
    ADD COLUMN IF NOT EXISTS atualizado_em timestamp(6),
    ADD COLUMN IF NOT EXISTS criado_em timestamp(6),
    ADD COLUMN IF NOT EXISTS apartamento_id uuid,
    ADD COLUMN IF NOT EXISTS bloco_id uuid,
    ADD COLUMN IF NOT EXISTS "condominioId" varchar(255),
    ADD COLUMN IF NOT EXISTS cpf varchar(255),
    ADD COLUMN IF NOT EXISTS email varchar(255),
    ADD COLUMN IF NOT EXISTS nome varchar(255),
    ADD COLUMN IF NOT EXISTS telefone varchar(255);

CREATE TABLE IF NOT EXISTS movimentacoes_chaves (
    id varchar(255) NOT NULL,
    data_devolucao timestamp(6),
    data_retirada timestamp(6) NOT NULL,
    chave_id varchar(255) NOT NULL,
    chave_nome varchar(255),
    "condominioId" varchar(255),
    nome_responsavel varchar(255),
    perfil_responsavel varchar(255),
    registrado_por_id varchar(255),
    registrado_por_nome varchar(255),
    responsavel_id varchar(255),
    CONSTRAINT movimentacoes_chaves_pkey PRIMARY KEY (id)
);
ALTER TABLE movimentacoes_chaves
    ADD COLUMN IF NOT EXISTS data_devolucao timestamp(6),
    ADD COLUMN IF NOT EXISTS data_retirada timestamp(6),
    ADD COLUMN IF NOT EXISTS chave_id varchar(255),
    ADD COLUMN IF NOT EXISTS chave_nome varchar(255),
    ADD COLUMN IF NOT EXISTS "condominioId" varchar(255),
    ADD COLUMN IF NOT EXISTS nome_responsavel varchar(255),
    ADD COLUMN IF NOT EXISTS perfil_responsavel varchar(255),
    ADD COLUMN IF NOT EXISTS registrado_por_id varchar(255),
    ADD COLUMN IF NOT EXISTS registrado_por_nome varchar(255),
    ADD COLUMN IF NOT EXISTS responsavel_id varchar(255);

CREATE TABLE IF NOT EXISTS movimentacoes_veiculo (
    id varchar(255) NOT NULL,
    entrada_em timestamp(6) NOT NULL,
    saida_em timestamp(6),
    "condominioId" varchar(255),
    modelo varchar(255),
    placa varchar(255) NOT NULL,
    registrado_por_entrada_id varchar(255),
    registrado_por_saida_id varchar(255),
    vaga_id varchar(255),
    veiculo_id varchar(255),
    vinculado_nome varchar(255),
    CONSTRAINT movimentacoes_veiculo_pkey PRIMARY KEY (id)
);
ALTER TABLE movimentacoes_veiculo
    ADD COLUMN IF NOT EXISTS entrada_em timestamp(6),
    ADD COLUMN IF NOT EXISTS saida_em timestamp(6),
    ADD COLUMN IF NOT EXISTS "condominioId" varchar(255),
    ADD COLUMN IF NOT EXISTS modelo varchar(255),
    ADD COLUMN IF NOT EXISTS placa varchar(255),
    ADD COLUMN IF NOT EXISTS registrado_por_entrada_id varchar(255),
    ADD COLUMN IF NOT EXISTS registrado_por_saida_id varchar(255),
    ADD COLUMN IF NOT EXISTS vaga_id varchar(255),
    ADD COLUMN IF NOT EXISTS veiculo_id varchar(255),
    ADD COLUMN IF NOT EXISTS vinculado_nome varchar(255);

CREATE TABLE IF NOT EXISTS pre_autorizacoes (
    id varchar(255) NOT NULL,
    validade_fim date NOT NULL,
    validade_inicio date NOT NULL,
    atualizado_em timestamp(6),
    criado_em timestamp(6),
    utilizada_em timestamp(6),
    placa_veiculo varchar(10),
    cpf_visitante varchar(14),
    telefone_visitante varchar(20),
    observacoes varchar(500),
    "condominioId" varchar(255),
    cor_veiculo varchar(255),
    empresa varchar(255),
    modelo_veiculo varchar(255),
    morador_id varchar(255) NOT NULL,
    nome_visitante varchar(255) NOT NULL,
    status varchar(255),
    tipo_pessoa varchar(255),
    unidade_id varchar(255),
    visitante_id varchar(255),
    CONSTRAINT pre_autorizacoes_status_check CHECK (status IN ('AGUARDANDO', 'UTILIZADA', 'EXPIRADA', 'CANCELADA')),
    CONSTRAINT pre_autorizacoes_tipo_pessoa_check CHECK (tipo_pessoa IN ('VISITA', 'SERVICO')),
    CONSTRAINT pre_autorizacoes_pkey PRIMARY KEY (id)
);
ALTER TABLE pre_autorizacoes
    ADD COLUMN IF NOT EXISTS validade_fim date,
    ADD COLUMN IF NOT EXISTS validade_inicio date,
    ADD COLUMN IF NOT EXISTS atualizado_em timestamp(6),
    ADD COLUMN IF NOT EXISTS criado_em timestamp(6),
    ADD COLUMN IF NOT EXISTS utilizada_em timestamp(6),
    ADD COLUMN IF NOT EXISTS placa_veiculo varchar(10),
    ADD COLUMN IF NOT EXISTS cpf_visitante varchar(14),
    ADD COLUMN IF NOT EXISTS telefone_visitante varchar(20),
    ADD COLUMN IF NOT EXISTS observacoes varchar(500),
    ADD COLUMN IF NOT EXISTS "condominioId" varchar(255),
    ADD COLUMN IF NOT EXISTS cor_veiculo varchar(255),
    ADD COLUMN IF NOT EXISTS empresa varchar(255),
    ADD COLUMN IF NOT EXISTS modelo_veiculo varchar(255),
    ADD COLUMN IF NOT EXISTS morador_id varchar(255),
    ADD COLUMN IF NOT EXISTS nome_visitante varchar(255),
    ADD COLUMN IF NOT EXISTS status varchar(255),
    ADD COLUMN IF NOT EXISTS tipo_pessoa varchar(255),
    ADD COLUMN IF NOT EXISTS unidade_id varchar(255),
    ADD COLUMN IF NOT EXISTS visitante_id varchar(255);

CREATE TABLE IF NOT EXISTS reserva_auditoria (
    id varchar(255) NOT NULL,
    criado_em timestamp(6),
    motivo varchar(500),
    autor_id varchar(255),
    autor_nome varchar(255),
    "condominioId" varchar(255),
    reserva_id varchar(255) NOT NULL,
    status_anterior varchar(255),
    status_novo varchar(255),
    CONSTRAINT reserva_auditoria_status_anterior_check CHECK (status_anterior IN ('PENDENTE', 'APROVADA', 'RECUSADA', 'EXPIRADA', 'CANCELADA', 'CONCLUIDA')),
    CONSTRAINT reserva_auditoria_status_novo_check CHECK (status_novo IN ('PENDENTE', 'APROVADA', 'RECUSADA', 'EXPIRADA', 'CANCELADA', 'CONCLUIDA')),
    CONSTRAINT reserva_auditoria_pkey PRIMARY KEY (id)
);
ALTER TABLE reserva_auditoria
    ADD COLUMN IF NOT EXISTS criado_em timestamp(6),
    ADD COLUMN IF NOT EXISTS motivo varchar(500),
    ADD COLUMN IF NOT EXISTS autor_id varchar(255),
    ADD COLUMN IF NOT EXISTS autor_nome varchar(255),
    ADD COLUMN IF NOT EXISTS "condominioId" varchar(255),
    ADD COLUMN IF NOT EXISTS reserva_id varchar(255),
    ADD COLUMN IF NOT EXISTS status_anterior varchar(255),
    ADD COLUMN IF NOT EXISTS status_novo varchar(255);

CREATE TABLE IF NOT EXISTS reservas (
    id varchar(255) NOT NULL,
    valortotal numeric(10,2),
    aprovado_em timestamp(6),
    atualizado_em timestamp(6),
    cancelado_em timestamp(6),
    criado_em timestamp(6),
    fim timestamp(6) NOT NULL,
    inicio timestamp(6) NOT NULL,
    descricao_evento varchar(500),
    motivo_recusa varchar(500),
    observacoes varchar(500),
    aprovado_por_id varchar(255),
    aprovado_por_nome varchar(255),
    area_comum_id varchar(255),
    cancelado_por_id varchar(255),
    cancelado_por_nome varchar(255),
    "condominioId" varchar(255),
    responsavel_id varchar(255),
    responsavel_nome varchar(255),
    responsavel_perfil varchar(255),
    solicitante_id varchar(255) NOT NULL,
    status varchar(255),
    tipo_reserva varchar(255) NOT NULL,
    unidade_id varchar(255),
    CONSTRAINT reservas_status_check CHECK (status IN ('PENDENTE', 'APROVADA', 'RECUSADA', 'EXPIRADA', 'CANCELADA', 'CONCLUIDA')),
    CONSTRAINT reservas_tipo_reserva_check CHECK (tipo_reserva IN ('MORADOR', 'EVENTO_CONDOMINIO')),
    CONSTRAINT reservas_pkey PRIMARY KEY (id)
);
ALTER TABLE reservas
    ADD COLUMN IF NOT EXISTS valortotal numeric(10,2),
    ADD COLUMN IF NOT EXISTS aprovado_em timestamp(6),
    ADD COLUMN IF NOT EXISTS atualizado_em timestamp(6),
    ADD COLUMN IF NOT EXISTS cancelado_em timestamp(6),
    ADD COLUMN IF NOT EXISTS criado_em timestamp(6),
    ADD COLUMN IF NOT EXISTS fim timestamp(6),
    ADD COLUMN IF NOT EXISTS inicio timestamp(6),
    ADD COLUMN IF NOT EXISTS descricao_evento varchar(500),
    ADD COLUMN IF NOT EXISTS motivo_recusa varchar(500),
    ADD COLUMN IF NOT EXISTS observacoes varchar(500),
    ADD COLUMN IF NOT EXISTS aprovado_por_id varchar(255),
    ADD COLUMN IF NOT EXISTS aprovado_por_nome varchar(255),
    ADD COLUMN IF NOT EXISTS area_comum_id varchar(255),
    ADD COLUMN IF NOT EXISTS cancelado_por_id varchar(255),
    ADD COLUMN IF NOT EXISTS cancelado_por_nome varchar(255),
    ADD COLUMN IF NOT EXISTS "condominioId" varchar(255),
    ADD COLUMN IF NOT EXISTS responsavel_id varchar(255),
    ADD COLUMN IF NOT EXISTS responsavel_nome varchar(255),
    ADD COLUMN IF NOT EXISTS responsavel_perfil varchar(255),
    ADD COLUMN IF NOT EXISTS solicitante_id varchar(255),
    ADD COLUMN IF NOT EXISTS status varchar(255),
    ADD COLUMN IF NOT EXISTS tipo_reserva varchar(255),
    ADD COLUMN IF NOT EXISTS unidade_id varchar(255);

CREATE TABLE IF NOT EXISTS turno_entradas (
    entrada timestamp(6),
    turno_id varchar(255) NOT NULL
);
ALTER TABLE turno_entradas
    ADD COLUMN IF NOT EXISTS entrada timestamp(6),
    ADD COLUMN IF NOT EXISTS turno_id varchar(255);

CREATE TABLE IF NOT EXISTS turno_saidas (
    saida timestamp(6),
    turno_id varchar(255) NOT NULL
);
ALTER TABLE turno_saidas
    ADD COLUMN IF NOT EXISTS saida timestamp(6),
    ADD COLUMN IF NOT EXISTS turno_id varchar(255);

CREATE TABLE IF NOT EXISTS turnos (
    id varchar(255) NOT NULL,
    cargo varchar(255) NOT NULL,
    "condominioId" varchar(255),
    funcionario varchar(255) NOT NULL,
    CONSTRAINT turnos_pkey PRIMARY KEY (id)
);
ALTER TABLE turnos
    ADD COLUMN IF NOT EXISTS cargo varchar(255),
    ADD COLUMN IF NOT EXISTS "condominioId" varchar(255),
    ADD COLUMN IF NOT EXISTS funcionario varchar(255);

CREATE TABLE IF NOT EXISTS vagas_estacionamento (
    id varchar(255) NOT NULL,
    ativa boolean NOT NULL,
    atualizado_em timestamp(6),
    criado_em timestamp(6),
    apartamento_id uuid,
    "condominioId" varchar(255),
    localizacao varchar(255),
    numero varchar(255) NOT NULL,
    tipo varchar(255),
    CONSTRAINT vagas_estacionamento_pkey PRIMARY KEY (id),
    CONSTRAINT vagas_estacionamento_numero_key UNIQUE (numero)
);
ALTER TABLE vagas_estacionamento
    ADD COLUMN IF NOT EXISTS ativa boolean,
    ADD COLUMN IF NOT EXISTS atualizado_em timestamp(6),
    ADD COLUMN IF NOT EXISTS criado_em timestamp(6),
    ADD COLUMN IF NOT EXISTS apartamento_id uuid,
    ADD COLUMN IF NOT EXISTS "condominioId" varchar(255),
    ADD COLUMN IF NOT EXISTS localizacao varchar(255),
    ADD COLUMN IF NOT EXISTS numero varchar(255),
    ADD COLUMN IF NOT EXISTS tipo varchar(255);

CREATE TABLE IF NOT EXISTS veiculo_pessoas (
    id varchar(255) NOT NULL,
    criado_em timestamp(6),
    "condominioId" varchar(255),
    pessoa_id varchar(255) NOT NULL,
    pessoa_nome varchar(255),
    unidade_id varchar(255),
    veiculo_id varchar(255) NOT NULL,
    CONSTRAINT veiculo_pessoas_pkey PRIMARY KEY (id),
    CONSTRAINT uk_veiculo_pessoa UNIQUE (veiculo_id, pessoa_id)
);
ALTER TABLE veiculo_pessoas
    ADD COLUMN IF NOT EXISTS criado_em timestamp(6),
    ADD COLUMN IF NOT EXISTS "condominioId" varchar(255),
    ADD COLUMN IF NOT EXISTS pessoa_id varchar(255),
    ADD COLUMN IF NOT EXISTS pessoa_nome varchar(255),
    ADD COLUMN IF NOT EXISTS unidade_id varchar(255),
    ADD COLUMN IF NOT EXISTS veiculo_id varchar(255);

CREATE TABLE IF NOT EXISTS veiculos (
    id varchar(255) NOT NULL,
    atualizado_em timestamp(6),
    criado_em timestamp(6),
    data_entrada timestamp(6),
    data_saida timestamp(6),
    categoria varchar(255) NOT NULL,
    "condominioId" varchar(255),
    cor varchar(255),
    modelo varchar(255),
    obs varchar(255),
    placa varchar(255) NOT NULL,
    proprietario_cpf varchar(255),
    proprietario_id varchar(255),
    proprietario_nome varchar(255),
    status varchar(255),
    tipo_proprietario varchar(255),
    vaga_id varchar(255),
    CONSTRAINT veiculos_categoria_check CHECK (categoria IN ('CARRO', 'MOTO', 'VEICULO_SERVICO')),
    CONSTRAINT veiculos_status_check CHECK (status IN ('DENTRO', 'SAIU')),
    CONSTRAINT veiculos_tipo_proprietario_check CHECK (tipo_proprietario IN ('MORADOR', 'VISITANTE', 'FUNCIONARIO')),
    CONSTRAINT veiculos_pkey PRIMARY KEY (id),
    CONSTRAINT veiculos_placa_key UNIQUE (placa)
);
ALTER TABLE veiculos
    ADD COLUMN IF NOT EXISTS atualizado_em timestamp(6),
    ADD COLUMN IF NOT EXISTS criado_em timestamp(6),
    ADD COLUMN IF NOT EXISTS data_entrada timestamp(6),
    ADD COLUMN IF NOT EXISTS data_saida timestamp(6),
    ADD COLUMN IF NOT EXISTS categoria varchar(255),
    ADD COLUMN IF NOT EXISTS "condominioId" varchar(255),
    ADD COLUMN IF NOT EXISTS cor varchar(255),
    ADD COLUMN IF NOT EXISTS modelo varchar(255),
    ADD COLUMN IF NOT EXISTS obs varchar(255),
    ADD COLUMN IF NOT EXISTS placa varchar(255),
    ADD COLUMN IF NOT EXISTS proprietario_cpf varchar(255),
    ADD COLUMN IF NOT EXISTS proprietario_id varchar(255),
    ADD COLUMN IF NOT EXISTS proprietario_nome varchar(255),
    ADD COLUMN IF NOT EXISTS status varchar(255),
    ADD COLUMN IF NOT EXISTS tipo_proprietario varchar(255),
    ADD COLUMN IF NOT EXISTS vaga_id varchar(255);

CREATE TABLE IF NOT EXISTS visitantes (
    id varchar(255) NOT NULL,
    ativo boolean NOT NULL,
    atualizado_em timestamp(6),
    criado_em timestamp(6),
    horario_entrada timestamp(6),
    horario_saida timestamp(6),
    apartamento_id uuid,
    bloco_id uuid,
    "condominioId" varchar(255),
    cpf varchar(255) NOT NULL,
    destino varchar(255),
    documento varchar(255),
    email varchar(255),
    empresa varchar(255),
    motivo_visita varchar(255),
    nome varchar(255) NOT NULL,
    status varchar(255),
    telefone varchar(255),
    tipo_visita varchar(255),
    CONSTRAINT visitantes_status_check CHECK (status IN ('DENTRO', 'SAIU')),
    CONSTRAINT visitantes_tipo_visita_check CHECK (tipo_visita IN ('VISITA', 'SERVICO')),
    CONSTRAINT visitantes_pkey PRIMARY KEY (id),
    CONSTRAINT visitantes_cpf_key UNIQUE (cpf)
);
ALTER TABLE visitantes
    ADD COLUMN IF NOT EXISTS ativo boolean,
    ADD COLUMN IF NOT EXISTS atualizado_em timestamp(6),
    ADD COLUMN IF NOT EXISTS criado_em timestamp(6),
    ADD COLUMN IF NOT EXISTS horario_entrada timestamp(6),
    ADD COLUMN IF NOT EXISTS horario_saida timestamp(6),
    ADD COLUMN IF NOT EXISTS apartamento_id uuid,
    ADD COLUMN IF NOT EXISTS bloco_id uuid,
    ADD COLUMN IF NOT EXISTS "condominioId" varchar(255),
    ADD COLUMN IF NOT EXISTS cpf varchar(255),
    ADD COLUMN IF NOT EXISTS destino varchar(255),
    ADD COLUMN IF NOT EXISTS documento varchar(255),
    ADD COLUMN IF NOT EXISTS email varchar(255),
    ADD COLUMN IF NOT EXISTS empresa varchar(255),
    ADD COLUMN IF NOT EXISTS motivo_visita varchar(255),
    ADD COLUMN IF NOT EXISTS nome varchar(255),
    ADD COLUMN IF NOT EXISTS status varchar(255),
    ADD COLUMN IF NOT EXISTS telefone varchar(255),
    ADD COLUMN IF NOT EXISTS tipo_visita varchar(255);

-- -----------------------------------------------------------------------------
-- Chaves estrangeiras
--
-- Ficam depois de todas as tabelas porque há referência circular (moradores e
-- apartamentos apontam um para o outro), e passam por uma função porque o
-- banco pode já existir.
--
-- Num banco existente a chave costuma estar lá, criada pelo Hibernate com o
-- mesmo nome ou por um script antigo com outro nome: nesse caso nada acontece.
-- Se ela faltar, entra como NOT VALID e só depois é validada. Se houver linha
-- órfã, a validação falha, a chave passa a valer só para linhas novas e o
-- aviso fica no log, em vez de impedir o serviço de subir.
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION pg_temp.garantir_fk(tabela regclass, nome text, definicao text) RETURNS void AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_constraint
                WHERE conrelid = tabela AND contype = 'f'
                  AND (conname = nome OR pg_get_constraintdef(oid) = definicao)) THEN
        RETURN;
    END IF;
    EXECUTE format('ALTER TABLE %s ADD CONSTRAINT %I %s NOT VALID', tabela, nome, definicao);
    BEGIN
        EXECUTE format('ALTER TABLE %s VALIDATE CONSTRAINT %I', tabela, nome);
    EXCEPTION WHEN foreign_key_violation THEN
        RAISE WARNING '%: há linhas órfãs; a chave vale só para as linhas novas', nome;
    END;
END
$$ LANGUAGE plpgsql;

SELECT pg_temp.garantir_fk('alugueis_vagas', 'fkxeqmxjgy48unvbuf3sr29k5l', 'FOREIGN KEY (vaga_id) REFERENCES vagas_estacionamento(id)');
SELECT pg_temp.garantir_fk('apartamentos', 'fk5682b3rhn8envteyjp2hikom0', 'FOREIGN KEY (morador_responsavel_id) REFERENCES moradores(id)');
SELECT pg_temp.garantir_fk('apartamentos', 'fks7i9rk405w15ocse7hq3joevc', 'FOREIGN KEY ("blocoId") REFERENCES blocos(id)');
SELECT pg_temp.garantir_fk('disponibilidade_vagas', 'fkhme36tjtw07pbfgi9gk8gpoas', 'FOREIGN KEY (vaga_id) REFERENCES vagas_estacionamento(id)');
SELECT pg_temp.garantir_fk('moradores', 'fkf2vps04qwb8pkhkvlysie8tbi', 'FOREIGN KEY (bloco_id) REFERENCES blocos(id)');
SELECT pg_temp.garantir_fk('moradores', 'fkkhmmor8lsiitkpr52f7rtgd6b', 'FOREIGN KEY (apartamento_id) REFERENCES apartamentos(id)');
SELECT pg_temp.garantir_fk('reservas', 'fk8k0t0d9jxu77vdas5vm2dvkd9', 'FOREIGN KEY (area_comum_id) REFERENCES areas_comuns(id)');
SELECT pg_temp.garantir_fk('turno_entradas', 'fk485my5oq6cl7y5umqhmm86jls', 'FOREIGN KEY (turno_id) REFERENCES turnos(id)');
SELECT pg_temp.garantir_fk('turno_saidas', 'fkl7pw37nsw3if4x5qhnisti3s1', 'FOREIGN KEY (turno_id) REFERENCES turnos(id)');
SELECT pg_temp.garantir_fk('vagas_estacionamento', 'fk26w06r5dkvt9uk6bbydj5dq9', 'FOREIGN KEY (apartamento_id) REFERENCES apartamentos(id)');
SELECT pg_temp.garantir_fk('veiculos', 'fk1wdk7vxuo1kjsqrjymbrr7hb0', 'FOREIGN KEY (vaga_id) REFERENCES vagas_estacionamento(id)');
SELECT pg_temp.garantir_fk('visitantes', 'fk7h6dd199xri0vi97vhb8xoo5o', 'FOREIGN KEY (apartamento_id) REFERENCES apartamentos(id)');
SELECT pg_temp.garantir_fk('visitantes', 'fkhhjo0puets40juq038q87nmdq', 'FOREIGN KEY (bloco_id) REFERENCES blocos(id)');
