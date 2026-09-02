-- Up Migration

CREATE TABLE multas (
  id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  condominio_id  VARCHAR(50) NOT NULL,
  unidade_id     UUID        NOT NULL,
  -- Quem recebeu a multa. Pode ser nulo quando a infração é da unidade e não
  -- de uma pessoa identificada.
  usuario_id     INTEGER,
  valor_centavos BIGINT      NOT NULL CHECK (valor_centavos >= 0),
  motivo         TEXT        NOT NULL,
  data_infracao  DATE        NOT NULL,
  status         VARCHAR(20) NOT NULL DEFAULT 'APLICADA'
                 CHECK (status IN ('APLICADA', 'EM_RECURSO', 'CANCELADA')),
  -- Sem FK: o ocorrencias-service ainda não existe. Quando existir, o vínculo
  -- continua sendo por id, porque estará em outro banco.
  ocorrencia_id  UUID,
  aplicada_por_id INTEGER    NOT NULL,

  recurso_texto  TEXT,
  recurso_em     TIMESTAMPTZ,
  julgado_por_id INTEGER,
  julgado_em     TIMESTAMPTZ,
  julgamento_justificativa TEXT,

  -- Preenchido quando a multa entra numa fatura. Enquanto for nulo, ela ainda
  -- não foi cobrada — é o que impede cobrá-la duas vezes no fechamento.
  fatura_item_id UUID REFERENCES fatura_itens(id) ON DELETE SET NULL,

  criado_em      TIMESTAMPTZ NOT NULL DEFAULT now(),
  atualizado_em  TIMESTAMPTZ NOT NULL DEFAULT now(),

  CONSTRAINT ck_multa_recurso CHECK (
    status <> 'EM_RECURSO' OR (recurso_texto IS NOT NULL AND recurso_em IS NOT NULL)
  )
);

CREATE INDEX idx_multas_condominio ON multas (condominio_id, status);
CREATE INDEX idx_multas_unidade ON multas (condominio_id, unidade_id);
-- As que o próximo fechamento precisa recolher.
CREATE INDEX idx_multas_a_faturar ON multas (condominio_id, unidade_id)
  WHERE status = 'APLICADA' AND fatura_item_id IS NULL;

-- Down Migration
DROP TABLE IF EXISTS multas;
