package portaria.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import portaria.model.enums.StatusAcesso;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "visitantes")
public class Visitante {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotBlank(message = "Nome é obrigatório")
    private String nome;

    private String documento;

    @Column(name = "motivo_visita")
    private String motivoVisita;

    @Enumerated(EnumType.STRING)
    private StatusAcesso status;

    @Column(name = "data_entrada")
    private LocalDateTime dataEntrada;

    @Column(name = "data_saida")
    private LocalDateTime dataSaida;
}
