-- =============================================================================
-- FASE 6: Avaliação de artigos e perguntas da FAQ
-- Criadas automaticamente pelo Spring ddl-auto=update ao subir o serviço.
-- Os ids de usuário são texto: no auth-api eles são inteiros, não UUID.
-- =============================================================================

-- "Essa resposta ajudou?" — um voto por usuário em cada artigo
CREATE TABLE IF NOT EXISTS artigo_avaliacoes (
    id             UUID         PRIMARY KEY,
    artigo_id      UUID         NOT NULL,
    usuario_id     VARCHAR(64)  NOT NULL,
    util           BOOLEAN      NOT NULL,
    criado_em      TIMESTAMP    DEFAULT NOW(),
    atualizado_em  TIMESTAMP    DEFAULT NOW(),
    UNIQUE (artigo_id, usuario_id)
);

CREATE INDEX IF NOT EXISTS idx_artigo_avaliacoes_artigo ON artigo_avaliacoes (artigo_id);

-- Dúvidas que o morador não encontrou na FAQ
CREATE TABLE IF NOT EXISTS faq_perguntas (
    id              UUID          PRIMARY KEY,
    "condominioId"  VARCHAR(255),
    autor_id        VARCHAR(64)   NOT NULL,
    autor_nome      VARCHAR(255),
    texto           TEXT          NOT NULL,
    categoria       VARCHAR(30),
    status          VARCHAR(20)   NOT NULL DEFAULT 'PENDENTE',
    resposta        TEXT,
    respondida_por  VARCHAR(255),
    respondida_em   TIMESTAMP,
    artigo_id       UUID,
    criado_em       TIMESTAMP     DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_faq_perguntas_condominio ON faq_perguntas ("condominioId", status);
CREATE INDEX IF NOT EXISTS idx_faq_perguntas_autor      ON faq_perguntas (autor_id);
