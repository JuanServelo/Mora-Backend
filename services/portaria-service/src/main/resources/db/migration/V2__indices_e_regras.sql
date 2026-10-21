-- =============================================================================
-- V2: índices e regras que o Hibernate não cria
--
-- Vinham de dois scripts aplicados à mão: o docker/init-databases.sql, que
-- criava blocos, apartamentos e áreas comuns antes do serviço subir, e o
-- docker/historico/migrate-condominio-id.sql, que separou os dados por
-- condomínio. Em banco novo, sem rodar este último, a portaria funcionava sem
-- nenhum índice por condomínio, e toda consulta filtra por ele.
--
-- Entra só o que de fato está no banco em uso. Idempotente: num banco que já
-- passou pelos scripts, nada muda.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Estrutura física (do init-databases.sql)
-- -----------------------------------------------------------------------------

-- Dois apartamentos 101 no mesmo bloco seriam o mesmo apartamento.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                    WHERE conrelid = 'apartamentos'::regclass AND contype = 'u'
                      AND pg_get_constraintdef(oid) = 'UNIQUE (numero, "blocoId")') THEN
        ALTER TABLE apartamentos
            ADD CONSTRAINT "apartamentos_numero_blocoId_key" UNIQUE (numero, "blocoId");
    END IF;
EXCEPTION WHEN unique_violation THEN
    RAISE WARNING 'apartamentos: número repetido no mesmo bloco; unicidade não aplicada';
END $$;

CREATE INDEX IF NOT EXISTS idx_blocos_ativo                 ON blocos (ativo);
CREATE INDEX IF NOT EXISTS idx_apartamentos_ativo           ON apartamentos (ativo);
CREATE INDEX IF NOT EXISTS idx_apartamentos_blocoid         ON apartamentos ("blocoId");
CREATE INDEX IF NOT EXISTS idx_apartamentos_numero_blocoid  ON apartamentos (numero, "blocoId");
CREATE INDEX IF NOT EXISTS idx_areas_comuns_ativo           ON areas_comuns (ativo);
CREATE INDEX IF NOT EXISTS idx_areas_comuns_tipo            ON areas_comuns (tipo);
CREATE INDEX IF NOT EXISTS idx_areas_comuns_pode_reservar   ON areas_comuns ("podeReservar");

-- -----------------------------------------------------------------------------
-- Separação por condomínio (do migrate-condominio-id.sql)
-- -----------------------------------------------------------------------------

-- Toda consulta da portaria filtra por condomínio.
CREATE INDEX IF NOT EXISTS idx_moradores_cond     ON moradores ("condominioId");
CREATE INDEX IF NOT EXISTS idx_funcionarios_cond  ON funcionarios ("condominioId");
CREATE INDEX IF NOT EXISTS idx_visitantes_cond    ON visitantes ("condominioId");
CREATE INDEX IF NOT EXISTS idx_veiculos_cond      ON veiculos ("condominioId");
CREATE INDEX IF NOT EXISTS idx_entregas_cond      ON entregas ("condominioId");
CREATE INDEX IF NOT EXISTS idx_chaves_cond        ON chaves ("condominioId");
CREATE INDEX IF NOT EXISTS idx_turnos_cond        ON turnos ("condominioId");
CREATE INDEX IF NOT EXISTS idx_vagas_cond         ON vagas_estacionamento ("condominioId");

-- Os filtros mais usados das telas da portaria.
CREATE INDEX IF NOT EXISTS idx_visitantes_cond_status ON visitantes ("condominioId", status);
CREATE INDEX IF NOT EXISTS idx_entregas_cond_status   ON entregas ("condominioId", status);
CREATE INDEX IF NOT EXISTS idx_veiculos_cond_status   ON veiculos ("condominioId", status);

-- Unicidade por condomínio. Por enquanto convive com a global: as entidades
-- ainda declaram CPF, placa e número da vaga únicos na plataforma inteira, e o
-- V1 cria essas constraints. Liberar "a vaga 12" em dois condomínios é uma
-- migração futura que derruba as globais; estes índices já estarão no lugar.
CREATE UNIQUE INDEX IF NOT EXISTS uq_areas_comuns_nome_cond ON areas_comuns ("condominioId", nome);
CREATE UNIQUE INDEX IF NOT EXISTS uq_vagas_numero_cond      ON vagas_estacionamento ("condominioId", numero);
CREATE UNIQUE INDEX IF NOT EXISTS uq_veiculos_placa_cond    ON veiculos ("condominioId", placa);
CREATE UNIQUE INDEX IF NOT EXISTS uq_moradores_cpf_cond     ON moradores ("condominioId", cpf);
CREATE UNIQUE INDEX IF NOT EXISTS uq_funcionarios_cpf_cond  ON funcionarios ("condominioId", cpf);
CREATE UNIQUE INDEX IF NOT EXISTS uq_visitantes_cpf_cond    ON visitantes ("condominioId", cpf);
