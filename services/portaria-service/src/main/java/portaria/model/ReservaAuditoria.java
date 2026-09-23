package portaria.model;

import jakarta.persistence.*;
import lombok.Data;
import portaria.model.enums.StatusReserva;

import java.time.LocalDateTime;

/**
 * Trilha das decisões sobre reservas (RN-10).
 *
 * Aprovação, recusa, cancelamento e expiração mudam o direito de alguém usar um
 * espaço, então cada transição fica registrada com autor, data e motivo.
 * Só append: não há endpoint de edição nem de exclusão, de propósito.
 */
@Data
@Entity
@Table(name = "reserva_auditoria")
public class ReservaAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "reserva_id", nullable = false)
    private String reservaId;

    @Column(name = "`condominioId`")
    private String condominioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior")
    private StatusReserva statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_novo")
    private StatusReserva statusNovo;

    @Column(length = 500)
    private String motivo;

    @Column(name = "autor_id")
    private String autorId;

    @Column(name = "autor_nome")
    private String autorNome;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm = LocalDateTime.now();
}
