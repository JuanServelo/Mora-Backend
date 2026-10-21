-- =============================================================================
-- V1: estrutura inicial do comunicacao-service (banco `mora`)
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
-- Idempotente de propósito (IF NOT EXISTS): o banco já existe nas máquinas de
-- quem roda o projeto, e o `mora` é dividido com o portaria-service. Lá esta
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

CREATE TABLE IF NOT EXISTS artigo_avaliacoes (
    id uuid NOT NULL,
    util boolean NOT NULL,
    atualizado_em timestamp(6),
    criado_em timestamp(6),
    artigo_id uuid NOT NULL,
    usuario_id varchar(64) NOT NULL,
    CONSTRAINT artigo_avaliacoes_pkey PRIMARY KEY (id),
    CONSTRAINT artigo_avaliacoes_artigo_id_usuario_id_key UNIQUE (artigo_id, usuario_id)
);
ALTER TABLE artigo_avaliacoes
    ADD COLUMN IF NOT EXISTS util boolean,
    ADD COLUMN IF NOT EXISTS atualizado_em timestamp(6),
    ADD COLUMN IF NOT EXISTS criado_em timestamp(6),
    ADD COLUMN IF NOT EXISTS artigo_id uuid,
    ADD COLUMN IF NOT EXISTS usuario_id varchar(64);

CREATE TABLE IF NOT EXISTS artigos_conhecimento (
    id uuid NOT NULL,
    publicado boolean NOT NULL,
    atualizado_em timestamp(6),
    criado_em timestamp(6),
    autor varchar(255),
    categoria varchar(255) NOT NULL,
    "condominioId" varchar(255),
    conteudo text NOT NULL,
    titulo varchar(255) NOT NULL,
    CONSTRAINT artigos_conhecimento_categoria_check CHECK (categoria IN ('REGRA', 'MANUAL', 'TUTORIAL', 'ORIENTACAO_CONVIVENCIA', 'FAQ')),
    CONSTRAINT artigos_conhecimento_pkey PRIMARY KEY (id)
);
ALTER TABLE artigos_conhecimento
    ADD COLUMN IF NOT EXISTS publicado boolean,
    ADD COLUMN IF NOT EXISTS atualizado_em timestamp(6),
    ADD COLUMN IF NOT EXISTS criado_em timestamp(6),
    ADD COLUMN IF NOT EXISTS autor varchar(255),
    ADD COLUMN IF NOT EXISTS categoria varchar(255),
    ADD COLUMN IF NOT EXISTS "condominioId" varchar(255),
    ADD COLUMN IF NOT EXISTS conteudo text,
    ADD COLUMN IF NOT EXISTS titulo varchar(255);

CREATE TABLE IF NOT EXISTS aviso_leituras (
    id varchar(255) NOT NULL,
    lido_em timestamp(6),
    aviso_id uuid NOT NULL,
    usuario_id varchar(255) NOT NULL,
    CONSTRAINT aviso_leituras_pkey PRIMARY KEY (id),
    CONSTRAINT aviso_leituras_aviso_id_usuario_id_key UNIQUE (aviso_id, usuario_id)
);
ALTER TABLE aviso_leituras
    ADD COLUMN IF NOT EXISTS lido_em timestamp(6),
    ADD COLUMN IF NOT EXISTS aviso_id uuid,
    ADD COLUMN IF NOT EXISTS usuario_id varchar(255);

CREATE TABLE IF NOT EXISTS avisos (
    id uuid NOT NULL,
    data_fim date NOT NULL,
    data_inicio date NOT NULL,
    publicado boolean NOT NULL,
    atualizado_em timestamp(6),
    criado_em timestamp(6),
    autor varchar(255),
    "condominioId" varchar(255),
    imagem_url varchar(255),
    mensagem text NOT NULL,
    publico_alvo varchar(255),
    titulo varchar(255) NOT NULL,
    CONSTRAINT avisos_publico_alvo_check CHECK (publico_alvo IN ('TODOS', 'MORADORES', 'FUNCIONARIOS', 'SINDICO')),
    CONSTRAINT avisos_pkey PRIMARY KEY (id)
);
ALTER TABLE avisos
    ADD COLUMN IF NOT EXISTS data_fim date,
    ADD COLUMN IF NOT EXISTS data_inicio date,
    ADD COLUMN IF NOT EXISTS publicado boolean,
    ADD COLUMN IF NOT EXISTS atualizado_em timestamp(6),
    ADD COLUMN IF NOT EXISTS criado_em timestamp(6),
    ADD COLUMN IF NOT EXISTS autor varchar(255),
    ADD COLUMN IF NOT EXISTS "condominioId" varchar(255),
    ADD COLUMN IF NOT EXISTS imagem_url varchar(255),
    ADD COLUMN IF NOT EXISTS mensagem text,
    ADD COLUMN IF NOT EXISTS publico_alvo varchar(255),
    ADD COLUMN IF NOT EXISTS titulo varchar(255);

CREATE TABLE IF NOT EXISTS chat_mensagens (
    id varchar(255) NOT NULL,
    lida boolean NOT NULL,
    enviado_em timestamp(6),
    lida_em timestamp(6),
    "condominioId" varchar(255),
    destinatario_id varchar(255),
    remetente_id varchar(255) NOT NULL,
    texto text NOT NULL,
    CONSTRAINT chat_mensagens_pkey PRIMARY KEY (id)
);
ALTER TABLE chat_mensagens
    ADD COLUMN IF NOT EXISTS lida boolean,
    ADD COLUMN IF NOT EXISTS enviado_em timestamp(6),
    ADD COLUMN IF NOT EXISTS lida_em timestamp(6),
    ADD COLUMN IF NOT EXISTS "condominioId" varchar(255),
    ADD COLUMN IF NOT EXISTS destinatario_id varchar(255),
    ADD COLUMN IF NOT EXISTS remetente_id varchar(255),
    ADD COLUMN IF NOT EXISTS texto text;

CREATE TABLE IF NOT EXISTS conversa_participantes (
    id varchar(255) NOT NULL,
    conversa_id bigint NOT NULL,
    entrou_em timestamp(6) NOT NULL,
    ultima_leitura_em timestamp(6),
    usuario_id varchar(64) NOT NULL,
    CONSTRAINT conversa_participantes_pkey PRIMARY KEY (id),
    CONSTRAINT conversa_participantes_conversa_id_usuario_id_key UNIQUE (conversa_id, usuario_id)
);
ALTER TABLE conversa_participantes
    ADD COLUMN IF NOT EXISTS conversa_id bigint,
    ADD COLUMN IF NOT EXISTS entrou_em timestamp(6),
    ADD COLUMN IF NOT EXISTS ultima_leitura_em timestamp(6),
    ADD COLUMN IF NOT EXISTS usuario_id varchar(64);

CREATE TABLE IF NOT EXISTS conversas (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    criada_em timestamp(6) NOT NULL,
    encerrada_em timestamp(6),
    ultima_mensagem_em timestamp(6),
    tipo varchar(20) NOT NULL,
    condominio_id varchar(50) NOT NULL,
    criada_por varchar(64) NOT NULL,
    assunto varchar(150) NOT NULL,
    CONSTRAINT conversas_tipo_check CHECK (tipo IN ('DIRETA', 'ADMINISTRACAO')),
    CONSTRAINT conversas_pkey PRIMARY KEY (id)
);
ALTER TABLE conversas
    ADD COLUMN IF NOT EXISTS criada_em timestamp(6),
    ADD COLUMN IF NOT EXISTS encerrada_em timestamp(6),
    ADD COLUMN IF NOT EXISTS ultima_mensagem_em timestamp(6),
    ADD COLUMN IF NOT EXISTS tipo varchar(20),
    ADD COLUMN IF NOT EXISTS condominio_id varchar(50),
    ADD COLUMN IF NOT EXISTS criada_por varchar(64),
    ADD COLUMN IF NOT EXISTS assunto varchar(150);

CREATE TABLE IF NOT EXISTS faq_perguntas (
    id uuid NOT NULL,
    criado_em timestamp(6),
    respondida_em timestamp(6),
    artigo_id uuid,
    status varchar(20) NOT NULL,
    autor_id varchar(64) NOT NULL,
    autor_nome varchar(255),
    categoria varchar(255),
    "condominioId" varchar(255),
    respondida_por varchar(255),
    resposta text,
    texto text NOT NULL,
    CONSTRAINT faq_perguntas_categoria_check CHECK (categoria IN ('REGRA', 'MANUAL', 'TUTORIAL', 'ORIENTACAO_CONVIVENCIA', 'FAQ')),
    CONSTRAINT faq_perguntas_status_check CHECK (status IN ('PENDENTE', 'RESPONDIDA')),
    CONSTRAINT faq_perguntas_pkey PRIMARY KEY (id)
);
ALTER TABLE faq_perguntas
    ADD COLUMN IF NOT EXISTS criado_em timestamp(6),
    ADD COLUMN IF NOT EXISTS respondida_em timestamp(6),
    ADD COLUMN IF NOT EXISTS artigo_id uuid,
    ADD COLUMN IF NOT EXISTS status varchar(20),
    ADD COLUMN IF NOT EXISTS autor_id varchar(64),
    ADD COLUMN IF NOT EXISTS autor_nome varchar(255),
    ADD COLUMN IF NOT EXISTS categoria varchar(255),
    ADD COLUMN IF NOT EXISTS "condominioId" varchar(255),
    ADD COLUMN IF NOT EXISTS respondida_por varchar(255),
    ADD COLUMN IF NOT EXISTS resposta text,
    ADD COLUMN IF NOT EXISTS texto text;

CREATE TABLE IF NOT EXISTS mensagens (
    id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    conversa_id bigint NOT NULL,
    criada_em timestamp(6) NOT NULL,
    removida_em timestamp(6),
    autor_perfil varchar(20) NOT NULL,
    autor_id varchar(64) NOT NULL,
    corpo text NOT NULL,
    CONSTRAINT mensagens_pkey PRIMARY KEY (id)
);
ALTER TABLE mensagens
    ADD COLUMN IF NOT EXISTS conversa_id bigint,
    ADD COLUMN IF NOT EXISTS criada_em timestamp(6),
    ADD COLUMN IF NOT EXISTS removida_em timestamp(6),
    ADD COLUMN IF NOT EXISTS autor_perfil varchar(20),
    ADD COLUMN IF NOT EXISTS autor_id varchar(64),
    ADD COLUMN IF NOT EXISTS corpo text;

CREATE TABLE IF NOT EXISTS notificacoes (
    id varchar(255) NOT NULL,
    lida boolean NOT NULL,
    criado_em timestamp(6),
    lida_em timestamp(6),
    origem varchar(30),
    chave_unica varchar(200),
    "condominioId" varchar(255),
    destinatario_id varchar(255) NOT NULL,
    mensagem text,
    referencia_id varchar(255),
    tipo varchar(255),
    titulo varchar(255) NOT NULL,
    dados jsonb,
    CONSTRAINT notificacoes_tipo_check CHECK (tipo IN ('AVISO', 'ENTREGA', 'RESERVA', 'ALUGUEL_VAGA', 'PRE_AUTORIZACAO', 'CHAT', 'SISTEMA', 'NOVA_FATURA', 'FATURA_VENCIDA', 'PAGAMENTO_CONFIRMADO', 'MULTA_APLICADA', 'MULTA_JULGADA')),
    CONSTRAINT notificacoes_pkey PRIMARY KEY (id)
);
ALTER TABLE notificacoes
    ADD COLUMN IF NOT EXISTS lida boolean,
    ADD COLUMN IF NOT EXISTS criado_em timestamp(6),
    ADD COLUMN IF NOT EXISTS lida_em timestamp(6),
    ADD COLUMN IF NOT EXISTS origem varchar(30),
    ADD COLUMN IF NOT EXISTS chave_unica varchar(200),
    ADD COLUMN IF NOT EXISTS "condominioId" varchar(255),
    ADD COLUMN IF NOT EXISTS destinatario_id varchar(255),
    ADD COLUMN IF NOT EXISTS mensagem text,
    ADD COLUMN IF NOT EXISTS referencia_id varchar(255),
    ADD COLUMN IF NOT EXISTS tipo varchar(255),
    ADD COLUMN IF NOT EXISTS titulo varchar(255),
    ADD COLUMN IF NOT EXISTS dados jsonb;
