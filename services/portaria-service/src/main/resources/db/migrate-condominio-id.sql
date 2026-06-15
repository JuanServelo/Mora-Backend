-- Vincula estruturas físicas ao condomínio (cliente).
ALTER TABLE blocos ADD COLUMN IF NOT EXISTS "condominioId" VARCHAR(50);
UPDATE blocos SET "condominioId" = 'default' WHERE "condominioId" IS NULL;

ALTER TABLE apartamentos ADD COLUMN IF NOT EXISTS "condominioId" VARCHAR(50);
UPDATE apartamentos SET "condominioId" = 'default' WHERE "condominioId" IS NULL;

ALTER TABLE areas_comuns ADD COLUMN IF NOT EXISTS "condominioId" VARCHAR(50);
UPDATE areas_comuns SET "condominioId" = 'default' WHERE "condominioId" IS NULL;

-- Permite mesmo nome de bloco em condomínios diferentes (unicidade por condomínio).
ALTER TABLE blocos DROP CONSTRAINT IF EXISTS blocos_nome_key;
ALTER TABLE blocos DROP CONSTRAINT IF EXISTS "blocos_nome_key";
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'blocos_nome_condominioId_key'
  ) THEN
    ALTER TABLE blocos
      ADD CONSTRAINT blocos_nome_condominioId_key UNIQUE (nome, "condominioId");
  END IF;
END$$;
