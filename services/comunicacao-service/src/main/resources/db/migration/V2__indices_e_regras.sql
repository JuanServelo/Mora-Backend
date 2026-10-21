-- =============================================================================
-- V2: índices e regras que o Hibernate não cria
--
-- Vinham dos scripts manuais de db/historico (fase5 a fase8) e de
-- docker/historico/migrate-condominio-id.sql, aplicados à mão em cada banco.
-- Quem não tinha rodado algum ficava sem o índice, ou pior, sem a regra: sem
-- o índice único de `chave_unica`, o financeiro notificaria duas vezes o
-- mesmo evento.
--
-- Entra só o que de fato está no banco em uso. Idempotente: num banco que já
-- passou pelos scripts, nada muda.
-- =============================================================================

-- Toda listagem filtra por condomínio.
CREATE INDEX IF NOT EXISTS idx_artigos_cond ON artigos_conhecimento ("condominioId");

-- "Essa resposta ajudou?": os totais são agrupados por artigo.
CREATE INDEX IF NOT EXISTS idx_artigo_avaliacoes_artigo ON artigo_avaliacoes (artigo_id);

-- Fila de perguntas da administração e "minhas perguntas".
CREATE INDEX IF NOT EXISTS idx_faq_perguntas_condominio ON faq_perguntas ("condominioId", status);
CREATE INDEX IF NOT EXISTS idx_faq_perguntas_autor      ON faq_perguntas (autor_id);

-- -----------------------------------------------------------------------------
-- Notificações (fase 8)
-- -----------------------------------------------------------------------------

-- A caixa de entrada lista as não lidas de um usuário, mais novas primeiro.
CREATE INDEX IF NOT EXISTS idx_notificacoes_destinatario
    ON notificacoes (destinatario_id, lida, criado_em DESC);

-- Quem publica (o financeiro) manda uma chave por evento; a segunda tentativa
-- com a mesma chave é recusada aqui. Parcial, e não total: quem publica sem
-- chave não pode colidir com todos os outros que também não têm.
CREATE UNIQUE INDEX IF NOT EXISTS uq_notificacoes_chave_unica
    ON notificacoes (chave_unica)
 WHERE chave_unica IS NOT NULL;

-- -----------------------------------------------------------------------------
-- Conversas (fase 7)
-- -----------------------------------------------------------------------------

CREATE INDEX IF NOT EXISTS idx_conversas_condominio
    ON conversas (condominio_id, ultima_mensagem_em DESC NULLS LAST);

-- Sustenta a caixa da gestão: conversas com a administração, abertas primeiro.
CREATE INDEX IF NOT EXISTS idx_conversas_administracao
    ON conversas (condominio_id, ultima_mensagem_em DESC NULLS LAST)
 WHERE tipo = 'ADMINISTRACAO';

-- "Minhas conversas" parte do participante.
CREATE INDEX IF NOT EXISTS idx_participantes_usuario ON conversa_participantes (usuario_id);
CREATE INDEX IF NOT EXISTS idx_mensagens_conversa    ON mensagens (conversa_id, criada_em);

-- Mensagem vazia não é mensagem. A tela já recusa; o banco garante.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                    WHERE conrelid = 'mensagens'::regclass AND conname = 'mensagens_corpo_check') THEN
        ALTER TABLE mensagens
            ADD CONSTRAINT mensagens_corpo_check CHECK (length(btrim(corpo)) > 0) NOT VALID;
        BEGIN
            ALTER TABLE mensagens VALIDATE CONSTRAINT mensagens_corpo_check;
        EXCEPTION WHEN check_violation THEN
            RAISE WARNING 'mensagens_corpo_check: há mensagens vazias; a regra vale só para as novas';
        END;
    END IF;
END $$;

-- Apagar a conversa leva junto participantes e mensagens. As entidades guardam
-- só o id da conversa, então a chave não vem do Hibernate.
CREATE OR REPLACE FUNCTION pg_temp.garantir_fk(tabela regclass, nome text, definicao text) RETURNS void AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_constraint
                WHERE conrelid = tabela AND contype = 'f'
                  AND (conname = nome OR pg_get_constraintdef(oid) = definicao)) THEN
        RETURN;
    END IF;
    EXECUTE format('ALTER TABLE %s ADD CONSTRAINT %I %s NOT VALID', tabela, nome, definicao);
    BEGIN
        EXECUTE format('ALTER TABLE %s VALIDATE CONSTRAINT %I', tabela, nome);
    EXCEPTION WHEN foreign_key_violation THEN
        RAISE WARNING '%: há linhas órfãs; a chave vale só para as linhas novas', nome;
    END;
END
$$ LANGUAGE plpgsql;

SELECT pg_temp.garantir_fk('conversa_participantes', 'conversa_participantes_conversa_id_fkey',
                           'FOREIGN KEY (conversa_id) REFERENCES conversas(id) ON DELETE CASCADE');
SELECT pg_temp.garantir_fk('mensagens', 'mensagens_conversa_id_fkey',
                           'FOREIGN KEY (conversa_id) REFERENCES conversas(id) ON DELETE CASCADE');
