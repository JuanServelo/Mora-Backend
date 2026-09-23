package portaria.dto.reserva;

import portaria.model.Reserva;
import portaria.model.enums.StatusReserva;
import portaria.model.enums.TipoReserva;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReservaResponseDTO(
        String id,
        String solicitanteId,
        String condominioId,
        String areaComumId,
        String areaComumNome,
        LocalDateTime inicio,
        LocalDateTime fim,
        TipoReserva tipoReserva,
        String unidadeId,
        String responsavelId,
        String responsavelNome,
        String responsavelPerfil,
        String descricaoEvento,
        BigDecimal valorTotal,
        StatusReserva status,
        String motivoRecusa,
        String observacoes,
        String aprovadoPorNome,
        LocalDateTime aprovadoEm,
        String canceladoPorNome,
        LocalDateTime canceladoEm,
        LocalDateTime criadoEm,
        /** Só para pendentes: quando a solicitação expira se ninguém decidir. */
        LocalDateTime prazoDecisao
) {
    public static ReservaResponseDTO fromEntity(Reserva r) {
        return new ReservaResponseDTO(
                r.getId(),
                r.getSolicitanteId(),
                r.getCondominioId(),
                r.getAreaComum() != null ? r.getAreaComum().getId() : null,
                r.getAreaComum() != null ? r.getAreaComum().getNome() : null,
                r.getInicio(),
                r.getFim(),
                r.getTipoReserva(),
                r.getUnidadeId(),
                r.getResponsavelId(),
                r.getResponsavelNome(),
                r.getResponsavelPerfil(),
                r.getDescricaoEvento(),
                r.getValorTotal(),
                r.getStatus(),
                r.getMotivoRecusa(),
                r.getObservacoes(),
                r.getAprovadoPorNome(),
                r.getAprovadoEm(),
                r.getCanceladoPorNome(),
                r.getCanceladoEm(),
                r.getCriadoEm(),
                // A regra do prazo mora no service: recalculá-la na tela faria
                // as duas divergirem na primeira mudança de PRAZO_APROVACAO.
                r.getStatus() == StatusReserva.PENDENTE
                        ? portaria.service.ReservaService.prazoDecisao(r)
                        : null
        );
    }
}
