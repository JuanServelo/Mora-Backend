-- =============================================================================
-- V2: índices por condomínio
--
-- Vinham de docker/historico/migrate-condominio-id.sql, aplicado à mão. A
-- agenda, as atas e as votações são sempre consultadas dentro de um
-- condomínio; sem estes índices, cada consulta lê a tabela inteira.
--
-- Idempotente: num banco que já passou pelo script, nada muda.
-- =============================================================================

CREATE INDEX IF NOT EXISTS idx_meetings_cond ON tb_meetings (condominio_id);
CREATE INDEX IF NOT EXISTS idx_atas_cond     ON tb_atas (condominio_id);
CREATE INDEX IF NOT EXISTS idx_poll_cond     ON poll (condominio_id);

-- A agenda é consultada por condomínio e período.
CREATE INDEX IF NOT EXISTS idx_meetings_cond_inicio ON tb_meetings (condominio_id, data_hora_inicio);
