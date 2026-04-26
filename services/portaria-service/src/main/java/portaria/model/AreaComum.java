package portaria.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "areas_comuns")
public class AreaComum {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotBlank(message = "Nome da área comum é obrigatório")
    @Column(unique = true)
    private String nome;

    @NotBlank(message = "Tipo é obrigatório")
    private String tipo;

    private String descricao;

    private String localizacao;

    private Integer capacidadeMaxima;

    private Double area;

    private boolean podeReservar = false;

    private String observacoes;

    private boolean ativo = true;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm = LocalDateTime.now();
}
