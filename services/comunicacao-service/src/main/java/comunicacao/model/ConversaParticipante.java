package comunicacao.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Quem participa de uma conversa, e até quando já leu.
 *
 * **`ultimaLeituraEm` é marca d'água, não linha por mensagem lida.** A contagem
 * de não lidas é `mensagens.criada_em > ultima_leitura_em`. Guardar uma linha
 * por mensagem lida cresceria com o volume de conversa sem responder nada que
 * a marca não responda.
 *
 * Para quem ainda não é participante não há marca, e a comparação trata tudo
 * como não lido — que é o correto: a gestão que nunca respondeu uma conversa
 * da administração tem todas as mensagens pendentes.
 */
@Data
@Entity
@Table(
        name = "conversa_participantes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"conversa_id", "usuario_id"})
)
public class ConversaParticipante {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "conversa_id", nullable = false)
    private Long conversaId;

    @Column(name = "usuario_id", nullable = false, length = 64)
    private String usuarioId;

    @Column(name = "ultima_leitura_em")
    private LocalDateTime ultimaLeituraEm;

    @Column(name = "entrou_em", nullable = false)
    private LocalDateTime entrouEm = LocalDateTime.now();
}
