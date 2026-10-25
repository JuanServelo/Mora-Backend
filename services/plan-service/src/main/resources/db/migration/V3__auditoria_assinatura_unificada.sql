-- Campos usados pela API unificada e pelo importador do arquivo legado.
ALTER TABLE tb_assinaturas
    ADD COLUMN IF NOT EXISTS created_by varchar(100),
    ADD COLUMN IF NOT EXISTS updated_by varchar(100),
    ADD COLUMN IF NOT EXISTS legacy_subscription_id bigint;
CREATE UNIQUE INDEX IF NOT EXISTS uq_assinatura_legacy_subscription ON tb_assinaturas (legacy_subscription_id);
