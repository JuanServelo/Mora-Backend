package comunicacao.model;

import comunicacao.model.enums.CategoriaArtigo;
import comunicacao.model.enums.StatusPergunta;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

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
    @Schema(description = "Id da pergunta.", example = "a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d", accessMode = Schema.AccessMode.READ_ONLY)
    private UUID id;

    @Column(name = "`condominioId`")
    @Schema(description = "Condomínio de quem perguntou.", example = "cond-parque-verde", accessMode = Schema.AccessMode.READ_ONLY)
    private String condominioId;

    @Column(name = "autor_id", nullable = false, length = 64)
    @Schema(description = "Id de quem perguntou (auth-api).", example = "4")
    private String autorId;

    @Column(name = "autor_nome")
    @Schema(description = "Nome de quem perguntou.", example = "Ana Souza")
    private String autorNome;

    @Column(nullable = false, columnDefinition = "TEXT")
    @Schema(description = "A pergunta.", example = "Posso usar a churrasqueira em dia de semana depois das 22h?")
    private String texto;

    @Enumerated(EnumType.STRING)
    @Schema(description = "Categoria sugerida.", example = "REGRA")
    private CategoriaArtigo categoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Schema(description = "PENDENTE ou RESPONDIDA.", example = "RESPONDIDA")
    private StatusPergunta status = StatusPergunta.PENDENTE;

    @Column(columnDefinition = "TEXT")
    @Schema(description = "Resposta da administração.", example = "Pode, até as 22h30, sem som alto. Depois disso vale o horário de silêncio.")
    private String resposta;

    @Column(name = "respondida_por")
    @Schema(description = "Quem respondeu.", example = "Síndico Residencial Parque Verde")
    private String respondidaPor;

    @Column(name = "respondida_em")
    @Schema(description = "Quando foi respondida.", example = "2026-10-20T18:30:00")
    private LocalDateTime respondidaEm;

    @Column(name = "artigo_id")
    @Schema(description = "Artigo publicado com a resposta, quando ela serve para todos.", example = "c4d2e8f1-5a7b-4c3d-9e1f-2b6a8d0c4e7f")
    private UUID artigoId;

    @Column(name = "criado_em")
    @Schema(description = "Quando foi feita.", example = "2026-10-19T21:10:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime criadoEm = LocalDateTime.now();
}
