package comunicacao.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import comunicacao.model.enums.TipoNotificacao;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "notificacoes",
        indexes = {
                @Index(name = "idx_notificacoes_destinatario", columnList = "destinatario_id"),
                @Index(name = "idx_notificacoes_lida", columnList = "destinatario_id, lida"),
        })
public class Notificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /** Id do usuário no auth-api, que é um inteiro — guardado como texto. */
    @NotBlank
    @Column(name = "destinatario_id", length = 64)
    private String destinatarioId;

    @Column(name = "`condominioId`")
    private String condominioId;

    @Enumerated(EnumType.STRING)
    private TipoNotificacao tipo;

    @NotBlank
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String mensagem;

    /**
     * Id do que originou a notificação — o aviso, a conversa, a encomenda.
     * É o que deixa o clique levar ao item de origem em vez de só à lista.
     */
    @Column(name = "referencia_id")
    private String referenciaId;

    private boolean lida = false;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "lida_em")
    private LocalDateTime lidaEm;
}
