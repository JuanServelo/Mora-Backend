-- Up Migration

-- Caixa de entrada única do usuário.
--
-- Centraliza o que hoje cada serviço guardaria por conta própria: o
-- `financeiro` já tem uma tabela igual, com notificações de fatura emitida e
-- vencida. Elas passam a ser publicadas aqui pela rota interna.
--
-- Convenções deste banco:
--   * `condominio_id` VARCHAR(50), o mesmo tipo de condominios.id no auth_db
--   * `usuario_id` INTEGER, o mesmo de users.id
--   * Sem FK entre bancos: cada serviço tem o seu, e a integridade
--     referencial que atravessa serviço é responsabilidade da aplicação

CREATE TABLE notificacoes (
  id            BIGSERIAL PRIMARY KEY,
  condominio_id VARCHAR(50)  NOT NULL,
  usuario_id    INTEGER      NOT NULL,
  -- Quem publicou: 'financeiro', 'portaria', 'comunicacao'. Guardado para a
  -- tela poder agrupar e para saber de onde veio quando algo parecer errado.
  origem        VARCHAR(30)  NOT NULL,
  tipo          VARCHAR(40)  NOT NULL,
  titulo        VARCHAR(150) NOT NULL,
  mensagem      TEXT         NOT NULL,
  -- Dados livres do evento: faturaId, avisoId, o que o publicador precisar
  -- para a tela montar um link de volta.
  dados         JSONB        NOT NULL DEFAULT '{}'::jsonb,
  lida_em       TIMESTAMPTZ,
  criado_em     TIMESTAMPTZ  NOT NULL DEFAULT now(),

  -- Chave do publicador, para a mesma notificação não entrar duas vezes quando
  -- um job reprocessa. Nula quando o publicador não tem como gerar uma.
  chave_unica   VARCHAR(160)
);

CREATE INDEX idx_notificacoes_caixa ON notificacoes (usuario_id, criado_em DESC);
-- Sustenta o contador de não lidas, que a tela pede em toda navegação.
CREATE INDEX idx_notificacoes_nao_lidas ON notificacoes (usuario_id)
  WHERE lida_em IS NULL;
CREATE UNIQUE INDEX uq_notificacao_chave ON notificacoes (chave_unica)
  WHERE chave_unica IS NOT NULL;

-- Down Migration
DROP TABLE IF EXISTS notificacoes;
