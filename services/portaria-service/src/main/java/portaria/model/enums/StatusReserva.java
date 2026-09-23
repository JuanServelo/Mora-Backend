package portaria.model.enums;

import java.util.Set;

public enum StatusReserva {
    PENDENTE,
    APROVADA,
    RECUSADA,
    EXPIRADA,
    CANCELADA,
    CONCLUIDA;

    /**
     * Transições aceitas (RN-03). Qualquer outra é rejeitada pelo backend —
     * sem isso, uma recusada poderia virar aprovada por requisição direta.
     */
    private static final java.util.Map<StatusReserva, Set<StatusReserva>> PERMITIDAS =
            java.util.Map.of(
                    PENDENTE, Set.of(APROVADA, RECUSADA, EXPIRADA, CANCELADA),
                    APROVADA, Set.of(CANCELADA, CONCLUIDA),
                    RECUSADA, Set.of(),
                    EXPIRADA, Set.of(),
                    CANCELADA, Set.of(),
                    CONCLUIDA, Set.of()
            );

    public boolean podeIrPara(StatusReserva novo) {
        return PERMITIDAS.getOrDefault(this, Set.of()).contains(novo);
    }

    /** Só estes ocupam o horário: pendente bloqueia até ser resolvida (RN-05). */
    public boolean ocupaHorario() {
        return this == PENDENTE || this == APROVADA || this == CONCLUIDA;
    }
}
