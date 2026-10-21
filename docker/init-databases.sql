-- Cria os bancos de cada microsservico. So os bancos: as tabelas sao de cada
-- servico, que cria e evolui o proprio esquema com uma ferramenta de migracao
-- ao subir.
--
--   mora             comunicacao-service e portaria-service   Flyway
--   mora_plan        plan-service                             Flyway
--   mora_meeting     meeting-service                          Flyway
--   auth_db          auth-api                                 Umzug
--   mora_financeiro  financeiro                               node-pg-migrate
--
-- O banco 'mora' e criado pela variavel POSTGRES_DB do container.
--
-- Ate a adocao das migracoes, este arquivo tambem criava as tabelas do
-- auth_db e da estrutura fisica do 'mora', e inseria um administrador com
-- senha fixa. Esse SQL agora vive na primeira migracao de cada servico, e a
-- conta inicial vem de ADMIN_SEED_EMAIL/ADMIN_SEED_PASSWORD.
--
-- ATENCAO: este script so roda quando o volume postgres_data e criado do zero.
-- Em volume ja existente o Postgres ignora docker-entrypoint-initdb.d, entao um
-- banco novo adicionado aqui NAO aparece em maquina que ja subiu antes. Use
-- docker/criar-banco-financeiro.sql para criar no volume atual.
CREATE DATABASE auth_db;
CREATE DATABASE mora_meeting;
CREATE DATABASE vagas_db;
CREATE DATABASE mora_plan;
CREATE DATABASE mora_financeiro;
