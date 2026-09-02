-- Up Migration

CREATE TABLE contratos_locacao (
  id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  condominio_id          VARCHAR(50) NOT NULL,
  unidade_id             UUID        NOT NULL,
  proprietario_usuario_id INTEGER    NOT NULL,
  inquilino_usuario_id   INTEGER,
  inicio                 DATE        NOT NULL,
  fim                    DATE,
  valor_aluguel_centavos BIGINT CHECK (valor_aluguel_centavos >= 0),
  -- Quem paga a taxa condominial. É o que o auth-api chama de
  -- `responsavelFinanceiro` na unidade; aqui fica o porquê contratual.
  responsavel_taxas      VARCHAR(20) NOT NULL DEFAULT 'INQUILINO'
                         CHECK (responsavel_taxas IN ('PROPRIETARIO', 'INQUILINO')),
  ativo                  BOOLEAN     NOT NULL DEFAULT true,
  criado_em              TIMESTAMPTZ NOT NULL DEFAULT now(),
  atualizado_em          TIMESTAMPTZ NOT NULL DEFAULT now(),

  CONSTRAINT ck_contrato_vigencia CHECK (fim IS NULL OR fim >= inicio)
);

CREATE INDEX idx_contratos_unidade ON contratos_locacao (condominio_id, unidade_id);
-- Uma unidade não pode ter dois contratos vigentes ao mesmo tempo. Índice
-- parcial porque contratos encerrados coexistem à vontade no histórico.
CREATE UNIQUE INDEX uq_contrato_ativo ON contratos_locacao (condominio_id, unidade_id)
  WHERE ativo;

CREATE TABLE lancamentos (
  id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  condominio_id    VARCHAR(50) NOT NULL,
  competencia      DATE        NOT NULL CHECK (EXTRACT(DAY FROM competencia) = 1),
  tipo             VARCHAR(10) NOT NULL CHECK (tipo IN ('RECEITA', 'DESPESA')),
  categoria        VARCHAR(80) NOT NULL,
  descricao        VARCHAR(300) NOT NULL,
  valor_centavos   BIGINT      NOT NULL CHECK (valor_centavos >= 0),
  data_lancamento  DATE        NOT NULL,
  comprovante_url  TEXT,
  criado_por_id    INTEGER     NOT NULL,
  criado_em        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_lancamentos_competencia ON lancamentos (condominio_id, competencia);

CREATE TABLE prestacao_contas (
  condominio_id  VARCHAR(50) NOT NULL,
  competencia    DATE        NOT NULL CHECK (EXTRACT(DAY FROM competencia) = 1),
  status         VARCHAR(20) NOT NULL DEFAULT 'RASCUNHO'
                 CHECK (status IN ('RASCUNHO', 'PUBLICADA')),
  publicado_em   TIMESTAMPTZ,
  publicado_por_id INTEGER,
  criado_em      TIMESTAMPTZ NOT NULL DEFAULT now(),
  atualizado_em  TIMESTAMPTZ NOT NULL DEFAULT now(),

  PRIMARY KEY (condominio_id, competencia),

  CONSTRAINT ck_prestacao_publicada CHECK (
    status <> 'PUBLICADA' OR (publicado_em IS NOT NULL AND publicado_por_id IS NOT NULL)
  )
);

-- Down Migration
DROP TABLE IF EXISTS prestacao_contas;
DROP TABLE IF EXISTS lancamentos;
DROP TABLE IF EXISTS contratos_locacao;
