package ocorrencias.dto;

import ocorrencias.model.Fornecedor;
import ocorrencias.model.OrdemServico;
import ocorrencias.model.OrdemServicoAndamento;
import ocorrencias.model.Reclamacao;
import ocorrencias.model.enums.PrioridadeOrdemServico;
import ocorrencias.model.enums.StatusOrdemServico;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * A OS como a gestão a vê. `andamentos` vem só no detalhe — na listagem fica
 * nulo, para não carregar o histórico de todas.
 */
public record OrdemServicoResponse(
        Long id,
        String numero,
        String descricao,
        PrioridadeOrdemServico prioridade,
        LocalDate prazo,
        String responsavel,
        FornecedorResumo fornecedor,
        ReclamacaoResumo reclamacao,
        boolean preventiva,
        StatusOrdemServico status,
        String condominioId,
        String abertaPorEmail,
        BigDecimal custo,
        String observacoes,
        LocalDateTime concluidaEm,
        boolean concluidaNoPrazo,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm,
        List<Andamento> andamentos
) {

    public record FornecedorResumo(Long id, String nome, String servico, String telefone) {
        static FornecedorResumo de(Fornecedor f) {
            return f == null ? null : new FornecedorResumo(f.getId(), f.getNome(), f.getServico(), f.getTelefone());
        }
    }

    public record ReclamacaoResumo(Long id, String protocolo, String categoria) {
        static ReclamacaoResumo de(Reclamacao r) {
            return r == null ? null : new ReclamacaoResumo(r.getId(), r.getProtocolo(), r.getCategoria());
        }
    }

    public record Andamento(
            Long id,
            String mensagem,
            StatusOrdemServico statusAnterior,
            StatusOrdemServico statusNovo,
            String autorEmail,
            boolean sistema,
            LocalDateTime criadoEm
    ) {
        static Andamento de(OrdemServicoAndamento a) {
            return new Andamento(a.getId(), a.getMensagem(), a.getStatusAnterior(), a.getStatusNovo(),
                    a.getAutorEmail(), a.getAutorId() == null, a.getCriadoEm());
        }
    }

    public static OrdemServicoResponse resumo(OrdemServico o) {
        return de(o, null);
    }

    public static OrdemServicoResponse detalhe(OrdemServico o) {
        return de(o, o.getAndamentos().stream().map(Andamento::de).toList());
    }

    private static OrdemServicoResponse de(OrdemServico o, List<Andamento> andamentos) {
        return new OrdemServicoResponse(
                o.getId(),
                o.getNumero(),
                o.getDescricao(),
                o.getPrioridade(),
                o.getPrazo(),
                o.getResponsavel(),
                FornecedorResumo.de(o.getFornecedor()),
                ReclamacaoResumo.de(o.getReclamacao()),
                o.isPreventiva(),
                o.getStatus(),
                o.getCondominioId(),
                o.getAbertaPorEmail(),
                o.getCusto(),
                o.getObservacoes(),
                o.getConcluidaEm(),
                o.isConcluidaNoPrazo(),
                o.getCriadoEm(),
                o.getAtualizadoEm(),
                andamentos
        );
    }
}
