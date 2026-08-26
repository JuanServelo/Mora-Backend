-- ============================================================================
-- Associa todo dado de domínio ao condomínio dono do registro.
--
-- O condomínio É o cliente da plataforma. Sem esta coluna, um síndico ou
-- porteiro enxerga a operação de outros condomínios: entregas, visitantes,
-- veículos, chaves, assembleias e aluguéis eram globais.
--
-- Os serviços Java criam a coluna pelo ddl-auto, mas deixam NULL. Este script
-- faz o backfill, cria os índices e corrige as constraints que precisam ser
-- únicas por condomínio, não globalmente.
--
-- Idempotente: pode rodar mais de uma vez.
--
-- Uso:
--   docker exec -i postgres psql -U admin -d postgres -f - < migrate-condominio-id.sql
-- ou, com o arquivo copiado para dentro do container:
--   docker exec postgres psql -U admin -f /tmp/migrate-condominio-id.sql
-- ============================================================================

\set COND_PADRAO '''default'''

-- ─────────────────────────────────────────────────────── mora (portaria) ───
\c mora;

ALTER TABLE moradores            ADD COLUMN IF NOT EXISTS "condominioId" VARCHAR(255);
ALTER TABLE funcionarios         ADD COLUMN IF NOT EXISTS "condominioId" VARCHAR(255);
ALTER TABLE visitantes           ADD COLUMN IF NOT EXISTS "condominioId" VARCHAR(255);
ALTER TABLE veiculos             ADD COLUMN IF NOT EXISTS "condominioId" VARCHAR(255);
ALTER TABLE carros               ADD COLUMN IF NOT EXISTS "condominioId" VARCHAR(255);
ALTER TABLE entregas             ADD COLUMN IF NOT EXISTS "condominioId" VARCHAR(255);
ALTER TABLE chaves               ADD COLUMN IF NOT EXISTS "condominioId" VARCHAR(255);
ALTER TABLE turnos               ADD COLUMN IF NOT EXISTS "condominioId" VARCHAR(255);
ALTER TABLE vagas_estacionamento ADD COLUMN IF NOT EXISTS "condominioId" VARCHAR(255);
ALTER TABLE artigos_conhecimento ADD COLUMN IF NOT EXISTS "condominioId" VARCHAR(255);

-- Backfill por vínculo real, onde ele existe ----------------------------------

-- Morador herda do apartamento, que já tem condomínio.
UPDATE moradores m
SET "condominioId" = a."condominioId"
FROM apartamentos a
WHERE m.apartamento_id = a.id AND m."condominioId" IS NULL;

-- Sem apartamento, tenta pelo bloco.
UPDATE moradores m
SET "condominioId" = b."condominioId"
FROM blocos b
WHERE m.bloco_id = b.id AND m."condominioId" IS NULL;

UPDATE visitantes v
SET "condominioId" = a."condominioId"
FROM apartamentos a
WHERE v.apartamento_id = a.id AND v."condominioId" IS NULL;

UPDATE visitantes v
SET "condominioId" = b."condominioId"
FROM blocos b
WHERE v.bloco_id = b.id AND v."condominioId" IS NULL;

-- Vaga herda do apartamento a que está vinculada.
UPDATE vagas_estacionamento vg
SET "condominioId" = a."condominioId"
FROM apartamentos a
WHERE vg.apartamento_id = a.id AND vg."condominioId" IS NULL;

-- Veículo herda da vaga.
UPDATE veiculos ve
SET "condominioId" = vg."condominioId"
FROM vagas_estacionamento vg
WHERE ve.vaga_id = vg.id AND ve."condominioId" IS NULL;

-- Resto vai para o condomínio padrão ------------------------------------------
UPDATE moradores            SET "condominioId" = :COND_PADRAO WHERE "condominioId" IS NULL;
UPDATE funcionarios         SET "condominioId" = :COND_PADRAO WHERE "condominioId" IS NULL;
UPDATE visitantes           SET "condominioId" = :COND_PADRAO WHERE "condominioId" IS NULL;
UPDATE veiculos             SET "condominioId" = :COND_PADRAO WHERE "condominioId" IS NULL;
UPDATE carros               SET "condominioId" = :COND_PADRAO WHERE "condominioId" IS NULL;
UPDATE entregas             SET "condominioId" = :COND_PADRAO WHERE "condominioId" IS NULL;
UPDATE chaves               SET "condominioId" = :COND_PADRAO WHERE "condominioId" IS NULL;
UPDATE turnos               SET "condominioId" = :COND_PADRAO WHERE "condominioId" IS NULL;
UPDATE vagas_estacionamento SET "condominioId" = :COND_PADRAO WHERE "condominioId" IS NULL;
UPDATE artigos_conhecimento SET "condominioId" = :COND_PADRAO WHERE "condominioId" IS NULL;

-- Índices: toda consulta passa a filtrar por condomínio ------------------------
CREATE INDEX IF NOT EXISTS idx_moradores_cond            ON moradores("condominioId");
CREATE INDEX IF NOT EXISTS idx_funcionarios_cond         ON funcionarios("condominioId");
CREATE INDEX IF NOT EXISTS idx_visitantes_cond           ON visitantes("condominioId");
CREATE INDEX IF NOT EXISTS idx_veiculos_cond             ON veiculos("condominioId");
CREATE INDEX IF NOT EXISTS idx_carros_cond               ON carros("condominioId");
CREATE INDEX IF NOT EXISTS idx_entregas_cond             ON entregas("condominioId");
CREATE INDEX IF NOT EXISTS idx_chaves_cond               ON chaves("condominioId");
CREATE INDEX IF NOT EXISTS idx_turnos_cond               ON turnos("condominioId");
CREATE INDEX IF NOT EXISTS idx_vagas_cond                ON vagas_estacionamento("condominioId");
CREATE INDEX IF NOT EXISTS idx_artigos_cond              ON artigos_conhecimento("condominioId");

-- Índices compostos para os filtros mais usados da portaria
CREATE INDEX IF NOT EXISTS idx_visitantes_cond_status ON visitantes("condominioId", status);
CREATE INDEX IF NOT EXISTS idx_entregas_cond_status   ON entregas("condominioId", status);
CREATE INDEX IF NOT EXISTS idx_veiculos_cond_status   ON veiculos("condominioId", status);

-- Unicidade que era global e passa a ser por condomínio ------------------------
-- Dois condomínios podem ter "Salão de Festas", a placa ABC-1234 e a vaga 12.
ALTER TABLE areas_comuns           DROP CONSTRAINT IF EXISTS areas_comuns_nome_key;
ALTER TABLE vagas_estacionamento   DROP CONSTRAINT IF EXISTS vagas_estacionamento_numero_key;
ALTER TABLE veiculos               DROP CONSTRAINT IF EXISTS veiculos_placa_key;
ALTER TABLE carros                 DROP CONSTRAINT IF EXISTS carros_placa_key;
ALTER TABLE moradores              DROP CONSTRAINT IF EXISTS moradores_cpf_key;
ALTER TABLE funcionarios           DROP CONSTRAINT IF EXISTS funcionarios_cpf_key;
ALTER TABLE visitantes             DROP CONSTRAINT IF EXISTS visitantes_cpf_key;

CREATE UNIQUE INDEX IF NOT EXISTS uq_areas_comuns_nome_cond ON areas_comuns("condominioId", nome);
CREATE UNIQUE INDEX IF NOT EXISTS uq_vagas_numero_cond      ON vagas_estacionamento("condominioId", numero);
CREATE UNIQUE INDEX IF NOT EXISTS uq_veiculos_placa_cond    ON veiculos("condominioId", placa);
CREATE UNIQUE INDEX IF NOT EXISTS uq_carros_placa_cond      ON carros("condominioId", placa);
CREATE UNIQUE INDEX IF NOT EXISTS uq_moradores_cpf_cond     ON moradores("condominioId", cpf);
CREATE UNIQUE INDEX IF NOT EXISTS uq_funcionarios_cpf_cond  ON funcionarios("condominioId", cpf);
CREATE UNIQUE INDEX IF NOT EXISTS uq_visitantes_cpf_cond    ON visitantes("condominioId", cpf);

-- ──────────────────────────────────────────────────────────── mora_meeting ───
\c mora_meeting;

ALTER TABLE tb_meetings ADD COLUMN IF NOT EXISTS condominio_id VARCHAR(255);
ALTER TABLE tb_atas     ADD COLUMN IF NOT EXISTS condominio_id VARCHAR(255);
ALTER TABLE poll        ADD COLUMN IF NOT EXISTS condominio_id VARCHAR(255);

-- Ata e enquete herdam da assembleia.
UPDATE tb_atas a SET condominio_id = m.condominio_id
FROM tb_meetings m WHERE a.meeting_id = m.id AND a.condominio_id IS NULL;

UPDATE poll p SET condominio_id = m.condominio_id
FROM tb_meetings m WHERE p.meeting_id = m.id AND p.condominio_id IS NULL;

UPDATE tb_meetings SET condominio_id = :COND_PADRAO WHERE condominio_id IS NULL;
UPDATE tb_atas     SET condominio_id = :COND_PADRAO WHERE condominio_id IS NULL;
UPDATE poll        SET condominio_id = :COND_PADRAO WHERE condominio_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_meetings_cond ON tb_meetings(condominio_id);
CREATE INDEX IF NOT EXISTS idx_atas_cond     ON tb_atas(condominio_id);
CREATE INDEX IF NOT EXISTS idx_poll_cond     ON poll(condominio_id);

-- A agenda é sempre consultada por condomínio + período.
CREATE INDEX IF NOT EXISTS idx_meetings_cond_inicio ON tb_meetings(condominio_id, data_hora_inicio);

-- ─────────────────────────────────────────────────────────────── vagas_db ───
-- Este banco é candidato a ser descontinuado (o vagas-service seria fundido no
-- portaria-service). Enquanto existir, precisa do mesmo isolamento.
\c vagas_db;

ALTER TABLE alugueis_vagas        ADD COLUMN IF NOT EXISTS "condominioId" VARCHAR(255);
ALTER TABLE disponibilidade_vagas ADD COLUMN IF NOT EXISTS "condominioId" VARCHAR(255);

UPDATE alugueis_vagas        SET "condominioId" = :COND_PADRAO WHERE "condominioId" IS NULL;
UPDATE disponibilidade_vagas SET "condominioId" = :COND_PADRAO WHERE "condominioId" IS NULL;

CREATE INDEX IF NOT EXISTS idx_alugueis_cond        ON alugueis_vagas("condominioId");
CREATE INDEX IF NOT EXISTS idx_disponibilidade_cond ON disponibilidade_vagas("condominioId");

-- Consulta central do serviço: vaga livre num período, dentro do condomínio.
CREATE INDEX IF NOT EXISTS idx_alugueis_cond_periodo
  ON alugueis_vagas("condominioId", vaga_id, data_inicio, data_fim);
CREATE INDEX IF NOT EXISTS idx_disponibilidade_cond_periodo
  ON disponibilidade_vagas("condominioId", vaga_id, data_inicio, data_fim);
