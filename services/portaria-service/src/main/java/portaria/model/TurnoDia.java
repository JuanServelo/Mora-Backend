package portaria.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Um dia da jornada. Serve aos dois tipos:
 *  - SEMANAL_FIXA usa `diaSemana`
 *  - ESCALA usa `posicao` (1..tamanho do ciclo)
 *
 * Uma tabela só porque a regra de turno é idêntica nos dois casos — inclusive
 * o `fim` anterior ao `inicio`, que significa turno virando a meia-noite.
 */
@Data
@Entity
@Table(name = "funcionario_turno_dias")
public class TurnoDia {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotBlank
    @Column(name = "jornada_id", nullable = false)
    private String jornadaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "dia_semana")
    private DayOfWeek diaSemana;

    @Column(name = "posicao")
    private Integer posicao;

    private boolean folga = false;

    /** Nulos quando é folga. */
    private LocalTime inicio;

    private LocalTime fim;

    /** Turno que vira o dia: fim <= início num dia em serviço. */
    public boolean atravessaMeiaNoite() {
        return !folga && inicio != null && fim != null && !fim.isAfter(inicio);
    }
}
