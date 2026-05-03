package vagas.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

@Data
@Entity
@Table(name = "apartamentos")
@JsonIgnoreProperties({"vagas", "moradores", "moradorResponsavel", "hibernateLazyInitializer", "handler"})
public class Apartamento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank(message = "Número do apartamento é obrigatório")
    private String numero;

    @NotNull(message = "Andar é obrigatório")
    private Integer andar;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "`blocoId`", nullable = false)
    private Bloco bloco;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "morador_responsavel_id")
    private Morador moradorResponsavel;

    private Integer quartos;

    @Column(name = "`areaMxComTotal`")
    private Double areaMxComTotal;

    @OneToMany(mappedBy = "apartamento", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Vaga> vagas;

    @OneToMany(mappedBy = "apartamento", fetch = FetchType.LAZY)
    private List<Morador> moradores;

    private String observacoes;

    private boolean ativo = true;

    @Column(name = "`criadoEm`")
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "`atualizadoEm`")
    private LocalDateTime atualizadoEm = LocalDateTime.now();
}

