-- =============================================================================
-- FASE 7: Conversas (RF-13)
--
-- O servico ja tinha `chat_mensagens`: mensagem direta entre dois usuarios,
-- identificados por id. Isso nao cobre o caso real do produto — o morador fala
-- com *a administracao*, nao com uma pessoa.
--
-- Por que o formato ADMINISTRACAO importa, e nao e cosmetica:
--
--   1. Ninguem quer descobrir o nome do sindico para relatar um vazamento, e a
--      conversa precisa sobreviver a troca de sindico.
--   2. O morador NAO ENXERGA o sindico em lista nenhuma. O auth-api aceita
--      /api/user-management/users para ele, mas recorta o resultado na unidade
--      dele: pedindo a lista, recebe a si mesmo e os ocupantes do proprio
--      apartamento. Sem este formato nao haveria destinatario a escolher.
--
-- `chat_mensagens` continua existindo e o ChatController segue atendendo quem
-- ja o usa. As tabelas abaixo convivem com ele.
-- =============================================================================

CREATE TABLE IF NOT EXISTS conversas (
    id                 BIGSERIAL PRIMARY KEY,
    condominio_id      VARCHAR(50)  NOT NULL,
    tipo               VARCHAR(20)  NOT NULL
                       CHECK (tipo IN ('DIRETA', 'ADMINISTRACAO')),
    assunto            VARCHAR(150) NOT NULL,
    -- VARCHAR e nao UUID: o auth-api numera usuarios com `integer`. Ver
    -- fase6-id-usuario-varchar.sql.
    criada_por         VARCHAR(64)  NOT NULL,
    -- Denormalizado de proposito: a lista ordena por isto, e sem a coluna toda
    -- listagem viraria um JOIN com MAX() sobre mensagens.
    ultima_mensagem_em TIMESTAMP,
    encerrada_em       TIMESTAMP,
    criada_em          TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_conversas_condominio
    ON conversas (condominio_id, ultima_mensagem_em DESC NULLS LAST);

-- Sustenta a caixa da gestao: conversas com a administracao, abertas primeiro.
CREATE INDEX IF NOT EXISTS idx_conversas_administracao
    ON conversas (condominio_id, ultima_mensagem_em DESC NULLS LAST)
 WHERE tipo = 'ADMINISTRACAO';

CREATE TABLE IF NOT EXISTS conversa_participantes (
    id                VARCHAR(36) PRIMARY KEY,
    conversa_id       BIGINT      NOT NULL REFERENCES conversas (id) ON DELETE CASCADE,
    usuario_id        VARCHAR(64) NOT NULL,
    -- Marca d'agua da leitura. A contagem de nao lidas e
    -- `mensagens.criada_em > ultima_leitura_em`, sem linha por mensagem lida.
    -- Para quem ainda nao e participante nao ha marca, e tudo conta como nao
    -- lido — que e o correto para a gestao que nunca respondeu.
    ultima_leitura_em TIMESTAMP,
    entrou_em         TIMESTAMP   NOT NULL DEFAULT NOW(),
    UNIQUE (conversa_id, usuario_id)
);

-- "Minhas conversas" parte daqui, entao o indice e pelo usuario.
CREATE INDEX IF NOT EXISTS idx_participantes_usuario
    ON conversa_participantes (usuario_id);

CREATE TABLE IF NOT EXISTS mensagens (
    id           BIGSERIAL PRIMARY KEY,
    conversa_id  BIGINT      NOT NULL REFERENCES conversas (id) ON DELETE CASCADE,
    autor_id     VARCHAR(64) NOT NULL,
    -- Guardado junto porque o autor pode deixar a gestao depois de escrever, e
    -- a mensagem tem de continuar dizendo em que papel foi escrita.
    autor_perfil VARCHAR(20) NOT NULL,
    corpo        TEXT        NOT NULL CHECK (length(btrim(corpo)) > 0),
    -- Apagar de verdade destruiria o registro da conversa para o outro lado.
    removida_em  TIMESTAMP,
    criada_em    TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_mensagens_conversa
    ON mensagens (conversa_id, criada_em);
