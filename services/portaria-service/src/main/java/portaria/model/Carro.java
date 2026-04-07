package portaria.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import portaria.model.enums.StatusAcesso;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "carros")
public class Carro {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotBlank(message = "Placa é obrigatória")
    @Column(unique = true)
    private String placa;

    private String modelo;

    private String proprietario;

    @Enumerated(EnumType.STRING)
    private StatusAcesso status;

    @Column(name = "data_entrada")
    private LocalDateTime dataEntrada;

    @Column(name = "data_saida")
    private LocalDateTime dataSaida;
}
