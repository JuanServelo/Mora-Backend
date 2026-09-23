package comunicacao.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "chat_mensagens")
public class ChatMensagem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
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
    private String remetenteId;

    @Column(name = "destinatario_id")
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
