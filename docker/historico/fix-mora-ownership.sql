-- Conectar ao banco mora e transferir ownership para admin
\c mora;

ALTER TABLE IF EXISTS blocos OWNER TO admin;
ALTER TABLE IF EXISTS apartamentos OWNER TO admin;
ALTER TABLE IF EXISTS areas_comuns OWNER TO admin;
ALTER TABLE IF EXISTS vagas OWNER TO admin;
ALTER TABLE IF EXISTS moradores OWNER TO admin;
ALTER TABLE IF EXISTS visitantes OWNER TO admin;
ALTER TABLE IF EXISTS funcionarios OWNER TO admin;
ALTER TABLE IF EXISTS carros OWNER TO admin;
ALTER TABLE IF EXISTS turnos OWNER TO admin;
ALTER TABLE IF EXISTS entregas OWNER TO admin;
ALTER TABLE IF EXISTS chaves OWNER TO admin;

GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO admin;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO admin;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO admin;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO admin;

\echo 'Ownership transferida para admin com sucesso'
