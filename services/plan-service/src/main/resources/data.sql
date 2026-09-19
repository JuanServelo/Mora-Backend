-- Seed de planos iniciais da plataforma Mora
-- Executado automaticamente por spring.sql.init.mode=always

INSERT INTO tb_plans (name, max_condominiums, max_users_per_condominium, monthly_price, is_active, created_at, updated_at)
VALUES ('Básico', 1, 50, 299.00, true, NOW(), NOW())
ON CONFLICT DO NOTHING;

INSERT INTO tb_plans (name, max_condominiums, max_users_per_condominium, monthly_price, is_active, created_at, updated_at)
VALUES ('Profissional', 3, 200, 599.00, true, NOW(), NOW())
ON CONFLICT DO NOTHING;

INSERT INTO tb_plans (name, max_condominiums, max_users_per_condominium, monthly_price, is_active, created_at, updated_at)
VALUES ('Enterprise', 10, 500, 999.00, true, NOW(), NOW())
ON CONFLICT DO NOTHING;

-- Módulos do plano Básico
INSERT INTO tb_plan_modules (plan_id, module_slug)
SELECT p.id, m.slug
FROM tb_plans p
CROSS JOIN (VALUES ('portaria'), ('veiculos'), ('chaves')) AS m(slug)
WHERE p.name = 'Básico'
AND NOT EXISTS (
    SELECT 1 FROM tb_plan_modules pm WHERE pm.plan_id = p.id AND pm.module_slug = m.slug
);

-- Módulos do plano Profissional (todos)
INSERT INTO tb_plan_modules (plan_id, module_slug)
SELECT p.id, m.slug
FROM tb_plans p
CROSS JOIN (VALUES ('portaria'), ('reunioes'), ('vagas'), ('entregas'), ('areas_comuns'),
                   ('reclamacoes'), ('conhecimento'), ('votacoes'), ('veiculos'), ('chaves'),
                   ('comunicacao'), ('financeiro')) AS m(slug)
WHERE p.name = 'Profissional'
AND NOT EXISTS (
    SELECT 1 FROM tb_plan_modules pm WHERE pm.plan_id = p.id AND pm.module_slug = m.slug
);

-- Módulos do plano Enterprise (todos)
INSERT INTO tb_plan_modules (plan_id, module_slug)
SELECT p.id, m.slug
FROM tb_plans p
CROSS JOIN (VALUES ('portaria'), ('reunioes'), ('vagas'), ('entregas'), ('areas_comuns'),
                   ('reclamacoes'), ('conhecimento'), ('votacoes'), ('veiculos'), ('chaves'),
                   ('comunicacao'), ('financeiro')) AS m(slug)
WHERE p.name = 'Enterprise'
AND NOT EXISTS (
    SELECT 1 FROM tb_plan_modules pm WHERE pm.plan_id = p.id AND pm.module_slug = m.slug
);
