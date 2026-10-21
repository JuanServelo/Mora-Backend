-- =============================================================================
-- V2: uma assinatura ATIVA por condomínio
--
-- A regra era UNIQUE (condominio_id, status), que vale para todos os status:
-- dois cancelamentos do mesmo condomínio também colidiam. Trocar de plano
-- cancela a assinatura vigente e cria outra, então a segunda troca falhava e o
-- cliente só podia mudar de plano uma vez.
--
-- O certo é um índice único parcial, que só vigia as ativas. JPA não expressa
-- índice parcial, e a correção morava em docker/historico/
-- corrigir-constraint-assinaturas.sql, aplicada à mão. Agora vai a todo banco.
-- =============================================================================

ALTER TABLE tb_assinaturas DROP CONSTRAINT IF EXISTS uq_assinatura_condominio_ativa;

CREATE UNIQUE INDEX IF NOT EXISTS uq_assinatura_ativa
    ON tb_assinaturas (condominio_id)
 WHERE status = 'ATIVA';
