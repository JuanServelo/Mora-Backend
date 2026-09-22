-- Cria o banco do comunicacao-service em um volume que ja existe.
--
-- Mesma situacao do financeiro: o init-databases.sql so roda em volume virgem,
-- entao quem ja subiu a stack antes nao ganha bancos novos por ali.
--
--   docker exec -i postgres psql -U admin -d postgres < docker/criar-banco-comunicacao.sql

SELECT 'CREATE DATABASE mora_comunicacao'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'mora_comunicacao')\gexec
