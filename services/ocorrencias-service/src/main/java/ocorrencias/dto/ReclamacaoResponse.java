package ocorrencias.dto;

import ocorrencias.model.OrdemServico;
import ocorrencias.model.Reclamacao;
import ocorrencias.model.ReclamacaoInteracao;
import ocorrencias.model.enums.PrioridadeOrdemServico;
import ocorrencias.model.enums.StatusOrdemServico;
import ocorrencias.model.enums.StatusReclamacao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * A ocorrência como a tela a vê: com o histórico de atendimento e as ordens de
 * serviço vinculadas.
 *
 * `usuario` só vem para a gestão (nome e e-mail de quem abriu). O e-mail de
 * quem respondeu também: para o morador, quem responde é "a administração".
 */
public record ReclamacaoResponse(
        Long id,
        String protocolo,
        String categoria,
        String descricao,
        String anexoUrl,
        StatusReclamacao status,
        String condominioId,
        String usuarioId,
        Usuario usuario,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm,
        LocalDateTime resolvidoEm,
        List<Interacao> interacoes,
        List<OrdemVinculada> ordensServico
) {

    public record Usuario(String nome, String email) {}

    public record Interacao(
            Long id,
            String mensagem,
            StatusReclamacao statusAnterior,
            StatusReclamacao statusNovo,
            String autorPerfil,
            String autorEmail,
            LocalDateTime criadoEm
    ) {
        static Interacao de(ReclamacaoInteracao i, boolean visaoDeGestao) {
            return new Interacao(i.getId(), i.getMensagem(), i.getStatusAnterior(), i.getStatusNovo(),
                    i.getAutorPerfil(), visaoDeGestao ? i.getAutorEmail() : null, i.getCriadoEm());
        }
    }

    public record OrdemVinculada(
            Long id,
            String numero,
            StatusOrdemServico status,
            PrioridadeOrdemServico prioridade,
            LocalDate prazo
    ) {
        public static OrdemVinculada de(OrdemServico o) {
            return new OrdemVinculada(o.getId(), o.getNumero(), o.getStatus(), o.getPrioridade(), o.getPrazo());
        }
    }

    public static ReclamacaoResponse de(Reclamacao r, Usuario usuario, List<OrdemVinculada> ordens,
                                        boolean visaoDeGestao) {
        return new ReclamacaoResponse(
                r.getId(),
                r.getProtocolo(),
                r.getCategoria(),
                r.getDescricao(),
                r.getAnexoUrl(),
                r.getStatus(),
                r.getCondominioId(),
                r.getUsuarioId(),
                visaoDeGestao ? usuario : null,
                r.getCriadoEm(),
                r.getAtualizadoEm(),
                r.getResolvidoEm(),
                r.getInteracoes().stream().map(i -> Interacao.de(i, visaoDeGestao)).toList(),
                ordens == null ? List.of() : ordens
        );
    }
}
