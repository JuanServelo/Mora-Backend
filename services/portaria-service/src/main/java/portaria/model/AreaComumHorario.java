package portaria.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Janela de funcionamento de uma área comum em um dia da semana (RN-09).
 *
 * `fechamento` anterior a `abertura` não é erro: significa que a janela
 * atravessa a meia-noite (sáb 18:00–02:00 termina no domingo).
 */
@Data
@Entity
@Table(
    name = "areas_comuns_horarios",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_area_dia",
        columnNames = {"area_comum_id", "dia_semana"}
    )
)
public class AreaComumHorario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotNull
    @Column(name = "area_comum_id", nullable = false)
    private String areaComumId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "dia_semana", nullable = false)
    private DayOfWeek diaSemana;

    private boolean fechado = false;

    /** Nulos quando o dia está fechado. */
    private LocalTime abertura;

    private LocalTime fechamento;

    /** Janela que vira o dia: fechamento <= abertura num dia aberto. */
    public boolean atravessaMeiaNoite() {
        return !fechado && abertura != null && fechamento != null
                && !fechamento.isAfter(abertura);
    }
}
