-- =============================================================================
-- FASE 5: tabelas do comunicacao-service
--
-- O Hibernate cria as tabelas novas sozinho (ddl-auto=update), mas nao altera
-- o tipo de coluna de uma tabela que ja existe. Este script cobre os dois
-- casos: cria do zero e conserta o que ficou com o tipo antigo.
--
-- `avisos` e `artigos_conhecimento` ja existem no banco `mora`, criadas pelo
-- portaria-service antes da divisao de dominios.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Leitura de avisos (RF-12)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS aviso_leituras (
    id          VARCHAR(36) PRIMARY KEY,
    aviso_id    UUID        NOT NULL,
    usuario_id  VARCHAR(64) NOT NULL,
    lido_em     TIMESTAMP   DEFAULT NOW(),
    UNIQUE (aviso_id, usuario_id)
);

CREATE INDEX IF NOT EXISTS idx_aviso_leituras_aviso   ON aviso_leituras (aviso_id);
CREATE INDEX IF NOT EXISTS idx_aviso_leituras_usuario ON aviso_leituras (usuario_id);

-- -----------------------------------------------------------------------------
-- Notificacoes internas (RF-13)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS notificacoes (
    id              VARCHAR(36)  PRIMARY KEY,
    destinatario_id VARCHAR(64)  NOT NULL,
    "condominioId"  VARCHAR(255),
    tipo            VARCHAR(30),
    titulo          VARCHAR(255) NOT NULL,
    mensagem        TEXT,
    referencia_id   VARCHAR(36),
    lida            BOOLEAN      NOT NULL DEFAULT FALSE,
    criado_em       TIMESTAMP    DEFAULT NOW(),
    lida_em         TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notificacoes_destinatario ON notificacoes (destinatario_id);
CREATE INDEX IF NOT EXISTS idx_notificacoes_lida         ON notificacoes (destinatario_id, lida);

-- -----------------------------------------------------------------------------
-- Categorias que o usuario optou por nao receber (RF-13)
--
-- Guardamos so as recusas: sem linha o usuario recebe tudo, entao conta nova
-- ja nasce configurada sem precisar de carga inicial.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS preferencias_notificacao (
    id         VARCHAR(36) PRIMARY KEY,
    usuario_id VARCHAR(64) NOT NULL,
    tipo       VARCHAR(30) NOT NULL,
    UNIQUE (usuario_id, tipo)
);

CREATE INDEX IF NOT EXISTS idx_preferencias_usuario ON preferencias_notificacao (usuario_id);

-- -----------------------------------------------------------------------------
-- Chat morador <-> administracao (RF-13)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS chat_mensagens (
    id              VARCHAR(36) PRIMARY KEY,
    remetente_id    VARCHAR(64) NOT NULL,
    destinatario_id VARCHAR(64) NOT NULL,
    "condominioId"  VARCHAR(255),
    texto           TEXT        NOT NULL,
    lida            BOOLEAN     NOT NULL DEFAULT FALSE,
    enviado_em      TIMESTAMP   DEFAULT NOW(),
    lida_em         TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_chat_remetente    ON chat_mensagens (remetente_id);
CREATE INDEX IF NOT EXISTS idx_chat_destinatario ON chat_mensagens (destinatario_id);

-- =============================================================================
-- Correcao de tipo: id de usuario e UUID -> VARCHAR
--
-- A versao anterior destas tabelas declarava o id do usuario como UUID, mas o
-- auth-api usa inteiro autoincremento (`users.id` e INTEGER). Todo endpoint de
-- chat, notificacao e leitura falhava ao converter "42" para UUID.
--
-- O bloco abaixo so age em quem ja subiu a versao antiga; num banco novo as
-- colunas ja nascem VARCHAR e o ALTER e ignorado.
-- =============================================================================
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'aviso_leituras' AND column_name = 'usuario_id'
                 AND data_type = 'uuid') THEN
        ALTER TABLE aviso_leituras ALTER COLUMN usuario_id TYPE VARCHAR(64) USING usuario_id::text;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'notificacoes' AND column_name = 'destinatario_id'
                 AND data_type = 'uuid') THEN
        ALTER TABLE notificacoes ALTER COLUMN destinatario_id TYPE VARCHAR(64) USING destinatario_id::text;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'chat_mensagens' AND column_name = 'remetente_id'
                 AND data_type = 'uuid') THEN
        ALTER TABLE chat_mensagens ALTER COLUMN remetente_id    TYPE VARCHAR(64) USING remetente_id::text;
        ALTER TABLE chat_mensagens ALTER COLUMN destinatario_id TYPE VARCHAR(64) USING destinatario_id::text;
    END IF;
END $$;

-- =============================================================================
-- Os dados de avisos e artigos_conhecimento nao precisam de migracao: o
-- comunicacao-service leu do mesmo banco `mora` desde o inicio. O banco
-- proprio (`mora_comunicacao`) fica criado e vazio, para a separacao futura.
-- =============================================================================
