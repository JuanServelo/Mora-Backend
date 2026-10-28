package ocorrencias.service;

import lombok.RequiredArgsConstructor;
import ocorrencias.service.EstatisticasReclamacoes.Contagem;
import ocorrencias.service.EstatisticasReclamacoes.Resultado;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Os números que os painéis consomem, agregados em SQL (RNF-20).
 *
 * As datas são gravadas no horário do condomínio (o container roda com
 * `TZ=America/Sao_Paulo`), então o mês de uma ocorrência é o `date_trunc` da
 * própria coluna — uma aberta às 22h do dia 31 é daquele mês.
 *
 * `condominioId` nulo significa "todos" — só o Admin Geral chega aqui assim;
 * o recorte é decidido antes, em `Autorizacao`.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EstatisticasService {

    private final NamedParameterJdbcTemplate jdbc;

    public Resultado reclamacoes(String condominioId, Integer meses) {
        List<String> janela = EstatisticasReclamacoes.mesesDaJanela(meses, LocalDate.now(EstatisticasReclamacoes.FUSO));
        String filtro = condominioId == null ? "" : " AND condominio_id = :condominioId";
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("condominioId", condominioId)
                .addValue("inicio", LocalDate.parse(janela.get(0) + "-01").atStartOfDay());

        Map<String, Object> atual = jdbc.queryForMap("""
                SELECT COUNT(*) FILTER (WHERE status = 'PENDENTE')   AS pendentes,
                       COUNT(*) FILTER (WHERE status = 'EM_ANALISE') AS em_analise,
                       MIN(criado_em) FILTER (WHERE status <> 'RESOLVIDO') AS mais_antiga
                  FROM reclamacoes
                 WHERE TRUE""" + filtro, p);

        Map<String, Long> abertasPorMes = porChave("""
                SELECT to_char(date_trunc('month', criado_em), 'YYYY-MM') AS chave, COUNT(*) AS total
                  FROM reclamacoes
                 WHERE criado_em >= :inicio""" + filtro + " GROUP BY 1", p);

        Map<String, Long> resolvidasPorMes = porChave("""
                SELECT to_char(date_trunc('month', resolvido_em), 'YYYY-MM') AS chave, COUNT(*) AS total
                  FROM reclamacoes
                 WHERE status = 'RESOLVIDO' AND resolvido_em >= :inicio""" + filtro + " GROUP BY 1", p);

        Map<String, Long> porCategoria = porChave("""
                SELECT categoria AS chave, COUNT(*) AS total
                  FROM reclamacoes
                 WHERE criado_em >= :inicio""" + filtro + " GROUP BY categoria", p);

        Double tempoMedio = jdbc.queryForObject("""
                SELECT AVG(EXTRACT(EPOCH FROM (resolvido_em - criado_em)) / 86400)
                  FROM reclamacoes
                 WHERE status = 'RESOLVIDO'
                   AND resolvido_em >= criado_em
                   AND resolvido_em >= :inicio""" + filtro, p, Double.class);

        Timestamp maisAntiga = (Timestamp) atual.get("mais_antiga");
        return EstatisticasReclamacoes.montar(
                janela,
                ((Number) atual.get("pendentes")).longValue(),
                ((Number) atual.get("em_analise")).longValue(),
                maisAntiga == null ? null : maisAntiga.toLocalDateTime(),
                abertasPorMes,
                resolvidasPorMes,
                porCategoria,
                tempoMedio
        );
    }

    /**
     * Ocorrências por status, para o painel da plataforma.
     *
     * `condominioIds` nulo = sem recorte; vazio = nenhum condomínio passou no
     * filtro, e a resposta é zero — confundir os dois faria um filtro sem
     * resultado exibir a plataforma inteira.
     */
    public Contagem contagem(Collection<String> condominioIds) {
        if (condominioIds != null && condominioIds.isEmpty()) {
            return EstatisticasReclamacoes.contagem(Map.of());
        }
        String filtro = condominioIds == null ? "" : " WHERE condominio_id IN (:ids)";
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("ids", condominioIds);
        return EstatisticasReclamacoes.contagem(
                porChave("SELECT status AS chave, COUNT(*) AS total FROM reclamacoes" + filtro + " GROUP BY status", p));
    }

    /** Indicadores das ordens de serviço (RF-24): no prazo e críticas atrasadas. */
    public record IndicadoresOrdemServico(
            long abertas,
            long emExecucao,
            long atrasadas,
            long criticasAtrasadas,
            long concluidas,
            long concluidasNoPrazo,
            Integer percentualNoPrazo
    ) {}

    public IndicadoresOrdemServico ordensServico(String condominioId) {
        String filtro = condominioId == null ? "" : " WHERE condominio_id = :condominioId";
        Map<String, Object> l = jdbc.queryForMap("""
                SELECT COUNT(*) FILTER (WHERE status = 'ABERTA')      AS abertas,
                       COUNT(*) FILTER (WHERE status = 'EM_EXECUCAO') AS em_execucao,
                       COUNT(*) FILTER (WHERE status = 'ATRASADA')    AS atrasadas,
                       COUNT(*) FILTER (WHERE status = 'ATRASADA' AND prioridade = 'CRITICA') AS criticas_atrasadas,
                       COUNT(*) FILTER (WHERE status = 'CONCLUIDA')   AS concluidas,
                       COUNT(*) FILTER (WHERE status = 'CONCLUIDA' AND concluida_em::date <= prazo) AS no_prazo
                  FROM ordens_servico""" + filtro,
                new MapSqlParameterSource("condominioId", condominioId));

        long concluidas = num(l, "concluidas");
        long noPrazo = num(l, "no_prazo");
        return new IndicadoresOrdemServico(
                num(l, "abertas"),
                num(l, "em_execucao"),
                num(l, "atrasadas"),
                num(l, "criticas_atrasadas"),
                concluidas,
                noPrazo,
                concluidas == 0 ? null : (int) Math.round(noPrazo * 100.0 / concluidas)
        );
    }

    private Map<String, Long> porChave(String sql, MapSqlParameterSource p) {
        Map<String, Long> mapa = new LinkedHashMap<>();
        jdbc.query(sql, p, rs -> {
            mapa.put(rs.getString("chave"), rs.getLong("total"));
        });
        return mapa;
    }

    private static long num(Map<String, Object> linha, String campo) {
        Object v = linha.get(campo);
        return v == null ? 0 : ((Number) v).longValue();
    }
}
