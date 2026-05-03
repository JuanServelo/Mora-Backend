package portaria.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Representa um período em que o proprietário disponibiliza sua vaga para aluguel (RF10).
 */
@Data
@Entity
@Table(name = "disponibilidade_vagas")
public class DisponibilidadeVaga {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /** ID do morador proprietário da vaga (propagado via header X-User-Id). */
    @NotNull
    @Column(name = "proprietario_id", nullable = false)
    private UUID proprietarioId;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vaga_id", nullable = false)
    private Vaga vaga;

    @NotNull
    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @NotNull
    @Column(name = "data_fim", nullable = false)
    private LocalDate dataFim;

    /** Indica se ainda está ativa (false quando cancelada pelo proprietário). */
    private boolean ativa = true;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm = LocalDateTime.now();
}
