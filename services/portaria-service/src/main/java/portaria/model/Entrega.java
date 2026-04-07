package portaria.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "entregas")
public class Entrega {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotBlank(message = "Nome do entregador é obrigatório")
    @Column(name = "nome_entregador")
    private String nomeEntregador;

    @NotBlank(message = "Destinatário é obrigatório")
    private String destinatario;

    private String recebedor;

    private String descricao;

    @Column(name = "data_recebimento")
    private LocalDateTime dataRecebimento;

    @Column(name = "data_retirada")
    private LocalDateTime dataRetirada;

    private boolean retirada = false;
}
