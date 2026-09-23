package comunicacao.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "chat_mensagens",
        indexes = {
                @Index(name = "idx_chat_remetente", columnList = "remetente_id"),
                @Index(name = "idx_chat_destinatario", columnList = "destinatario_id"),
        })
public class ChatMensagem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /** Ids do auth-api (inteiros) guardados como texto. */
    @NotBlank
    @Column(name = "remetente_id", length = 64)
    private String remetenteId;

    @NotBlank
    @Column(name = "destinatario_id", length = 64)
    private String destinatarioId;

    @Column(name = "`condominioId`")
    private String condominioId;

    @NotBlank
    @Column(columnDefinition = "TEXT")
    private String texto;

    private boolean lida = false;

    @Column(name = "enviado_em")
    private LocalDateTime enviadoEm = LocalDateTime.now();

    @Column(name = "lida_em")
    private LocalDateTime lidaEm;
}
