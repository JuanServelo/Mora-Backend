package portaria.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "apartamentos")
public class Apartamento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank(message = "Número do apartamento é obrigatório")
    private String numero;

    @NotNull(message = "Andar é obrigatório")
    private Integer andar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bloco_id", nullable = false)
    private Bloco bloco;

    private Integer quartos;

    private Double areaMxComTotal;

    private String observacoes;

    private boolean ativo = true;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm = LocalDateTime.now();
}