package portaria.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import portaria.model.enums.SituacaoFuncional;
import portaria.model.enums.TipoJornada;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Vida funcional do funcionário: situação e jornada (RF-09).
 *
 * A identidade (nome, CPF, perfil, login) continua sendo do auth-api — aqui só
 * existe o que é da portaria. A chave é o id do usuário no auth-api, o mesmo
 * padrão usado no vínculo de veículos, que evita um segundo cadastro da mesma
 * pessoa podendo divergir do primeiro.
 */
@Data
@Entity
@Table(
    name = "funcionario_jornadas",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_jornada_funcionario",
        columnNames = {"auth_user_id", "condominioId"}
    )
)
public class FuncionarioJornada {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /** Id do usuário no auth-api (inteiro em texto). */
    @NotBlank
    @Column(name = "auth_user_id", nullable = false)
    private String authUserId;

    /** Cópia do nome, para mensagens e histórico não dependerem de outra base. */
    @Column(name = "nome")
    private String nome;

    @Column(name = "`condominioId`")
    private String condominioId;

    @NotNull
    @Enumerated(EnumType.STRING)
    private SituacaoFuncional situacao = SituacaoFuncional.ATIVO;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_jornada")
    private TipoJornada tipoJornada = TipoJornada.SEMANAL_FIXA;

    /** Âncora do cálculo da escala; nulo na jornada semanal. */
    @Column(name = "ciclo_inicio")
    private LocalDate cicloInicio;

    /** Tamanho do ciclo em dias (2 a 31); nulo na jornada semanal. */
    @Column(name = "ciclo_tamanho")
    private Integer cicloTamanho;

    @Column(length = 500)
    private String observacoes;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm = LocalDateTime.now();
}
