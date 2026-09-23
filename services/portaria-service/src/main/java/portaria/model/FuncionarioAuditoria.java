package portaria.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Trilha de auditoria da vida funcional (RN-08).
 *
 * Alterações de situação, de jornada e liberações excepcionais são passíveis de
 * contestação, então cada uma fica registrada com o valor anterior, o novo,
 * quem alterou e quando.
 *
 * Só há escrita por append: não existe endpoint de edição nem de exclusão, de
 * propósito — uma trilha que pode ser corrigida não serve como trilha.
 */
@Data
@Entity
@Table(name = "funcionario_auditoria")
public class FuncionarioAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "auth_user_id", nullable = false)
    private String authUserId;

    @Column(name = "funcionario_nome")
    private String funcionarioNome;

    @Column(name = "`condominioId`")
    private String condominioId;

    /** O que mudou: "situacao", "jornada", "liberacao_excepcional". */
    @Column(nullable = false)
    private String campo;

    @Column(name = "valor_anterior", length = 1000)
    private String valorAnterior;

    @Column(name = "valor_novo", length = 1000)
    private String valorNovo;

    @Column(name = "autor_id")
    private String autorId;

    @Column(name = "autor_nome")
    private String autorNome;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm = LocalDateTime.now();
}
