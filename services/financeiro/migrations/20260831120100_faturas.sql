-- Up Migration

CREATE TABLE faturas (
  id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  condominio_id        VARCHAR(50) NOT NULL,
  unidade_id           UUID        NOT NULL,
  -- Sempre o dia 1 do mês de referência: guardar como DATE deixa a ordenação e
  -- a comparação a cargo do banco, o que 'YYYY-MM' em texto não dá de graça.
  competencia          DATE        NOT NULL CHECK (EXTRACT(DAY FROM competencia) = 1),
  vencimento           DATE        NOT NULL,
  valor_centavos       BIGINT      NOT NULL CHECK (valor_centavos >= 0),
  status               VARCHAR(20) NOT NULL DEFAULT 'ABERTA'
                       CHECK (status IN ('ABERTA', 'PAGA', 'EM_ATRASO', 'CANCELADA')),
  -- Quem responde por esta fatura no momento da emissão. Guardado aqui, e não
  -- resolvido na leitura, porque o responsável da unidade muda com o tempo e a
  -- fatura emitida é registro do que valia naquele mês.
  responsavel_usuario_id INTEGER,
  pago_em              TIMESTAMPTZ,
  valor_pago_centavos  BIGINT CHECK (valor_pago_centavos >= 0),
  forma_baixa          VARCHAR(20) CHECK (forma_baixa IN ('GATEWAY', 'MANUAL')),
  criado_em            TIMESTAMPTZ NOT NULL DEFAULT now(),
  atualizado_em        TIMESTAMPTZ NOT NULL DEFAULT now(),

  -- Fatura paga sem data ou sem valor é registro pela metade.
  CONSTRAINT ck_fatura_paga CHECK (
    status <> 'PAGA' OR (pago_em IS NOT NULL AND valor_pago_centavos IS NOT NULL)
  )
);

-- A defesa contra o mês fechar duas vezes: se o job rodar de novo — container
-- reiniciado no horário, duas instâncias, disparo manual em cima do agendado —
-- o banco recusa a segunda fatura.
--
-- Parcial, e não UNIQUE de tabela, porque cancelar precisa liberar a
-- competência para reemissão. Um UNIQUE total prenderia a unidade a uma única
-- tentativa para sempre: erra o valor, cancela, e não consegue refaturar o mês.
-- É o mesmo defeito que `UNIQUE (condominio_id, status)` causou em
-- tb_assinaturas no plan-service, onde impede o histórico de existir.
CREATE UNIQUE INDEX uq_fatura_competencia ON faturas (condominio_id, unidade_id, competencia)
  WHERE status <> 'CANCELADA';

CREATE INDEX idx_faturas_condominio_competencia ON faturas (condominio_id, competencia);
CREATE INDEX idx_faturas_unidade ON faturas (condominio_id, unidade_id);
-- Sustenta o indicador de inadimplência e o job que marca atraso.
CREATE INDEX idx_faturas_em_aberto ON faturas (condominio_id, vencimento)
  WHERE status IN ('ABERTA', 'EM_ATRASO');

CREATE TABLE fatura_itens (
  id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  fatura_id      UUID        NOT NULL REFERENCES faturas(id) ON DELETE CASCADE,
  tipo           VARCHAR(20) NOT NULL
                 CHECK (tipo IN ('TAXA', 'MULTA', 'RESERVA', 'AVULSO', 'ESTORNO')),
  descricao      VARCHAR(200) NOT NULL,
  valor_centavos BIGINT      NOT NULL,
  -- Aponta para a origem (tipo_taxa, multa, reserva) sem FK: nem toda origem
  -- mora neste banco, e a de reserva ainda não existe em lugar nenhum.
  origem_id      UUID,
  criado_em      TIMESTAMPTZ NOT NULL DEFAULT now(),

  -- Estorno é o único item que abate. Uma multa cancelada depois de faturada
  -- vira estorno; apagar o item original reescreveria uma fatura já emitida.
  CONSTRAINT ck_item_sinal CHECK (
    (tipo = 'ESTORNO' AND valor_centavos <= 0) OR
    (tipo <> 'ESTORNO' AND valor_centavos >= 0)
  )
);

CREATE INDEX idx_fatura_itens_fatura ON fatura_itens (fatura_id);

-- Down Migration
DROP TABLE IF EXISTS fatura_itens;
DROP TABLE IF EXISTS faturas;
