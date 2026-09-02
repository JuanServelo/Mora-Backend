-- Seed data para visualização de planos
-- Este script não é executado automaticamente em produção (spring.sql.init.mode=never)
-- Você pode rodá-lo manualmente no banco `mora_plan` para fins de demonstração

-- Inserindo planos de exemplo
INSERT INTO tb_plans (name, max_condominiums, max_users_per_condominium, monthly_price, is_active, created_at, updated_at, created_by, updated_by)
VALUES 
('Básico', 1, 50, 0.00, true, NOW(), NOW(), 'system', 'system'),
('Profissional', 5, 200, 199.90, true, NOW(), NOW(), 'system', 'system'),
('Enterprise', 999, 9999, 999.90, true, NOW(), NOW(), 'system', 'system');

-- Inserindo módulos ativos para o plano Profissional
INSERT INTO tb_plan_modules (plan_id, module_slug)
SELECT id, 'portaria' FROM tb_plans WHERE name = 'Profissional';
INSERT INTO tb_plan_modules (plan_id, module_slug)
SELECT id, 'reservas' FROM tb_plans WHERE name = 'Profissional';

-- Inserindo módulos ativos para o plano Enterprise
INSERT INTO tb_plan_modules (plan_id, module_slug)
SELECT id, 'portaria' FROM tb_plans WHERE name = 'Enterprise';
INSERT INTO tb_plan_modules (plan_id, module_slug)
SELECT id, 'reservas' FROM tb_plans WHERE name = 'Enterprise';
INSERT INTO tb_plan_modules (plan_id, module_slug)
SELECT id, 'financeiro' FROM tb_plans WHERE name = 'Enterprise';

-- Exemplo de assinatura ativa para um condomínio fictício (id: 'cond-123')
INSERT INTO tb_subscriptions (condominio_id, plan_id, status, start_date, created_at, updated_at, created_by, updated_by)
SELECT 'cond-123', id, 'ACTIVE', NOW(), NOW(), NOW(), 'system', 'system' 
FROM tb_plans WHERE name = 'Profissional';
