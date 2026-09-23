-- Corrige o escopo da unicidade de assinaturas.
--
-- A constraint era UNIQUE (condominio_id, status), com a intencao de impedir
-- duas assinaturas ATIVAS no mesmo condominio. So que ela vale para TODOS os
-- status: dois registros CANCELADA do mesmo condominio tambem sao recusados.
--
-- Consequencia pratica: trocar de plano cancela a assinatura vigente e cria
-- outra. Na segunda troca, o cancelamento colide com o cancelamento anterior e
-- a operacao falha. O cliente poderia mudar de plano uma unica vez, e o
-- historico nunca teria mais de uma linha encerrada.
--
-- O correto e um indice unico PARCIAL, que so vigia as ativas. JPA nao expressa
-- indice parcial, por isso a criacao fica aqui e a anotacao saiu da entidade.
--
-- Rodar:
--   docker exec -i postgres psql -U admin -d mora_plan < docker/corrigir-constraint-assinaturas.sql

ALTER TABLE tb_assinaturas DROP CONSTRAINT IF EXISTS uq_assinatura_condominio_ativa;
DROP INDEX IF EXISTS uq_assinatura_ativa;

CREATE UNIQUE INDEX uq_assinatura_ativa
    ON tb_assinaturas (condominio_id)
 WHERE status = 'ATIVA';

SELECT indexname, indexdef FROM pg_indexes
 WHERE tablename = 'tb_assinaturas' AND indexname LIKE 'uq_%';
