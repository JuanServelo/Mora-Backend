package vagas.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import vagas.model.enums.ModalidadeAluguel;
import vagas.model.enums.StatusAluguel;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Representa uma solicitação/contrato de aluguel de vaga entre moradores (RF10).
 */
@Data
@Entity
@Table(name = "alugueis_vagas")
public class AluguelVaga {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /** Morador que solicitou o aluguel. */
    @NotNull
    @Column(name = "solicitante_id", nullable = false)
    private UUID solicitanteId;

    /** Proprietário da vaga (dono que disponibilizou). */
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

    @NotNull
    @Enumerated(EnumType.STRING)
    private ModalidadeAluguel modalidade;

    /** Valor total calculado no momento da solicitação. */
    @Column(name = "valor_total", precision = 10, scale = 2)
    private BigDecimal valorTotal;

    /** Penalidade aplicada em caso de cancelamento fora do prazo. */
    @Column(name = "valor_penalidade", precision = 10, scale = 2)
    private BigDecimal valorPenalidade;

    @NotNull
    @Enumerated(EnumType.STRING)
    private StatusAluguel status = StatusAluguel.PENDENTE;

    /** Motivo preenchido na recusa. */
    @Column(name = "motivo_recusa", length = 500)
    private String motivoRecusa;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm = LocalDateTime.now();
}

