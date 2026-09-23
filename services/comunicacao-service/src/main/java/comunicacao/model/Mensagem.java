package comunicacao.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "mensagens")
public class Mensagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversa_id", nullable = false)
    private Long conversaId;

    @Column(name = "autor_id", nullable = false, length = 64)
    private String autorId;

    /**
     * O perfil de quem escreveu, no momento em que escreveu.
     *
     * Guardado junto, e não consultado depois: o autor pode deixar a gestão em
     * seguida, e a mensagem precisa continuar dizendo em que papel foi escrita.
     */
    @Column(name = "autor_perfil", nullable = false, length = 20)
    private String autorPerfil;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String corpo;

    /**
     * Remoção é lógica.
     *
     * Apagar de verdade destruiria o registro da conversa para o outro lado —
     * a pessoa veria o fio mudar de sentido sem explicação. Removida, a
     * mensagem continua na conversa, sem o texto.
     */
    @Column(name = "removida_em")
    private LocalDateTime removidaEm;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm = LocalDateTime.now();
}
