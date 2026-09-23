-- Corrige o escopo da unicidade de blocos.
--
-- O banco carregava `blocos_nome_key UNIQUE (nome)`, criado antes de a coluna
-- condominioId existir. Com ela, dois condominios nao podiam ter um "Bloco A"
-- cada um: o nome do bloco era unico na plataforma inteira, e o segundo cliente
-- a cadastrar recebia 409.
--
-- A entidade Bloco.java ja declara @UniqueConstraint(nome, condominioId), mas o
-- Hibernate com ddl-auto=update nao remove constraint antiga — ele so acrescenta.
-- Por isso a correcao precisa ser manual, uma vez por instalacao.
--
-- Rodar:
--   docker exec -i postgres psql -U admin -d mora < docker/corrigir-constraint-blocos.sql

ALTER TABLE blocos DROP CONSTRAINT IF EXISTS blocos_nome_key;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
     WHERE conrelid = 'blocos'::regclass
       AND conname  = 'blocos_nome_condominioId_key'
  ) THEN
    ALTER TABLE blocos
      ADD CONSTRAINT "blocos_nome_condominioId_key" UNIQUE (nome, "condominioId");
  END IF;
END $$;

SELECT conname, pg_get_constraintdef(oid) AS definicao
  FROM pg_constraint
 WHERE conrelid = 'blocos'::regclass AND contype = 'u';
