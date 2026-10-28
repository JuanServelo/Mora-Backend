-- Cria o banco do gestao-geral (preferencias dos paineis) em um volume que ja
-- existe.
--
-- O init-databases.sql so roda quando o Postgres inicializa um volume vazio.
-- Quem ja subiu a stack antes nao ganha bancos novos por ali, e o gestao-geral
-- sobe apontando para um banco inexistente.
--
-- Rodar:
--   docker exec -i postgres psql -U admin -d postgres < docker/criar-banco-gestao.sql
--
-- CREATE DATABASE nao aceita IF NOT EXISTS nem roda dentro de bloco DO, entao o
-- comando e montado como texto e executado pelo \gexec do psql.

SELECT 'CREATE DATABASE mora_gestao'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'mora_gestao')\gexec
