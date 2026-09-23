-- Up Migration

-- Núcleo de configuração: o que o síndico define antes de qualquer cobrança.
--
-- Convenções deste banco:
--   * Todo valor monetário é BIGINT em centavos. JavaScript não tem decimal, e
--     NUMERIC chega como string no driver pg — inteiro elimina a classe inteira
--     de erro de arredondamento.
--   * `condominio_id` é VARCHAR(50), o mesmo tipo de condominios.id no auth_db.
--   * `unidade_id` é UUID, o mesmo tipo de apartamentos.id no banco mora.
--   * Não há FK entre bancos: cada serviço tem o seu, e a integridade
--     referencial atravessando serviço é responsabilidade da aplicação.

CREATE TABLE unidades_fracao (
  id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  condominio_id    VARCHAR(50) NOT NULL,
  unidade_id       UUID        NOT NULL,
  -- Milésimos inteiros, como consta em convenção de condomínio. Percentual
  -- fracionário em ponto flutuante nunca soma exatamente 100%.
  fracao_milesimos INTEGER     NOT NULL CHECK (fracao_milesimos > 0),
  criado_em        TIMESTAMPTZ NOT NULL DEFAULT now(),
  atualizado_em    TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_fracao_unidade UNIQUE (condominio_id, unidade_id)
);

CREATE INDEX idx_fracao_condominio ON unidades_fracao (condominio_id);

CREATE TABLE tipos_taxa (
  id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  condominio_id  VARCHAR(50)  NOT NULL,
  nome           VARCHAR(120) NOT NULL,
  descricao      TEXT,
  valor_centavos BIGINT       NOT NULL CHECK (valor_centavos >= 0),
  -- Diz o que `valor_centavos` significa, e sem isso o rateio é ambíguo:
  -- R$ 350 de taxa é 350 por apartamento, ou 350 para o prédio inteiro dividir?
  -- No modo FIXO é por unidade; no modo FRACAO_IDEAL é o total a ratear. Deixar
  -- implícito no modo do condomínio faria a mesma linha significar coisas
  -- diferentes conforme uma configuração que vive em outra tabela.
  base_calculo   VARCHAR(20)  NOT NULL DEFAULT 'POR_UNIDADE'
                 CHECK (base_calculo IN ('POR_UNIDADE', 'TOTAL_CONDOMINIO')),
  periodicidade  VARCHAR(20)  NOT NULL DEFAULT 'MENSAL'
                 CHECK (periodicidade IN ('MENSAL', 'ANUAL', 'UNICA')),
  -- Taxa extraordinária é aprovada em assembleia e costuma ser parcelada.
  extraordinaria BOOLEAN      NOT NULL DEFAULT false,
  parcelas       INTEGER      NOT NULL DEFAULT 1 CHECK (parcelas >= 1),
  ativo          BOOLEAN      NOT NULL DEFAULT true,
  criado_em      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  atualizado_em  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  CONSTRAINT uq_tipo_taxa_nome UNIQUE (condominio_id, nome)
);

CREATE INDEX idx_tipos_taxa_condominio ON tipos_taxa (condominio_id) WHERE ativo;

CREATE TABLE regras_taxa (
  -- Uma linha por condomínio: são as regras de fechamento, não um histórico.
  condominio_id     VARCHAR(50) PRIMARY KEY,
  modo              VARCHAR(20) NOT NULL DEFAULT 'FIXO'
                    CHECK (modo IN ('FIXO', 'FRACAO_IDEAL')),
  dia_fechamento    INTEGER     NOT NULL DEFAULT 25
                    CHECK (dia_fechamento BETWEEN 1 AND 28),
  dia_vencimento    INTEGER     NOT NULL DEFAULT 10
                    CHECK (dia_vencimento BETWEEN 1 AND 28),
  -- Prazo para o morador recorrer de uma multa, contado da aplicação.
  dias_recurso_multa INTEGER    NOT NULL DEFAULT 15 CHECK (dias_recurso_multa >= 0),
  criado_em         TIMESTAMPTZ NOT NULL DEFAULT now(),
  atualizado_em     TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 28 é o teto porque fevereiro existe: dia 30 não cai em todo mês, e a regra
-- precisa valer para os doze.

-- Down Migration
DROP TABLE IF EXISTS regras_taxa;
DROP TABLE IF EXISTS tipos_taxa;
DROP TABLE IF EXISTS unidades_fracao;
