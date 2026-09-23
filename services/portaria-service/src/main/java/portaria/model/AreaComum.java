package portaria.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import portaria.model.enums.ModoFuncionamento;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "areas_comuns", uniqueConstraints = {
    @UniqueConstraint(name = "areas_comuns_nome_condominioId_key", columnNames = {"nome", "condominioId"})
})
public class AreaComum {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotBlank(message = "Nome da área comum é obrigatório")
    private String nome;

    @Column(name = "`condominioId`")
    private String condominioId;

    @NotBlank(message = "Tipo é obrigatório")
    private String tipo;

    private String descricao;

    private String localizacao;

    @Column(name = "`capacidadeMaxima`")
    private Integer capacidadeMaxima;

    private Double area;

    @Column(name = "`podeReservar`")
    private boolean podeReservar = false;

    private Double taxaLocacao;

    private String informacoesLimpeza;

    private String politicaCancelamento;

    private String observacoes;

    /**
     * RN-09. Espaços anteriores à funcionalidade migram para SEM_RESTRICAO:
     * impor uma janela poderia invalidar reservas futuras já existentes.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "modo_funcionamento", nullable = false)
    private ModoFuncionamento modoFuncionamento = ModoFuncionamento.SEM_RESTRICAO;

    /** Marca os espaços migrados automaticamente, para revisão da gestão. */
    @Column(name = "funcionamento_a_revisar")
    private boolean funcionamentoARevisar = false;

    /**
     * RN-04. Falso por padrão para os espaços já existentes continuarem com o
     * comportamento atual — toda reserva nasce confirmada. Exigir aprovação em
     * tudo colocaria o síndico no caminho de cada churrasqueira de sábado.
     */
    @Column(name = "exige_aprovacao", nullable = false)
    private boolean exigeAprovacao = false;

    private boolean ativo = true;

    @Column(name = "`criadoEm`")
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "`atualizadoEm`")
    private LocalDateTime atualizadoEm = LocalDateTime.now();
}
