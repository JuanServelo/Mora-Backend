package portaria.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import portaria.model.enums.TipoVaga;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "vagas_estacionamento")
public class Vaga {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotBlank(message = "Número da vaga é obrigatório")
    @Column(unique = true)
    private String numero;

    private String localizacao;

    @NotNull(message = "Tipo da vaga é obrigatório")
    @Enumerated(EnumType.STRING)
    private TipoVaga tipo;

    private boolean ativa = true;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "apartamento_id")
    private Apartamento apartamento;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm = LocalDateTime.now();
}
