-- =============================================================================
-- V3: aviso para o síndico
--
-- `PublicoAlvo.SINDICO` entrou no código em agosto (e6d607a), mas o CHECK da
-- coluna continuou com os três valores de quando ela foi criada: o Hibernate
-- monta o CHECK do enum no primeiro boot e nunca mais o atualiza. Em todo
-- banco criado antes disso, publicar aviso só para o síndico falhava com
-- SQLSTATE 23514.
--
-- É o mesmo defeito que a fase 8 corrigiu à mão para `notificacoes.tipo`. Com
-- migração versionada, a correção chega a todo banco na próxima subida.
-- =============================================================================

ALTER TABLE avisos DROP CONSTRAINT IF EXISTS avisos_publico_alvo_check;
ALTER TABLE avisos ADD CONSTRAINT avisos_publico_alvo_check
    CHECK (publico_alvo IN ('TODOS', 'MORADORES', 'FUNCIONARIOS', 'SINDICO'));
