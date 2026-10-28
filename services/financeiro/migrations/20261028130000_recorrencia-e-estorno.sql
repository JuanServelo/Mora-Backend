-- Up Migration

-- Cobrança recorrente (assinatura no Asaas) e estorno de pagamento.
--
-- Estorno aqui é devolver dinheiro que o gateway recebeu, pelo próprio
-- gateway. É outra coisa que o ESTORNO de `fatura_itens`, que é crédito de
-- multa cancelada na fatura seguinte.

-- ── 1. Fatura estornada ────────────────────────────────────────────────────
--
-- Paga e estornada é um estado próprio, e não "cancelada": o dinheiro entrou e
-- voltou, e a fatura guarda as duas coisas. Como a cancelada, ela deixa de
-- contar e libera a competência para reemissão — o motivo comum do estorno é
-- cobrança errada, que precisa sair de novo com o valor certo.
ALTER TABLE faturas DROP CONSTRAINT faturas_status_check;
ALTER TABLE faturas ADD CONSTRAINT faturas_status_check
  CHECK (status IN ('ABERTA', 'PAGA', 'EM_ATRASO', 'CANCELADA', 'ESTORNADA'));

ALTER TABLE faturas
  ADD COLUMN estornado_em   TIMESTAMPTZ,
  ADD COLUMN estorno_motivo VARCHAR(300);

ALTER TABLE faturas ADD CONSTRAINT ck_fatura_estornada CHECK (
  status <> 'ESTORNADA' OR (pago_em IS NOT NULL AND estornado_em IS NOT NULL)
);

DROP INDEX uq_fatura_competencia;
CREATE UNIQUE INDEX uq_fatura_competencia ON faturas (condominio_id, unidade_id, competencia)
  WHERE status NOT IN ('CANCELADA', 'ESTORNADA');

-- O gatilho de fatura encerrada ganha uma única saída para a paga: o estorno.
-- Ele só pode mudar o status e preencher os campos do estorno; valor, baixa e
-- competência continuam intocáveis. Estornada também é estado final.
CREATE OR REPLACE FUNCTION proteger_fatura_encerrada()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  IF TG_OP = 'DELETE' THEN
    IF OLD.status IN ('PAGA', 'ESTORNADA') THEN
      RAISE EXCEPTION 'A fatura % já foi paga e não pode ser apagada.', OLD.id
        USING ERRCODE = 'restrict_violation';
    END IF;
    RETURN OLD;
  END IF;

  IF OLD.status IN ('PAGA', 'ESTORNADA') AND
     (NEW.valor_centavos, NEW.valor_pago_centavos, NEW.pago_em, NEW.forma_baixa,
      NEW.competencia, NEW.unidade_id, NEW.condominio_id)
     IS DISTINCT FROM
     (OLD.valor_centavos, OLD.valor_pago_centavos, OLD.pago_em, OLD.forma_baixa,
      OLD.competencia, OLD.unidade_id, OLD.condominio_id)
  THEN
    RAISE EXCEPTION 'A fatura % já foi paga e não pode ser alterada.', OLD.id
      USING ERRCODE = 'restrict_violation';
  END IF;

  IF OLD.status = 'PAGA' AND NEW.status NOT IN ('PAGA', 'ESTORNADA') THEN
    RAISE EXCEPTION 'A fatura % já foi paga; só um estorno muda o status dela.', OLD.id
      USING ERRCODE = 'restrict_violation';
  END IF;

  IF OLD.status = 'ESTORNADA' AND
     (NEW.status, NEW.estornado_em, NEW.estorno_motivo)
     IS DISTINCT FROM (OLD.status, OLD.estornado_em, OLD.estorno_motivo)
  THEN
    RAISE EXCEPTION 'A fatura % já foi estornada.', OLD.id
      USING ERRCODE = 'restrict_violation';
  END IF;

  IF OLD.status = 'CANCELADA' AND NEW.status <> 'CANCELADA' THEN
    RAISE EXCEPTION 'A fatura % foi cancelada; emita outra para a competência.', OLD.id
      USING ERRCODE = 'restrict_violation';
  END IF;

  RETURN NEW;
END
$$;

-- Os itens também ficam congelados na estornada: é o registro do que foi
-- cobrado e devolvido.
CREATE OR REPLACE FUNCTION proteger_itens_de_fatura_paga()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
  v_fatura UUID := CASE WHEN TG_OP = 'INSERT' THEN NEW.fatura_id ELSE OLD.fatura_id END;
BEGIN
  IF EXISTS (SELECT 1 FROM faturas WHERE id = v_fatura AND status IN ('PAGA', 'ESTORNADA')) THEN
    RAISE EXCEPTION 'A fatura % já foi paga; os itens dela não mudam.', v_fatura
      USING ERRCODE = 'restrict_violation';
  END IF;
  RETURN CASE WHEN TG_OP = 'DELETE' THEN OLD ELSE NEW END;
END
$$;

-- Estornada sai dos totais do painel, como a cancelada: não foi recebida nem é
-- devida.
CREATE OR REPLACE FUNCTION kpis_faturas_por_mes(p_condominio_id VARCHAR, p_meses INTEGER)
RETURNS TABLE (
  mes            TEXT,
  total          INTEGER,
  total_centavos BIGINT,
  pagas          INTEGER,
  pagas_centavos BIGINT,
  abertas        INTEGER,
  em_atraso      INTEGER
)
LANGUAGE sql STABLE
AS $$
  WITH janela AS (
    SELECT generate_series(
             date_trunc('month', (now() AT TIME ZONE 'America/Sao_Paulo')::date)
               - make_interval(months => GREATEST(p_meses, 1) - 1),
             date_trunc('month', (now() AT TIME ZONE 'America/Sao_Paulo')::date),
             interval '1 month'
           )::date AS competencia
  )
  SELECT to_char(j.competencia, 'YYYY-MM'),
         (COUNT(f.id) FILTER (WHERE f.status NOT IN ('CANCELADA', 'ESTORNADA')))::int,
         COALESCE(SUM(f.valor_centavos) FILTER (WHERE f.status NOT IN ('CANCELADA', 'ESTORNADA')), 0)::bigint,
         (COUNT(f.id) FILTER (WHERE f.status = 'PAGA'))::int,
         COALESCE(SUM(f.valor_centavos) FILTER (WHERE f.status = 'PAGA'), 0)::bigint,
         (COUNT(f.id) FILTER (WHERE f.status IN ('ABERTA', 'EM_ATRASO')))::int,
         (COUNT(f.id) FILTER (WHERE f.status = 'EM_ATRASO'))::int
    FROM janela j
    LEFT JOIN faturas f
           ON f.condominio_id = p_condominio_id
          AND f.competencia = j.competencia
   GROUP BY j.competencia
   ORDER BY j.competencia
$$;

-- ── 2. Mensalidade do Mora em cobrança recorrente ──────────────────────────
--
-- O condomínio paga a plataforma por uma assinatura no Asaas: o gateway gera
-- uma cobrança por mês sozinho, sem job nosso. O valor e o plano vêm do
-- plan-service no momento da ativação e ficam gravados aqui, porque são o que
-- foi contratado no gateway.
CREATE TABLE assinaturas_plataforma (
  id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  condominio_id       VARCHAR(50)  NOT NULL,
  plano_nome          VARCHAR(100) NOT NULL,
  plano_assinatura_id BIGINT,
  asaas_assinatura_id VARCHAR(120) NOT NULL UNIQUE,
  asaas_cliente_id    VARCHAR(120) NOT NULL,
  forma               VARCHAR(20)  NOT NULL CHECK (forma IN ('PIX', 'BOLETO')),
  valor_centavos      BIGINT       NOT NULL CHECK (valor_centavos > 0),
  ciclo               VARCHAR(20)  NOT NULL DEFAULT 'MONTHLY',
  status              VARCHAR(20)  NOT NULL DEFAULT 'ATIVA' CHECK (status IN ('ATIVA', 'CANCELADA')),
  ativada_por_id      INTEGER      NOT NULL,
  criado_em           TIMESTAMPTZ  NOT NULL DEFAULT now(),
  cancelada_em        TIMESTAMPTZ,

  CONSTRAINT ck_assinatura_cancelada CHECK (status <> 'CANCELADA' OR cancelada_em IS NOT NULL)
);

-- Uma cobrança recorrente ativa por condomínio: duas cobrariam a mensalidade
-- em dobro. Parcial, para o histórico de canceladas ficar.
CREATE UNIQUE INDEX uq_assinatura_plataforma_ativa ON assinaturas_plataforma (condominio_id)
  WHERE status = 'ATIVA';

-- Cada cobrança que o Asaas gera para a assinatura. Chega pelo webhook e,
-- como o webhook não alcança um ambiente local, também pela conciliação, que
-- pergunta ao Asaas. As duas gravam pelo id do pagamento, então repetir não
-- duplica.
CREATE TABLE pagamentos_plataforma (
  id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  assinatura_id      UUID         NOT NULL REFERENCES assinaturas_plataforma(id),
  asaas_pagamento_id VARCHAR(120) NOT NULL UNIQUE,
  valor_centavos     BIGINT       NOT NULL CHECK (valor_centavos >= 0),
  vencimento         DATE         NOT NULL,
  status             VARCHAR(20)  NOT NULL
                     CHECK (status IN ('PENDENTE', 'RECEBIDA', 'VENCIDA', 'ESTORNADA', 'CANCELADA')),
  forma              VARCHAR(20),
  url_pagamento      TEXT,
  pago_em            DATE,
  atualizado_em      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_pagamentos_plataforma_assinatura ON pagamentos_plataforma (assinatura_id, vencimento DESC);

-- ── 3. Estornos ────────────────────────────────────────────────────────────
--
-- O estorno é assíncrono no Asaas: o pedido é aceito, e a devolução só se
-- conclui depois (a conta pode exigir autorização da ação). Esta tabela guarda
-- o pedido e o desfecho; a fatura só vira ESTORNADA quando o gateway confirma.
CREATE TABLE estornos (
  id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  condominio_id            VARCHAR(50)  NOT NULL,
  fatura_id                UUID REFERENCES faturas(id),
  pagamento_plataforma_id  UUID REFERENCES pagamentos_plataforma(id),
  asaas_pagamento_id       VARCHAR(120) NOT NULL,
  valor_centavos           BIGINT       NOT NULL CHECK (valor_centavos > 0),
  motivo                   VARCHAR(300) NOT NULL,
  status                   VARCHAR(20)  NOT NULL DEFAULT 'SOLICITADO'
                           CHECK (status IN ('SOLICITADO', 'CONCLUIDO', 'FALHOU')),
  detalhe                  TEXT,
  solicitado_por_id        INTEGER,
  solicitado_em            TIMESTAMPTZ  NOT NULL DEFAULT now(),
  concluido_em             TIMESTAMPTZ,

  -- Ou é de uma fatura de morador, ou de uma mensalidade da plataforma.
  CONSTRAINT ck_estorno_origem CHECK (num_nonnulls(fatura_id, pagamento_plataforma_id) = 1),
  CONSTRAINT ck_estorno_concluido CHECK (status <> 'CONCLUIDO' OR concluido_em IS NOT NULL)
);

-- O mesmo pagamento não é devolvido duas vezes. O pedido que falhou não conta,
-- para poder tentar de novo.
CREATE UNIQUE INDEX uq_estorno_por_pagamento ON estornos (asaas_pagamento_id)
  WHERE status <> 'FALHOU';

CREATE INDEX idx_estornos_condominio ON estornos (condominio_id, solicitado_em DESC);
CREATE INDEX idx_estornos_pendentes ON estornos (solicitado_em) WHERE status = 'SOLICITADO';

-- Down Migration
DROP TABLE IF EXISTS estornos;
DROP TABLE IF EXISTS pagamentos_plataforma;
DROP TABLE IF EXISTS assinaturas_plataforma;
DROP INDEX IF EXISTS uq_fatura_competencia;
CREATE UNIQUE INDEX uq_fatura_competencia ON faturas (condominio_id, unidade_id, competencia)
  WHERE status <> 'CANCELADA';
ALTER TABLE faturas DROP CONSTRAINT IF EXISTS ck_fatura_estornada;
ALTER TABLE faturas DROP COLUMN IF EXISTS estorno_motivo;
ALTER TABLE faturas DROP COLUMN IF EXISTS estornado_em;
ALTER TABLE faturas DROP CONSTRAINT faturas_status_check;
ALTER TABLE faturas ADD CONSTRAINT faturas_status_check
  CHECK (status IN ('ABERTA', 'PAGA', 'EM_ATRASO', 'CANCELADA'));
