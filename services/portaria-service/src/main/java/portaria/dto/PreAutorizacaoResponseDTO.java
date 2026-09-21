package portaria.dto;

import portaria.model.PreAutorizacao;
import portaria.model.enums.StatusPreAutorizacao;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PreAutorizacaoResponseDTO(
        String id,
        String moradorId,
        String unidadeId,
        String condominioId,
        String nomeVisitante,
        String cpfVisitante,
        String telefoneVisitante,
        portaria.model.enums.TipoVisita tipoPessoa,
        String empresa,
        String placaVeiculo,
        String modeloVeiculo,
        String corVeiculo,
        LocalDate validadeInicio,
        LocalDate validadeFim,
        String observacoes,
        StatusPreAutorizacao status,
        LocalDateTime utilizadaEm,
        LocalDateTime criadoEm
) {
    public static PreAutorizacaoResponseDTO fromEntity(PreAutorizacao p) {
        return new PreAutorizacaoResponseDTO(
                p.getId(),
                p.getMoradorId(),
                p.getUnidadeId(),
                p.getCondominioId(),
                p.getNomeVisitante(),
                p.getCpfVisitante(),
                p.getTelefoneVisitante(),
                p.getTipoPessoa(),
                p.getEmpresa(),
                // visitanteId não é exposto: o morador não deve enxergar a base
                // de visitantes do condomínio (RN-06).
                p.getPlacaVeiculo(),
                p.getModeloVeiculo(),
                p.getCorVeiculo(),
                p.getValidadeInicio(),
                p.getValidadeFim(),
                p.getObservacoes(),
                // statusEfetivo(), não status: o cliente precisa ver EXPIRADA.
                p.statusEfetivo(),
                p.getUtilizadaEm(),
                p.getCriadoEm()
        );
    }
}
