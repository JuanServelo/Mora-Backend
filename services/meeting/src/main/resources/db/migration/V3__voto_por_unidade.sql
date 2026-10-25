-- Unidade é resolvida no auth-api. Votos antigos continuam sem unidade;
-- LegacyVoteMigration encerra essas votações sem apagar seu histórico.
ALTER TABLE tb_poll_vote ADD COLUMN IF NOT EXISTS unidade_id varchar(100);
CREATE UNIQUE INDEX IF NOT EXISTS uq_poll_vote_unidade ON tb_poll_vote (poll_id, unidade_id);
