-- =============================================================================
-- V1: estrutura inicial do meeting-service (banco `mora_meeting`)
--
-- Até aqui as tabelas nasciam do Hibernate (`ddl-auto: update`), e o que ele
-- não faz (índice parcial, CHECK de enum que ganhou valor novo) ia por SQL
-- avulso, aplicado à mão em cada máquina. Daqui em diante quem cria e evolui o
-- banco é o Flyway, e o Hibernate só confere (`ddl-auto: validate`).
--
-- O conteúdo é o esquema que o Hibernate gera hoje num banco vazio, lido do
-- catálogo do Postgres. Conferido: aplicado num banco vazio, o resultado é
-- idêntico, coluna a coluna, constraint a constraint.
--
-- Idempotente de propósito (IF NOT EXISTS): o banco já existe nas máquinas de
-- quem roda o projeto. Lá esta versão
-- só cria o que faltar e passa a constar no histórico do Flyway.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Cada CREATE TABLE abaixo vem seguido de um ALTER TABLE ... ADD COLUMN IF NOT
-- EXISTS. Num banco novo ele não faz nada. Num banco que já existia, cria a
-- coluna que a entidade ganhou e o banco ainda não tem: era o que o
-- `ddl-auto: update` fazia na subida, e sem isso o `validate` recusaria subir
-- o serviço num banco atrasado. Entram sem NOT NULL, porque a tabela pode ter
-- linhas.
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS poll (
    id bigserial NOT NULL,
    status smallint,
    data_hora_fim timestamp(6),
    condominio_id varchar(255),
    descricao varchar(255),
    titulo varchar(255),
    CONSTRAINT poll_status_check CHECK (((status >= 0) AND (status <= 2))),
    CONSTRAINT poll_pkey PRIMARY KEY (id)
);
ALTER TABLE poll
    ADD COLUMN IF NOT EXISTS status smallint,
    ADD COLUMN IF NOT EXISTS data_hora_fim timestamp(6),
    ADD COLUMN IF NOT EXISTS condominio_id varchar(255),
    ADD COLUMN IF NOT EXISTS descricao varchar(255),
    ADD COLUMN IF NOT EXISTS titulo varchar(255);

CREATE TABLE IF NOT EXISTS tb_ata_presentes (
    ata_id bigint NOT NULL,
    morador_id bigint
);
ALTER TABLE tb_ata_presentes
    ADD COLUMN IF NOT EXISTS ata_id bigint,
    ADD COLUMN IF NOT EXISTS morador_id bigint;

CREATE TABLE IF NOT EXISTS tb_atas (
    id bigserial NOT NULL,
    data_publicacao timestamp(6) NOT NULL,
    meeting_id bigint NOT NULL,
    condominio_id varchar(255),
    decisoes_tomadas text NOT NULL,
    topicos_discutidos text NOT NULL,
    CONSTRAINT tb_atas_pkey PRIMARY KEY (id),
    CONSTRAINT tb_atas_meeting_id_key UNIQUE (meeting_id)
);
ALTER TABLE tb_atas
    ADD COLUMN IF NOT EXISTS data_publicacao timestamp(6),
    ADD COLUMN IF NOT EXISTS meeting_id bigint,
    ADD COLUMN IF NOT EXISTS condominio_id varchar(255),
    ADD COLUMN IF NOT EXISTS decisoes_tomadas text,
    ADD COLUMN IF NOT EXISTS topicos_discutidos text;

CREATE TABLE IF NOT EXISTS tb_meeting_convidados (
    nota_avaliacao integer,
    meeting_id bigint NOT NULL,
    usuario_id bigint NOT NULL,
    comentario_avaliacao varchar(500),
    status_presenca varchar(255) NOT NULL,
    CONSTRAINT tb_meeting_convidados_status_presenca_check CHECK (status_presenca IN ('PENDENTE', 'CONFIRMADO', 'RECUSADO'))
);
ALTER TABLE tb_meeting_convidados
    ADD COLUMN IF NOT EXISTS nota_avaliacao integer,
    ADD COLUMN IF NOT EXISTS meeting_id bigint,
    ADD COLUMN IF NOT EXISTS usuario_id bigint,
    ADD COLUMN IF NOT EXISTS comentario_avaliacao varchar(500),
    ADD COLUMN IF NOT EXISTS status_presenca varchar(255);

CREATE TABLE IF NOT EXISTS tb_meetings (
    id bigserial NOT NULL,
    data_hora_fim timestamp(6) NOT NULL,
    data_hora_inicio timestamp(6) NOT NULL,
    id_organizador bigint NOT NULL,
    titulo varchar(100) NOT NULL,
    descricao varchar(500),
    condominio_id varchar(255),
    google_event_id varchar(255),
    meet_link varchar(255),
    status varchar(255),
    CONSTRAINT tb_meetings_status_check CHECK (status IN ('AGENDADA', 'CANCELADA', 'FINALIZADA')),
    CONSTRAINT tb_meetings_pkey PRIMARY KEY (id),
    CONSTRAINT tb_meetings_google_event_id_key UNIQUE (google_event_id)
);
ALTER TABLE tb_meetings
    ADD COLUMN IF NOT EXISTS data_hora_fim timestamp(6),
    ADD COLUMN IF NOT EXISTS data_hora_inicio timestamp(6),
    ADD COLUMN IF NOT EXISTS id_organizador bigint,
    ADD COLUMN IF NOT EXISTS titulo varchar(100),
    ADD COLUMN IF NOT EXISTS descricao varchar(500),
    ADD COLUMN IF NOT EXISTS condominio_id varchar(255),
    ADD COLUMN IF NOT EXISTS google_event_id varchar(255),
    ADD COLUMN IF NOT EXISTS meet_link varchar(255),
    ADD COLUMN IF NOT EXISTS status varchar(255);

CREATE TABLE IF NOT EXISTS tb_poll_option (
    id bigserial NOT NULL,
    poll_id bigint NOT NULL,
    descricao varchar(100) NOT NULL,
    CONSTRAINT tb_poll_option_pkey PRIMARY KEY (id)
);
ALTER TABLE tb_poll_option
    ADD COLUMN IF NOT EXISTS poll_id bigint,
    ADD COLUMN IF NOT EXISTS descricao varchar(100);

CREATE TABLE IF NOT EXISTS tb_poll_vote (
    id bigserial NOT NULL,
    poll_id bigint NOT NULL,
    poll_option_id bigint NOT NULL,
    usuario_id bigint NOT NULL,
    CONSTRAINT tb_poll_vote_pkey PRIMARY KEY (id)
);
ALTER TABLE tb_poll_vote
    ADD COLUMN IF NOT EXISTS poll_id bigint,
    ADD COLUMN IF NOT EXISTS poll_option_id bigint,
    ADD COLUMN IF NOT EXISTS usuario_id bigint;

-- -----------------------------------------------------------------------------
-- Chaves estrangeiras
--
-- Ficam depois de todas as tabelas porque há referência circular (moradores e
-- apartamentos apontam um para o outro), e passam por uma função porque o
-- banco pode já existir.
--
-- Num banco existente a chave costuma estar lá, criada pelo Hibernate com o
-- mesmo nome ou por um script antigo com outro nome: nesse caso nada acontece.
-- Se ela faltar, entra como NOT VALID e só depois é validada. Se houver linha
-- órfã, a validação falha, a chave passa a valer só para linhas novas e o
-- aviso fica no log, em vez de impedir o serviço de subir.
-- -----------------------------------------------------------------------------
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

SELECT pg_temp.garantir_fk('tb_ata_presentes', 'fkbsa6kujqtm765yt1tnlojtbn4', 'FOREIGN KEY (ata_id) REFERENCES tb_atas(id)');
SELECT pg_temp.garantir_fk('tb_atas', 'fkynx4bsyq81xmoa4wfr4y448t', 'FOREIGN KEY (meeting_id) REFERENCES tb_meetings(id)');
SELECT pg_temp.garantir_fk('tb_meeting_convidados', 'fkw5i3rviqwjbmbcdbe006iqvr', 'FOREIGN KEY (meeting_id) REFERENCES tb_meetings(id)');
SELECT pg_temp.garantir_fk('tb_poll_option', 'fk3xpedh5bj87elftp97g688cv8', 'FOREIGN KEY (poll_id) REFERENCES poll(id)');
SELECT pg_temp.garantir_fk('tb_poll_vote', 'fkc8bmdfrcej4inh8nnpmf7qyku', 'FOREIGN KEY (poll_option_id) REFERENCES tb_poll_option(id)');
SELECT pg_temp.garantir_fk('tb_poll_vote', 'fknl68rqyqqb6irirqdye4w7k5n', 'FOREIGN KEY (poll_id) REFERENCES poll(id)');
