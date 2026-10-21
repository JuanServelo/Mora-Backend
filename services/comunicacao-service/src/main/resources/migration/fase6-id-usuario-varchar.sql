-- =============================================================================
-- FASE 6: id de usuario deixa de ser UUID
--
-- Por que
-- -------
-- O auth-api numera usuarios com `integer` autoincremental (`users.id`), e e
-- esse valor que viaja na claim do token. As tabelas deste servico declaravam
-- o id de usuario como UUID, entao toda rota que precisava saber *quem* estava
-- pedindo caia com 500:
--
--     java.lang.IllegalArgumentException: Invalid UUID string: 32
--
-- Isso derrubava confirmacao de leitura, a caixa de notificacoes inteira e o
-- chat. So sobreviviam as rotas que nao olham o usuario — o CRUD de avisos e
-- artigos. Passou despercebido porque nenhuma tela consumia essas rotas: as
-- tres tabelas estavam com zero linhas quando o defeito foi encontrado.
--
-- Por que VARCHAR, e nao BIGINT
-- -----------------------------
-- BIGINT casaria com o tipo de hoje, mas prenderia o esquema a ele. VARCHAR
-- aceita o formato atual e sobrevive a uma eventual migracao dos usuarios para
-- UUID sem nova alteracao de esquema. O preco e nao haver tipo forte no banco.
--
-- Seguranca da conversao
-- ----------------------
-- `USING coluna::text` e seguro nos dois sentidos: converte UUID existente para
-- a sua representacao textual, sem perda. Se houver linhas, elas sobrevivem —
-- apenas nao corresponderao a nenhum usuario, porque nunca corresponderam.
--
-- `aviso_id` NAO muda: `avisos.id` e UUID de verdade (GenerationType.UUID).
-- =============================================================================

ALTER TABLE aviso_leituras
    ALTER COLUMN usuario_id TYPE VARCHAR(64) USING usuario_id::text;

ALTER TABLE notificacoes
    ALTER COLUMN destinatario_id TYPE VARCHAR(64) USING destinatario_id::text;

ALTER TABLE chat_mensagens
    ALTER COLUMN remetente_id    TYPE VARCHAR(64) USING remetente_id::text,
    ALTER COLUMN destinatario_id TYPE VARCHAR(64) USING destinatario_id::text;

-- Conferencia: as quatro colunas devem aparecer como character varying.
--
--   SELECT table_name, column_name, data_type
--     FROM information_schema.columns
--    WHERE (table_name, column_name) IN (
--            ('aviso_leituras', 'usuario_id'),
--            ('notificacoes',   'destinatario_id'),
--            ('chat_mensagens', 'remetente_id'),
--            ('chat_mensagens', 'destinatario_id'))
--    ORDER BY table_name, column_name;
