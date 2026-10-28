-- =============================================================================
-- V4: notificação de pagamento estornado
--
-- O financeiro passou a devolver pagamentos pelo gateway e avisa o morador com
-- o tipo PAGAMENTO_ESTORNADO. O CHECK de `notificacoes.tipo` lista os tipos
-- aceitos, e sem este valor o aviso era recusado (HTTP 400 na rota interna).
-- =============================================================================

ALTER TABLE notificacoes DROP CONSTRAINT IF EXISTS notificacoes_tipo_check;
ALTER TABLE notificacoes ADD CONSTRAINT notificacoes_tipo_check
    CHECK (tipo IN ('AVISO', 'ENTREGA', 'RESERVA', 'ALUGUEL_VAGA', 'PRE_AUTORIZACAO', 'CHAT', 'SISTEMA',
                    'NOVA_FATURA', 'FATURA_VENCIDA', 'PAGAMENTO_CONFIRMADO', 'MULTA_APLICADA', 'MULTA_JULGADA',
                    'PAGAMENTO_ESTORNADO'));
