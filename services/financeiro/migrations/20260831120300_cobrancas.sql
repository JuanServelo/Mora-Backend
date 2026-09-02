-- Up Migration

CREATE TABLE cobrancas (
  id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  fatura_id      UUID        NOT NULL REFERENCES faturas(id) ON DELETE CASCADE,
  condominio_id  VARCHAR(50) NOT NULL,
  -- Id da cobrança no Asaas. Único: é a chave que liga o webhook de volta.
  asaas_id       VARCHAR(120) NOT NULL UNIQUE,
  billing_type   VARCHAR(20) NOT NULL CHECK (billing_type IN ('PIX', 'BOLETO')),
  status         VARCHAR(30) NOT NULL DEFAULT 'PENDENTE',
  valor_centavos BIGINT      NOT NULL CHECK (valor_centavos >= 0),
  vencimento     DATE        NOT NULL,
  url_boleto     TEXT,
  pix_payload    TEXT,
  pix_qrcode     TEXT,
  criado_em      TIMESTAMPTZ NOT NULL DEFAULT now(),
  atualizado_em  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_cobrancas_fatura ON cobrancas (fatura_id);
CREATE INDEX idx_cobrancas_condominio ON cobrancas (condominio_id);

-- O que impede a baixa dupla.
--
-- Gateways reenviam webhook: por timeout, por retentativa, por reprocessamento
-- do lado deles. Sem esta tabela a segunda entrega dá baixa de novo. O INSERT
-- aqui e a baixa da fatura precisam rodar na MESMA transação — separados, uma
-- falha entre os dois deixa o evento marcado como visto sem ter surtido efeito.
CREATE TABLE webhook_eventos (
  id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  evento_id           VARCHAR(160) NOT NULL UNIQUE,
  tipo                VARCHAR(60)  NOT NULL,
  asaas_pagamento_id  VARCHAR(120),
  payload             JSONB        NOT NULL,
  recebido_em         TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_webhook_pagamento ON webhook_eventos (asaas_pagamento_id);

-- Down Migration
DROP TABLE IF EXISTS webhook_eventos;
DROP TABLE IF EXISTS cobrancas;
