package portaria.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import portaria.model.enums.StatusAcesso;
import portaria.model.enums.TipoProprietario;
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

    /** Cliente dono deste registro. Todo dado de domínio pertence a um condomínio. */
    @Column(name = "`condominioId`")
    private String condominioId;

    @NotNull(message = "ID do proprietário é obrigatório")
    @Column(name = "proprietario_id")
    private String proprietarioId;

    @NotNull(message = "Tipo de proprietário é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_proprietario")
    private TipoProprietario tipoProprietario;

    @Enumerated(EnumType.STRING)
    private StatusAcesso status;

    @Column(name = "data_entrada")
    private LocalDateTime dataEntrada;

    @Column(name = "data_saida")
    private LocalDateTime dataSaida;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm = LocalDateTime.now();
}
