-- Up Migration

-- Confirmação de leitura de aviso (RF-12).
--
-- O aviso continua no `portaria-service`, no banco `mora`. Só o registro de
-- quem leu nasce aqui — mesmo padrão já usado no `financeiro`, onde `multas`
-- referencia `ocorrencia_id` de outro serviço sem FK.
--
-- `aviso_id` é UUID: `avisos.id` no portaria é `GenerationType.UUID`, não
-- serial. Errar o tipo aqui só apareceria na primeira confirmação real.
--
-- Sem FK, a validação é da aplicação: antes de gravar, o serviço busca o aviso
-- no portaria e confere que ele é do condomínio de quem confirma. Sem isso,
-- qualquer UUID seria aceito e o relatório do síndico contaria leitura de aviso
-- que não existe.

CREATE TABLE aviso_leituras (
  id            BIGSERIAL PRIMARY KEY,
  condominio_id VARCHAR(50) NOT NULL,
  aviso_id      UUID        NOT NULL,
  usuario_id    INTEGER     NOT NULL,
  confirmada_em TIMESTAMPTZ NOT NULL DEFAULT now(),

  -- Confirmar duas vezes é a mesma confirmação: a segunda não pode virar linha
  -- nova, senão "quantos leram" conta a mesma pessoa mais de uma vez.
  UNIQUE (aviso_id, usuario_id)
);

-- Relatório do síndico: quem leu um aviso.
CREATE INDEX idx_leituras_aviso ON aviso_leituras (aviso_id);
-- Tela do morador: quais avisos já confirmei.
CREATE INDEX idx_leituras_usuario ON aviso_leituras (usuario_id, condominio_id);

-- Down Migration
DROP TABLE IF EXISTS aviso_leituras;
