-- Up Migration

-- Multa cancelada depois de entrar numa fatura.
--
-- A fatura emitida não é reescrita: apagar o item mudaria um documento que o
-- morador já recebeu, e talvez já pagou. O cancelamento vira um item ESTORNO,
-- negativo, no próximo fechamento da unidade. Esta coluna aponta para esse
-- item; enquanto for nula numa multa CANCELADA que tem `fatura_item_id`, o
-- estorno ainda está pendente.
ALTER TABLE multas
  ADD COLUMN estorno_fatura_item_id UUID REFERENCES fatura_itens(id) ON DELETE SET NULL;

-- O que o próximo fechamento precisa devolver.
CREATE INDEX idx_multas_a_estornar ON multas (condominio_id, unidade_id)
  WHERE status = 'CANCELADA' AND fatura_item_id IS NOT NULL AND estorno_fatura_item_id IS NULL;

-- Down Migration
DROP INDEX IF EXISTS idx_multas_a_estornar;
ALTER TABLE multas DROP COLUMN IF EXISTS estorno_fatura_item_id;
