-- =============================================================================
-- Controle de Reservas — aprovação do síndico
--
-- Só é necessário em bancos que JÁ existem. Num banco novo o ddl-auto=update
-- cria a coluna e a constraint corretas na primeira subida.
-- =============================================================================

-- 1) Espaços passam a poder exigir aprovação.
--
-- O ddl-auto não adiciona coluna NOT NULL em tabela com linhas, então o
-- DEFAULT entra junto: os espaços já cadastrados continuam confirmando na
-- hora, que é o comportamento que tinham antes desta mudança.
ALTER TABLE areas_comuns
    ADD COLUMN IF NOT EXISTS exige_aprovacao BOOLEAN NOT NULL DEFAULT FALSE;

-- 2) StatusReserva ganhou EXPIRADA.
--
-- O ddl-auto NÃO recria check constraints existentes. Sem este passo, toda
-- reserva falha com SQLSTATE 23514 assim que o novo código tenta gravar —
-- e o erro chega à tela como "Registro duplicado", que não tem relação
-- nenhuma com a causa.
ALTER TABLE reservas DROP CONSTRAINT IF EXISTS reservas_status_check;
ALTER TABLE reservas ADD CONSTRAINT reservas_status_check
    CHECK (status IN ('PENDENTE', 'APROVADA', 'RECUSADA',
                      'EXPIRADA', 'CANCELADA', 'CONCLUIDA'));

-- 3) Trilha de decisões (RN-10). Criada pelo ddl-auto; documentada aqui.
CREATE TABLE IF NOT EXISTS reserva_auditoria (
    id              VARCHAR(255) PRIMARY KEY,
    reserva_id      VARCHAR(255) NOT NULL,
    "condominioId"  VARCHAR(255),
    status_anterior VARCHAR(255),
    status_novo     VARCHAR(255),
    motivo          VARCHAR(500),
    autor_id        VARCHAR(255),
    autor_nome      VARCHAR(255),
    criado_em       TIMESTAMP
);
