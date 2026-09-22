package portaria.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import portaria.model.enums.StatusReserva;
import portaria.model.enums.TipoReserva;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "reservas")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /** Quem registrou a reserva no sistema (id do auth-api, inteiro em texto). */
    @NotNull
    @Column(name = "solicitante_id")
    private String solicitanteId;

    @Column(name = "`condominioId`")
    private String condominioId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "area_comum_id")
    private AreaComum areaComum;

    /**
     * Intervalo da reserva. Data e hora juntas: separá-las obrigaria toda
     * comparação de conflito e de janela a remontar o instante, e foi o que
     * fazia a checagem antiga ignorar o horário.
     */
    @NotNull
    private LocalDateTime inicio;

    @NotNull
    private LocalDateTime fim;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_reserva", nullable = false)
    private TipoReserva tipoReserva = TipoReserva.MORADOR;

    /** Preenchida apenas em reserva de morador; nula em evento do condomínio. */
    @Column(name = "unidade_id")
    private String unidadeId;

    /** Responsável pela reserva — morador da unidade ou funcionário do evento. */
    @Column(name = "responsavel_id")
    private String responsavelId;

    @Column(name = "responsavel_nome")
    private String responsavelNome;

    @Column(name = "responsavel_perfil")
    private String responsavelPerfil;

    @Column(name = "descricao_evento", length = 500)
    private String descricaoEvento;

    @Column(precision = 10, scale = 2)
    private BigDecimal valorTotal;

    @Enumerated(EnumType.STRING)
    private StatusReserva status = StatusReserva.PENDENTE;

    @Column(name = "motivo_recusa", length = 500)
    private String motivoRecusa;

    /** Quem confirmou a reserva, e quando. */
    @Column(name = "aprovado_por_id")
    private String aprovadoPorId;

    @Column(name = "aprovado_por_nome")
    private String aprovadoPorNome;

    @Column(name = "aprovado_em")
    private LocalDateTime aprovadoEm;

    /**
     * Quem encerrou a reserva, e quando. Preenchido tanto no cancelamento
     * quanto na recusa: nos dois casos a pergunta do histórico é a mesma —
     * quem tirou esta reserva do ar.
     */
    @Column(name = "cancelado_por_id")
    private String canceladoPorId;

    @Column(name = "cancelado_por_nome")
    private String canceladoPorNome;

    @Column(name = "cancelado_em")
    private LocalDateTime canceladoEm;

    @Column(length = 500)
    private String observacoes;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm = LocalDateTime.now();

    /** Cancelada e recusada não ocupam o horário (RN-06). */
    public boolean ocupaHorario() {
        return status == StatusReserva.PENDENTE
                || status == StatusReserva.APROVADA
                || status == StatusReserva.CONCLUIDA;
    }
}
