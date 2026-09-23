package comunicacao.model;

import comunicacao.model.enums.CategoriaArtigo;
import comunicacao.model.enums.StatusPergunta;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Dúvida que o morador não encontrou na FAQ e mandou para a administração.
 *
 * A resposta fica aqui mesmo, visível para quem perguntou. Quando a dúvida
 * serve para todos, a administração também a publica como artigo, e
 * `artigoId` aponta para ele.
 */
@Data
@Entity
@Table(name = "faq_perguntas")
public class PerguntaFaq {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "`condominioId`")
    private String condominioId;

    @Column(name = "autor_id", nullable = false, length = 64)
    private String autorId;

    @Column(name = "autor_nome")
    private String autorNome;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String texto;

    @Enumerated(EnumType.STRING)
    private CategoriaArtigo categoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusPergunta status = StatusPergunta.PENDENTE;

    @Column(columnDefinition = "TEXT")
    private String resposta;

    @Column(name = "respondida_por")
    private String respondidaPor;

    @Column(name = "respondida_em")
    private LocalDateTime respondidaEm;

    @Column(name = "artigo_id")
    private UUID artigoId;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm = LocalDateTime.now();
}
