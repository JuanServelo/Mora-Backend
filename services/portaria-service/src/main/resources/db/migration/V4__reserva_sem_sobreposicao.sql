-- =============================================================================
-- V4: duas reservas não podem ocupar o mesmo espaço ao mesmo tempo
--
-- A checagem já existia no ReservaService, com lock pessimista. Ela continua —
-- é ela que produz a mensagem legível para quem está reservando. Esta
-- constraint é a garantia de última instância: nenhum caminho que escreva na
-- tabela, nem um UPDATE manual, nem um serviço futuro, consegue criar a
-- sobreposição. Regra de negócio que vive no banco não depende de quem chama.
--
-- `tsrange`, e não `tstzrange`: inicio e fim são `timestamp without time zone`
-- (a aplicação grava LocalDateTime). Usar tstzrange forçaria uma conversão com
-- o fuso do servidor e mudaria o significado do dado já gravado.
--
-- `[)` — início fechado, fim aberto: uma reserva que termina às 16:00 e outra
-- que começa às 16:00 NÃO se sobrepõem. É o mesmo recorte do
-- findConflitantes (`inicio < :fim AND fim > :inicio`), então o banco e o
-- serviço concordam sobre o que é conflito.
--
-- O WHERE repete o StatusReserva.ocupaHorario(): cancelada, recusada e
-- expirada liberam o horário, e sem o filtro uma reserva cancelada bloquearia
-- o espaço para sempre.
-- =============================================================================

-- Necessária para comparar area_comum_id (varchar) com `=` dentro de um índice
-- GiST, que por padrão só conhece os tipos geométricos e de intervalo.
CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE reservas
    DROP CONSTRAINT IF EXISTS reservas_sem_sobreposicao;

ALTER TABLE reservas
    ADD CONSTRAINT reservas_sem_sobreposicao
    EXCLUDE USING gist (
        area_comum_id WITH =,
        tsrange(inicio, fim, '[)') WITH &&
    )
    WHERE (status IN ('PENDENTE', 'APROVADA', 'CONCLUIDA'));
