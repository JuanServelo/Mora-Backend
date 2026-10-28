-- =============================================================================
-- V3: receita da plataforma calculada no banco
--
-- O painel de receita do Admin Geral (gestao-geral, GET /api/gestao/receita)
-- baixava todas as assinaturas e somava em JavaScript: receita recorrente,
-- receita por plano, evolução mês a mês e o quanto cada cliente já rendeu.
-- Com dezenas de clientes e 36 meses de janela, era a plataforma inteira
-- trafegando para virar meia dúzia de números.
--
-- Agora uma função devolve o painel pronto, em JSON:
--   - GROUP BY por plano e por mês, com FILTER e agregações;
--   - generate_series monta a janela de meses, e o LEFT JOIN dá uma linha
--     para cada mês, inclusive os sem cliente;
--   - daterange e o operador && (sobreposição) dizem se a vigência da
--     assinatura cobre algum dia do mês;
--   - DISTINCT ON escolhe uma assinatura por condomínio: a ativa, ou a mais
--     recente;
--   - jsonb_build_object e jsonb_agg montam a resposta sem passar por objeto
--     intermediário no Java.
--
-- Valores em centavos, inteiros: a mensalidade é numeric(10,2), e o
-- arredondamento acontece uma vez, aqui.
-- =============================================================================

CREATE OR REPLACE FUNCTION receita_plataforma(p_meses INTEGER, p_plano VARCHAR)
RETURNS jsonb
LANGUAGE sql STABLE
AS $$
  WITH params AS (
    -- O mesmo intervalo que o gestao-geral aceita: 1 a 36 meses, 12 se vazio.
    SELECT LEAST(GREATEST(COALESCE(p_meses, 12), 1), 36)   AS meses,
           NULLIF(NULLIF(p_plano, ''), 'todos')            AS plano
  ),
  -- O filtro de plano recorta a base ANTES de agregar: os indicadores passam a
  -- descrever só o plano escolhido.
  base AS (
    SELECT a.id, a.condominio_id, a.status, a.vigencia_inicio, a.vigencia_fim, a.created_at,
           p.name                                    AS plano,
           round(p.monthly_price * 100)::bigint      AS mensalidade_centavos,
           -- Vigência fechada nas duas pontas; fim vazio é sem prazo. Um fim
           -- anterior ao início (dado antigo) vira vigência de um dia, em vez
           -- de derrubar a função com "range lower bound must be less than or
           -- equal to range upper bound".
           daterange(a.vigencia_inicio,
                     CASE WHEN a.vigencia_fim < a.vigencia_inicio THEN a.vigencia_inicio
                          ELSE a.vigencia_fim END,
                     '[]')                           AS vigencia
      FROM tb_assinaturas a
      JOIN tb_plans p ON p.id = a.plan_id
     CROSS JOIN params
     WHERE params.plano IS NULL OR p.name = params.plano
  ),
  ativas AS (
    SELECT * FROM base WHERE status = 'ATIVA'
  ),
  janela AS (
    SELECT generate_series(
             date_trunc('month', current_date) - make_interval(months => (SELECT meses FROM params) - 1),
             date_trunc('month', current_date),
             interval '1 month'
           )::date AS mes
  ),
  -- Reconstruída da vigência, não de um histórico gravado: quem esteve
  -- vigente em algum dia do mês conta naquele mês.
  evolucao AS (
    SELECT j.mes,
           COUNT(b.id)::int                               AS clientes,
           COALESCE(SUM(b.mensalidade_centavos), 0)::bigint AS mrr
      FROM janela j
      LEFT JOIN base b
             ON b.vigencia && daterange(j.mes, (j.mes + interval '1 month')::date, '[)')
     GROUP BY j.mes
  ),
  por_plano AS (
    SELECT plano, COUNT(*)::int AS assinaturas, SUM(mensalidade_centavos)::bigint AS mrr
      FROM ativas
     GROUP BY plano
  ),
  clientes AS (
    SELECT DISTINCT ON (condominio_id)
           condominio_id, plano, status, vigencia_inicio, mensalidade_centavos,
           (status = 'ATIVA' AND vigencia @> current_date) AS vigente,
           -- Quanto o cliente já rendeu: mensalidade vezes os meses desde o
           -- início, no mínimo um. Aproximação, porque o plan-service guarda o
           -- contrato, não as cobranças.
           mensalidade_centavos * GREATEST(1,
             ((EXTRACT(YEAR FROM current_date) - EXTRACT(YEAR FROM vigencia_inicio)) * 12
              + EXTRACT(MONTH FROM current_date) - EXTRACT(MONTH FROM vigencia_inicio) + 1)::int
           ) AS acumulado
      FROM base
     ORDER BY condominio_id, (status = 'ATIVA') DESC, created_at DESC
  )
  SELECT jsonb_build_object(
    'filtros', jsonb_build_object(
      'meses', (SELECT meses FROM params),
      'plano', COALESCE((SELECT plano FROM params), 'todos')
    ),
    -- Sempre a lista completa, mesmo com filtro: montada da base filtrada, o
    -- seletor da tela ficaria preso no plano escolhido.
    'planosDisponiveis', COALESCE((
      SELECT jsonb_agg(DISTINCT p.name ORDER BY p.name)
        FROM tb_assinaturas a JOIN tb_plans p ON p.id = a.plan_id
    ), '[]'::jsonb),
    'indicadores', (
      SELECT jsonb_build_object(
               'mrrCentavos',         COALESCE(SUM(mensalidade_centavos), 0),
               'clientesAtivos',      COUNT(*),
               'ticketMedioCentavos', COALESCE(round(AVG(mensalidade_centavos)), 0)
             )
        FROM ativas
    ) || jsonb_build_object(
      'acumuladoCentavos', (SELECT COALESCE(SUM(acumulado), 0) FROM clientes)
    ),
    'porPlano', COALESCE((
      SELECT jsonb_agg(jsonb_build_object('plano', plano, 'assinaturas', assinaturas, 'mrrCentavos', mrr)
                       ORDER BY mrr DESC, plano)
        FROM por_plano
    ), '[]'::jsonb),
    'evolucao', (
      SELECT jsonb_agg(jsonb_build_object('mes', to_char(mes, 'YYYY-MM'), 'clientes', clientes, 'mrrCentavos', mrr)
                       ORDER BY mes)
        FROM evolucao
    ),
    'clientes', COALESCE((
      SELECT jsonb_agg(jsonb_build_object(
               'condominioId', condominio_id, 'plano', plano, 'status', status, 'vigente', vigente,
               'desde', vigencia_inicio, 'mensalidadeCentavos', mensalidade_centavos,
               'acumuladoCentavos', acumulado)
             ORDER BY mensalidade_centavos DESC, condominio_id)
        FROM clientes
    ), '[]'::jsonb),
    -- Todos os condomínios com alguma assinatura, de qualquer plano. O
    -- gestao-geral cruza com a lista do auth-api para achar quem não assina:
    -- são bancos diferentes, e o cruzamento não pode ser um JOIN.
    'condominiosComAssinatura', COALESCE((
      SELECT jsonb_agg(DISTINCT condominio_id) FROM tb_assinaturas
    ), '[]'::jsonb)
  )
$$;
