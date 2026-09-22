-- Up Migration

-- Conversas do RF-13.
--
-- Dois formatos, e a diferença não é cosmética:
--
--   DIRETA        conversa entre pessoas nomeadas. Quem cria escolhe com quem
--                 fala, então precisa poder listar usuários — o que só a gestão
--                 consegue no auth-api.
--
--   ADMINISTRACAO o morador fala com "a administração", não com uma pessoa. É o
--                 caso real: ninguém quer descobrir o nome do síndico para
--                 relatar um vazamento, e a conversa tem de sobreviver à troca
--                 de síndico. A visibilidade do lado da gestão é por regra
--                 (mesmo condomínio + perfil de gestão), não por linha em
--                 `conversa_participantes`.
--
-- O segundo formato também resolve um limite concreto: o morador não tem
-- permissão para listar os usuários do condomínio, então não teria como montar
-- um destinatário nomeado.

CREATE TABLE conversas (
  id                BIGSERIAL PRIMARY KEY,
  condominio_id     VARCHAR(50) NOT NULL,
  tipo              VARCHAR(20) NOT NULL
                    CHECK (tipo IN ('DIRETA', 'ADMINISTRACAO')),
  assunto           VARCHAR(150) NOT NULL,
  criada_por        INTEGER     NOT NULL,
  -- Denormalizado de propósito: a lista de conversas ordena por isto, e sem a
  -- coluna toda listagem viraria um JOIN com MAX() sobre mensagens.
  ultima_mensagem_em TIMESTAMPTZ,
  encerrada_em      TIMESTAMPTZ,
  criada_em         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_conversas_condominio ON conversas (condominio_id, ultima_mensagem_em DESC NULLS LAST);
-- Sustenta a caixa da gestão: conversas com a administração, abertas primeiro.
CREATE INDEX idx_conversas_administracao ON conversas (condominio_id, ultima_mensagem_em DESC NULLS LAST)
  WHERE tipo = 'ADMINISTRACAO';

CREATE TABLE conversa_participantes (
  conversa_id       BIGINT      NOT NULL REFERENCES conversas (id) ON DELETE CASCADE,
  usuario_id        INTEGER     NOT NULL,
  -- Marca d'água da leitura. A contagem de não lidas é
  -- `mensagens.criada_em > ultima_leitura_em`, sem linha por mensagem lida.
  ultima_leitura_em TIMESTAMPTZ,
  entrou_em         TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (conversa_id, usuario_id)
);

-- "Minhas conversas" parte daqui, então o índice é pelo usuário.
CREATE INDEX idx_participantes_usuario ON conversa_participantes (usuario_id);

CREATE TABLE mensagens (
  id          BIGSERIAL PRIMARY KEY,
  conversa_id BIGINT      NOT NULL REFERENCES conversas (id) ON DELETE CASCADE,
  autor_id    INTEGER     NOT NULL,
  -- Guardado junto porque o autor pode deixar a gestão depois de escrever, e a
  -- mensagem tem de continuar dizendo em que papel ela foi escrita.
  autor_perfil VARCHAR(20) NOT NULL,
  corpo       TEXT        NOT NULL CHECK (length(btrim(corpo)) > 0),
  -- Apagar de verdade destruiria o registro da conversa para o outro lado.
  removida_em TIMESTAMPTZ,
  criada_em   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_mensagens_conversa ON mensagens (conversa_id, criada_em);

-- Down Migration
DROP TABLE IF EXISTS mensagens;
DROP TABLE IF EXISTS conversa_participantes;
DROP TABLE IF EXISTS conversas;
