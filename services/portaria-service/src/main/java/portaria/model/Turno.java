package portaria.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "turnos")
public class Turno {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotBlank(message = "Nome do funcionário é obrigatório")
    private String funcionario;

    @NotBlank(message = "Cargo é obrigatório")
    private String cargo;

    @ElementCollection
    @CollectionTable(name = "turno_entradas", joinColumns = @JoinColumn(name = "turno_id"))
    @Column(name = "entrada")
    private List<LocalDateTime> entradas = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "turno_saidas", joinColumns = @JoinColumn(name = "turno_id"))
    @Column(name = "saida")
    private List<LocalDateTime> saidas = new ArrayList<>();
}
