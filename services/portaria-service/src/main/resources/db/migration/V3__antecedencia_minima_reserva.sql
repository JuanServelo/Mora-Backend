-- =============================================================================
-- V3: antecedência mínima para reservar um espaço (RF-10)
--
-- Por espaço, não global: churrasqueira que precisa de limpeza prévia pede
-- dois dias; quadra de uso livre não pede nada.
--
-- DEFAULT 0 desliga a regra, que é exatamente o comportamento de todo espaço
-- já cadastrado — a migração não muda o que hoje é permitido reservar. Sem o
-- DEFAULT, a coluna NOT NULL não entraria em tabela com linhas.
-- =============================================================================

ALTER TABLE areas_comuns
    ADD COLUMN IF NOT EXISTS antecedencia_minima_horas integer NOT NULL DEFAULT 0;

-- Antecedência negativa não significa nada, e 0 já é "sem exigência". O teto
-- de um ano evita o engano de digitar horas onde se queria dias e deixar o
-- espaço impossível de reservar sem que ninguém entenda por quê.
ALTER TABLE areas_comuns
    DROP CONSTRAINT IF EXISTS areas_comuns_antecedencia_check;
ALTER TABLE areas_comuns
    ADD CONSTRAINT areas_comuns_antecedencia_check
    CHECK (antecedencia_minima_horas >= 0 AND antecedencia_minima_horas <= 8760);
