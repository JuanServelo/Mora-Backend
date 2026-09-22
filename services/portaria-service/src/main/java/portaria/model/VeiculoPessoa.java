package portaria.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Vínculo entre um veículo e uma pessoa da unidade (RN-06).
 *
 * O nome é gravado como cópia, não como referência: o dono do dado é o auth-api,
 * e os registros de acesso já gravados devem continuar exibindo quem era o
 * condutor na época, mesmo que o vínculo mude ou a pessoa saia do condomínio.
 */
@Data
@Entity
@Table(
    name = "veiculo_pessoas",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_veiculo_pessoa",
        columnNames = {"veiculo_id", "pessoa_id"}
    )
)
public class VeiculoPessoa {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotBlank
    @Column(name = "veiculo_id", nullable = false)
    private String veiculoId;

    /** Id do usuário no auth-api (inteiro em texto). */
    @NotBlank
    @Column(name = "pessoa_id", nullable = false)
    private String pessoaId;

    @Column(name = "pessoa_nome")
    private String pessoaNome;

    /** Unidade na qual o vínculo foi criado — base do escopo do morador. */
    @Column(name = "unidade_id")
    private String unidadeId;

    @Column(name = "`condominioId`")
    private String condominioId;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm = LocalDateTime.now();
}
