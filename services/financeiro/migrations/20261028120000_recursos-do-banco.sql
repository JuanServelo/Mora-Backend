-- Up Migration

-- Recursos do PostgreSQL que guardam regras de negócio e alimentam o painel
-- da gestão. Ficam no banco, e não só no código, porque valem para qualquer
-- caminho de escrita: rota, job, webhook ou alguém com psql na mão.

-- ── 1. Inadimplência mês a mês (painel estratégico do síndico) ─────────────
--
-- Antes o gestao-geral pedia os KPIs de uma competência por vez: 12 meses
-- eram 12 requisições e 12 consultas. Agora é uma consulta agrupada.
--
-- `generate_series` monta a janela inteira, e o LEFT JOIN garante uma linha
-- por mês, inclusive os sem fatura: o gráfico não fica com buracos e ninguém
-- precisa completar meses em JavaScript. O "hoje" é o do fuso do condomínio,
-- o mesmo que o gestao-geral usa para rotular os meses.
CREATE OR REPLACE FUNCTION kpis_faturas_por_mes(p_condominio_id VARCHAR, p_meses INTEGER)
RETURNS TABLE (
  mes            TEXT,
  total          INTEGER,
  total_centavos BIGINT,
  pagas          INTEGER,
  pagas_centavos BIGINT,
  abertas        INTEGER,
  em_atraso      INTEGER
)
LANGUAGE sql STABLE
AS $$
  WITH janela AS (
    SELECT generate_series(
             date_trunc('month', (now() AT TIME ZONE 'America/Sao_Paulo')::date)
               - make_interval(months => GREATEST(p_meses, 1) - 1),
             date_trunc('month', (now() AT TIME ZONE 'America/Sao_Paulo')::date),
             interval '1 month'
           )::date AS competencia
  )
  SELECT to_char(j.competencia, 'YYYY-MM'),
         (COUNT(f.id) FILTER (WHERE f.status <> 'CANCELADA'))::int,
         COALESCE(SUM(f.valor_centavos) FILTER (WHERE f.status <> 'CANCELADA'), 0)::bigint,
         (COUNT(f.id) FILTER (WHERE f.status = 'PAGA'))::int,
         COALESCE(SUM(f.valor_centavos) FILTER (WHERE f.status = 'PAGA'), 0)::bigint,
         (COUNT(f.id) FILTER (WHERE f.status IN ('ABERTA', 'EM_ATRASO')))::int,
         (COUNT(f.id) FILTER (WHERE f.status = 'EM_ATRASO'))::int
    FROM janela j
    LEFT JOIN faturas f
           ON f.condominio_id = p_condominio_id
          AND f.competencia = j.competencia
   GROUP BY j.competencia
   ORDER BY j.competencia
$$;

-- ── 2. Fatura paga não muda; cancelada não volta ───────────────────────────
--
-- A fatura paga é o recibo do morador e entra na prestação de contas. Hoje
-- todo UPDATE do código já filtra `status IN ('ABERTA', 'EM_ATRASO')`, mas
-- isso é disciplina de quem escreve a consulta. O gatilho transforma a
-- disciplina em regra: nenhum caminho altera valor, competência ou baixa de
-- uma fatura paga, nem a apaga.
--
-- Cancelada também é estado final: reabrir uma fatura cancelada colidiria com
-- a reemissão do mesmo mês, que o `uq_fatura_competencia` libera justamente
-- porque a cancelada deixou de contar.
--
-- O TRUNCATE do seed de desenvolvimento não dispara gatilho de linha.
CREATE OR REPLACE FUNCTION proteger_fatura_encerrada()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  IF TG_OP = 'DELETE' THEN
    IF OLD.status = 'PAGA' THEN
      RAISE EXCEPTION 'A fatura % já foi paga e não pode ser apagada.', OLD.id
        USING ERRCODE = 'restrict_violation';
    END IF;
    RETURN OLD;
  END IF;

  IF OLD.status = 'PAGA' AND
     (NEW.status, NEW.valor_centavos, NEW.valor_pago_centavos, NEW.pago_em, NEW.forma_baixa,
      NEW.competencia, NEW.unidade_id, NEW.condominio_id)
     IS DISTINCT FROM
     (OLD.status, OLD.valor_centavos, OLD.valor_pago_centavos, OLD.pago_em, OLD.forma_baixa,
      OLD.competencia, OLD.unidade_id, OLD.condominio_id)
  THEN
    RAISE EXCEPTION 'A fatura % já foi paga e não pode ser alterada.', OLD.id
      USING ERRCODE = 'restrict_violation';
  END IF;

  IF OLD.status = 'CANCELADA' AND NEW.status <> 'CANCELADA' THEN
    RAISE EXCEPTION 'A fatura % foi cancelada; emita outra para a competência.', OLD.id
      USING ERRCODE = 'restrict_violation';
  END IF;

  RETURN NEW;
END
$$;

CREATE TRIGGER trg_fatura_encerrada
  BEFORE UPDATE OR DELETE ON faturas
  FOR EACH ROW EXECUTE FUNCTION proteger_fatura_encerrada();

-- Os itens são o detalhamento do valor: mexer neles numa fatura paga muda o
-- documento sem mudar o total, que é pior. Multa cancelada depois de paga
-- vira ESTORNO no fechamento seguinte, numa fatura nova.
CREATE OR REPLACE FUNCTION proteger_itens_de_fatura_paga()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
  v_fatura UUID := CASE WHEN TG_OP = 'INSERT' THEN NEW.fatura_id ELSE OLD.fatura_id END;
BEGIN
  IF EXISTS (SELECT 1 FROM faturas WHERE id = v_fatura AND status = 'PAGA') THEN
    RAISE EXCEPTION 'A fatura % já foi paga; os itens dela não mudam.', v_fatura
      USING ERRCODE = 'restrict_violation';
  END IF;
  RETURN CASE WHEN TG_OP = 'DELETE' THEN OLD ELSE NEW END;
END
$$;

CREATE TRIGGER trg_itens_fatura_paga
  BEFORE INSERT OR UPDATE OR DELETE ON fatura_itens
  FOR EACH ROW EXECUTE FUNCTION proteger_itens_de_fatura_paga();

-- ── 3. Contratos da mesma unidade não se sobrepõem no tempo ────────────────
--
-- `uq_contrato_ativo` garante um contrato vigente por unidade, mas só olha o
-- flag `ativo`: nada impedia cadastrar um contrato novo com início dentro do
-- período de um já encerrado, e a unidade passaria a ter dois inquilinos
-- responsáveis pelo mesmo mês.
--
-- A restrição de exclusão compara os períodos (`daterange` com `&&`, "se
-- sobrepõe") de todos os contratos da mesma unidade, ativos ou não. Precisa do
-- `btree_gist` para combinar a igualdade dos textos com a sobreposição no
-- mesmo índice GiST.
--
-- Intervalo `[)`: o dia do fim é o dia da troca. Encerrar um contrato hoje e
-- abrir o próximo hoje é o fluxo normal da tela, e não conta como sobreposição.
-- Contrato sem fim vai até o infinito.
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- Até esta migração, encerrar um contrato antes do prazo mantinha o fim
-- previsto, no futuro. Esse contrato "acabou" no dia do encerramento, que é o
-- `atualizado_em` dele (encerrado não muda mais). Sem isto, o contrato do
-- inquilino seguinte cruzaria o período e a restrição não poderia ser criada.
-- É a mesma regra que o encerramento passa a seguir no código.
UPDATE contratos_locacao
   SET fim = GREATEST(inicio, atualizado_em::date)
 WHERE NOT ativo
   AND fim > atualizado_em::date;

DO $$
DECLARE
  v_conflitos TEXT;
BEGIN
  SELECT string_agg(format('unidade %s: contratos %s e %s', a.unidade_id, a.id, b.id), '; ')
    INTO v_conflitos
    FROM contratos_locacao a
    JOIN contratos_locacao b
      ON b.condominio_id = a.condominio_id
     AND b.unidade_id = a.unidade_id
     AND b.id > a.id
     AND daterange(a.inicio, a.fim, '[)') && daterange(b.inicio, b.fim, '[)');

  -- Melhor parar com a lista do que falhar com a mensagem genérica do
  -- Postgres: quem for corrigir precisa saber quais contratos olhar.
  IF v_conflitos IS NOT NULL THEN
    RAISE EXCEPTION 'Contratos com períodos sobrepostos; encerre um deles antes de migrar: %', v_conflitos;
  END IF;
END
$$;

ALTER TABLE contratos_locacao
  ADD CONSTRAINT ex_contrato_periodo_unico
  EXCLUDE USING gist (
    condominio_id WITH =,
    unidade_id    WITH =,
    daterange(inicio, fim, '[)') WITH &&
  );

-- Down Migration
ALTER TABLE contratos_locacao DROP CONSTRAINT IF EXISTS ex_contrato_periodo_unico;
DROP TRIGGER IF EXISTS trg_itens_fatura_paga ON fatura_itens;
DROP FUNCTION IF EXISTS proteger_itens_de_fatura_paga();
DROP TRIGGER IF EXISTS trg_fatura_encerrada ON faturas;
DROP FUNCTION IF EXISTS proteger_fatura_encerrada();
DROP FUNCTION IF EXISTS kpis_faturas_por_mes(VARCHAR, INTEGER);
