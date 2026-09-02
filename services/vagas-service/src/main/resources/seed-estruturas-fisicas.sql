-- Dados de exemplo para Estruturas Físicas
-- Este arquivo pode ser executado manualmente ou incluído como parte da inicialização

-- Conectar ao banco mora
\c mora;

-- Inserir blocos de exemplo
INSERT INTO blocos (id, nome, descricao, andares, "apartamentosPorAndar", ativo, "criadoEm", "atualizadoEm")
VALUES 
  ('123e4567-e89b-12d3-a456-426614174001', 'Bloco A', 'Bloco principal com 4 andares', 4, 4, true, NOW(), NOW()),
  ('123e4567-e89b-12d3-a456-426614174002', 'Bloco B', 'Bloco secundário com 6 andares', 6, 4, true, NOW(), NOW()),
  ('123e4567-e89b-12d3-a456-426614174003', 'Bloco C', 'Bloco auxiliar com 3 andares', 3, 6, true, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Inserir apartamentos do Bloco A
INSERT INTO apartamentos (id, numero, andar, "blocoId", quartos, "areaMxComTotal", observacoes, ativo, "criadoEm", "atualizadoEm")
VALUES 
  ('223e4567-e89b-12d3-a456-426614174001', '101', 1, '123e4567-e89b-12d3-a456-426614174001', 3, 85.5, 'Apto com varanda', true, NOW(), NOW()),
  ('223e4567-e89b-12d3-a456-426614174002', '102', 1, '123e4567-e89b-12d3-a456-426614174001', 2, 65.0, NULL, true, NOW(), NOW()),
  ('223e4567-e89b-12d3-a456-426614174003', '201', 2, '123e4567-e89b-12d3-a456-426614174001', 3, 85.5, NULL, true, NOW(), NOW()),
  ('223e4567-e89b-12d3-a456-426614174004', '202', 2, '123e4567-e89b-12d3-a456-426614174001', 2, 65.0, NULL, true, NOW(), NOW())
ON CONFLICT (numero, "blocoId") DO NOTHING;

-- Inserir apartamentos do Bloco B
INSERT INTO apartamentos (id, numero, andar, "blocoId", quartos, "areaMxComTotal", observacoes, ativo, "criadoEm", "atualizadoEm")
VALUES 
  ('223e4567-e89b-12d3-a456-426614174101', '101', 1, '123e4567-e89b-12d3-a456-426614174002', 4, 120.0, 'Duplex', true, NOW(), NOW()),
  ('223e4567-e89b-12d3-a456-426614174102', '102', 1, '123e4567-e89b-12d3-a456-426614174002', 3, 95.0, NULL, true, NOW(), NOW()),
  ('223e4567-e89b-12d3-a456-426614174103', '201', 2, '123e4567-e89b-12d3-a456-426614174002', 4, 120.0, NULL, true, NOW(), NOW()),
  ('223e4567-e89b-12d3-a456-426614174104', '202', 2, '123e4567-e89b-12d3-a456-426614174002', 3, 95.0, NULL, true, NOW(), NOW())
ON CONFLICT (numero, "blocoId") DO NOTHING;

-- Inserir áreas comuns
INSERT INTO areas_comuns (id, nome, tipo, descricao, localizacao, "capacidadeMaxima", area, "podeReservar", observacoes, ativo, "criadoEm", "atualizadoEm")
VALUES 
  ('323e4567-e89b-12d3-a456-426614174001', 'Churrasqueira 01', 'churrasqueira', 'Churrasqueira com 2 churrascos', 'Andar térreo - Lateral direita', 30, 50.0, true, 'Necessário agendamento prévio', true, NOW(), NOW()),
  ('323e4567-e89b-12d3-a456-426614174002', 'Salão de Festas', 'salao_festas', 'Salão multiuso para eventos', 'Andar térreo - Centro', 100, 200.0, true, 'Capacidade máxima: 100 pessoas', true, NOW(), NOW()),
  ('323e4567-e89b-12d3-a456-426614174003', 'Playground', 'playground', 'Área de recreação infantil', 'Lateral esquerda', 50, 80.0, false, 'Supervisionado de segunda a sexta 15h-19h', true, NOW(), NOW()),
  ('323e4567-e89b-12d3-a456-426614174004', 'Quadra Poliesportiva', 'quadra', 'Quadra para futsal e vôlei', 'Fundos do condomínio', 30, 400.0, true, 'Horários: 9h-22h', true, NOW(), NOW()),
  ('323e4567-e89b-12d3-a456-426614174005', 'Academia', 'academia', 'Academia com equipamentos modernos', 'Andar térreo - Fundos', 20, 150.0, false, 'Horário: 6h-22h', true, NOW(), NOW()),
  ('323e4567-e89b-12d3-a456-426614174006', 'Piscina Adulto', 'piscina', 'Piscina para adultos (1,8m profundidade)', 'Fundos do condomínio', 50, 120.0, false, 'Horário: 7h-19h', true, NOW(), NOW()),
  ('323e4567-e89b-12d3-a456-426614174007', 'Sauna', 'sauna', 'Sauna seca', 'Subsolo - Ala B', 6, 20.0, false, 'Máximo 6 pessoas por sessão', true, NOW(), NOW()),
  ('323e4567-e89b-12d3-a456-426614174008', 'Sala de Reunião', 'sala_reuniao', 'Sala com equipamentos para videoconferência', 'Andar térreo - Ala A', 15, 40.0, true, 'Disponível para sindico e associações', true, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;
