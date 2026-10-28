-- Up Migration

-- Preferências de tela por usuário: como cada um monta os próprios painéis
-- (blocos visíveis, ordem, largura, altura, tipo de gráfico e cor).
--
-- `usuario_id` é VARCHAR: o auth-api numera usuários com inteiro hoje, e a
-- decisão D28 prevê UUID. Não há FK para o auth_db — cada serviço tem o seu
-- banco, e a identidade vem do token revalidado no auth-api.

CREATE TABLE preferencias_usuario (
  usuario_id    VARCHAR(64) NOT NULL,
  chave         VARCHAR(60) NOT NULL,
  valor         JSONB       NOT NULL,
  atualizado_em TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT preferencias_usuario_pkey PRIMARY KEY (usuario_id, chave),
  -- A mesma regra da rota, também no banco: só chaves de painel conhecidas.
  CONSTRAINT preferencias_usuario_chave_check CHECK (chave ~ '^painel(\.[a-z0-9-]{1,30}){1,3}$'),
  CONSTRAINT preferencias_usuario_valor_check CHECK (jsonb_typeof(valor) = 'object')
);

-- Down Migration

DROP TABLE preferencias_usuario;
