-- =============================================================================
-- Migra as reclamações do auth_db para o mora_ocorrencias
--
-- As reclamações nasceram no auth-api, antes da divisão de domínios (D27). Este
-- script as traz para cá:
--
--   auth_db.reclamacoes                -> reclamacoes            (mesmo id e protocolo)
--   auth_db.reclamacoes.interactions   -> reclamacao_interacoes  (JSON vira linha)
--
-- Pré-requisitos:
--   1. o banco mora_ocorrencias existe (docker/criar-banco-ocorrencias.sql);
--   2. o ocorrencias-service subiu uma vez — o Flyway dele cria as tabelas.
--
-- Rodar:
--   docker exec -i postgres psql -U admin -d mora_ocorrencias < services/ocorrencias-service/src/main/resources/db/manual/migrar-reclamacoes-auth-db.sql
--
-- Idempotente: o que já foi migrado (mesmo id) é ignorado. A tabela do auth_db
-- não é apagada — fica como cópia até a equipe conferir a migração.
-- =============================================================================

-- Lê o outro banco do mesmo servidor. Vem com o Postgres (contrib).
CREATE EXTENSION IF NOT EXISTS dblink;

BEGIN;

-- As datas do auth-api são `timestamptz`; aqui são o horário do condomínio,
-- como tudo que o serviço grava (o container roda em America/Sao_Paulo).
INSERT INTO reclamacoes (id, usuario_id, condominio_id, protocolo, categoria, descricao, anexo_url,
                         status, criado_em, atualizado_em, resolvido_em)
SELECT a.id,
       a.user_id::text,
       COALESCE(a.condominio_id, 'default'),
       a.protocolo,
       a.categoria,
       a.descricao,
       a.anexo_url,
       a.status,
       a.criado_em AT TIME ZONE 'America/Sao_Paulo',
       a.atualizado_em AT TIME ZONE 'America/Sao_Paulo',
       -- O auth-api não guardava quando resolveu; a última alteração de uma
       -- resolvida é a melhor estimativa — a mesma que as estatísticas usavam.
       CASE WHEN a.status = 'RESOLVIDO' THEN a.atualizado_em AT TIME ZONE 'America/Sao_Paulo' END
  FROM dblink('dbname=auth_db user=' || current_user,
              'SELECT id, "userId", "condominioId", "protocolNumber", category, description,
                      "attachmentUrl", status::text, "createdAt", "updatedAt"
                 FROM reclamacoes')
       AS a(id bigint, user_id integer, condominio_id varchar, protocolo varchar, categoria varchar,
            descricao text, anexo_url varchar, status varchar, criado_em timestamptz, atualizado_em timestamptz)
 WHERE NOT EXISTS (SELECT 1 FROM reclamacoes r WHERE r.id = a.id);

-- O histórico era um array JSON de { id, response, status, createdAt }, sem
-- autor. Cada item vira uma interação; o `status` registrado nele é o status
-- depois da resposta.
INSERT INTO reclamacao_interacoes (reclamacao_id, mensagem, status_novo, autor_perfil, criado_em)
SELECT a.id,
       item ->> 'response',
       NULLIF(item ->> 'status', ''),
       'ADMIN_SINDICO',
       COALESCE((item ->> 'createdAt')::timestamptz, a.atualizado_em) AT TIME ZONE 'America/Sao_Paulo'
  FROM dblink('dbname=auth_db user=' || current_user,
              'SELECT id, interactions::text, "updatedAt" FROM reclamacoes')
       AS a(id bigint, interactions text, atualizado_em timestamptz)
 CROSS JOIN LATERAL json_array_elements(COALESCE(NULLIF(a.interactions, ''), '[]')::json) AS item
 WHERE COALESCE(item ->> 'response', '') <> ''
   AND NOT EXISTS (SELECT 1 FROM reclamacao_interacoes i WHERE i.reclamacao_id = a.id);

-- Os ids vieram explícitos: a sequência precisa continuar depois do maior,
-- senão a próxima ocorrência registrada colide com uma migrada.
SELECT setval(pg_get_serial_sequence('reclamacoes', 'id'), COALESCE((SELECT MAX(id) FROM reclamacoes), 0) + 1, false);

COMMIT;

SELECT (SELECT COUNT(*) FROM reclamacoes)           AS reclamacoes,
       (SELECT COUNT(*) FROM reclamacao_interacoes) AS interacoes;
