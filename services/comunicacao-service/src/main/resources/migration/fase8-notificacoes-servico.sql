-- =============================================================================
-- FASE 8: notificações publicadas por outros serviços
--
-- O financeiro publica fatura fechada, fatura vencida e multa em
-- POST /interno/notificacoes. Para isso a tabela precisa de três coisas:
--
--   origem      quem publicou (`financeiro`, ...)
--   chave_unica identifica o evento, para não notificar duas vezes
--   dados       o que o publicador anexou (id da fatura, da multa), em JSONB
--
-- O `ddl-auto: update` cria as colunas sozinho; o que ele NÃO faz está aqui:
-- o índice único parcial e a ampliação do CHECK do `tipo`.
--
-- Rodar uma vez no banco `mora`. Idempotente: pode rodar de novo sem efeito.
-- =============================================================================

ALTER TABLE notificacoes ADD COLUMN IF NOT EXISTS origem      VARCHAR(30);
ALTER TABLE notificacoes ADD COLUMN IF NOT EXISTS chave_unica VARCHAR(200);
ALTER TABLE notificacoes ADD COLUMN IF NOT EXISTS dados       JSONB;

-- Parcial, e não total: quem publica sem chave não pode colidir com todos os
-- outros que também não têm.
CREATE UNIQUE INDEX IF NOT EXISTS uq_notificacoes_chave_unica
    ON notificacoes (chave_unica)
 WHERE chave_unica IS NOT NULL;

-- O Hibernate criou este CHECK com os valores do enum no primeiro boot, e não o
-- atualiza depois. Sem recriá-lo, o banco recusa os tipos novos do financeiro.
ALTER TABLE notificacoes DROP CONSTRAINT IF EXISTS notificacoes_tipo_check;
ALTER TABLE notificacoes ADD CONSTRAINT notificacoes_tipo_check CHECK (tipo IN (
    'AVISO', 'ENTREGA', 'RESERVA', 'ALUGUEL_VAGA', 'PRE_AUTORIZACAO', 'CHAT', 'SISTEMA',
    'NOVA_FATURA', 'FATURA_VENCIDA', 'PAGAMENTO_CONFIRMADO', 'MULTA_APLICADA', 'MULTA_JULGADA'
));

-- A caixa de entrada lista as não lidas de um usuário, mais novas primeiro.
CREATE INDEX IF NOT EXISTS idx_notificacoes_destinatario
    ON notificacoes (destinatario_id, lida, criado_em DESC);
