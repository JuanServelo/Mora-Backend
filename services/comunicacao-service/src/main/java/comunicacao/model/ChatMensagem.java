package comunicacao.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Entity
@Table(name = "chat_mensagens")
public class ChatMensagem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Schema(description = "Id da mensagem.", example = "8e1d4c7a-2b9f-4a3e-b5d6-0c7f1e2a9b84")
    private String id;

    @NotNull

    /**
     * O id de quem, como texto.
     *
     * Não é UUID: o `auth-api` numera usuários com `integer` autoincremental, e
     * era isso que o token trazia — `UUID.fromString("32")` derrubava com 500
     * toda rota que precisa saber quem está pedindo. `VARCHAR` aceita o formato
     * de hoje e sobrevive a uma eventual migração para UUID sem nova alteração
     * de esquema; o preço é não haver tipo forte no banco.
     */
    @Column(name = "remetente_id")
    @Schema(description = "Id de quem enviou (auth-api).", example = "2")
    private String remetenteId;

    @Column(name = "destinatario_id")
    @Schema(description = "Id de quem recebe (auth-api).", example = "4")
    private String destinatarioId;

    @Column(name = "`condominioId`")
    @Schema(description = "Condomínio dos dois.", example = "cond-parque-verde")
    private String condominioId;

    @NotBlank
    @Column(columnDefinition = "TEXT")
    @Schema(description = "Texto da mensagem.", example = "Oi, Ana. A encomenda que chegou ontem já está na portaria.")
    private String texto;

    @Schema(description = "Se o destinatário já leu.", example = "false")
    private boolean lida = false;

    @Column(name = "enviado_em")
    @Schema(description = "Quando foi enviada.", example = "2026-10-20T10:42:00")
    private LocalDateTime enviadoEm = LocalDateTime.now();

    @Column(name = "lida_em")
    @Schema(description = "Quando foi lida.", example = "2026-10-20T11:05:00")
    private LocalDateTime lidaEm;
}
