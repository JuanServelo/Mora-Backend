package comunicacao.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import comunicacao.model.enums.CategoriaArtigo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Entradas e saídas da avaliação de artigos e das perguntas da FAQ. */
public final class FaqDTOs {

    private FaqDTOs() {}

    public record AvaliacaoRequest(
            @Schema(description = "Se o artigo ajudou.", example = "true")
            @NotNull(message = "Informe se o artigo ajudou") Boolean util
    ) {}

    /** Totais de um artigo e, para quem pediu, o próprio voto (null = não votou). */
    public record AvaliacaoResumo(
            @Schema(description = "Id do artigo.", example = "c4d2e8f1-5a7b-4c3d-9e1f-2b6a8d0c4e7f") UUID artigoId,
            @Schema(description = "Quantos acharam útil.", example = "12") long uteis,
            @Schema(description = "Quantos não acharam útil.", example = "2") long naoUteis,
            @Schema(description = "O voto de quem pediu; nulo se não votou.", example = "true") Boolean meuVoto
    ) {}

    public record PerguntaRequest(
            @NotBlank(message = "Escreva a sua pergunta")
            @Size(min = 10, max = 1000, message = "A pergunta deve ter entre 10 e 1000 caracteres")
            @Schema(description = "A pergunta.", example = "Posso usar a churrasqueira em dia de semana depois das 22h?")
            String texto,
            @Schema(description = "Categoria sugerida (opcional).", example = "REGRA")
            CategoriaArtigo categoria
    ) {}

    public record RespostaRequest(
            @NotBlank(message = "Escreva a resposta")
            @Size(max = 5000, message = "A resposta deve ter no máximo 5000 caracteres")
            @Schema(description = "Resposta da administração.", example = "Pode, até as 22h30, sem som alto. Depois disso vale o horário de silêncio.")
            String resposta,
            @Schema(description = "Publicar a resposta como artigo da FAQ.", example = "true")
            boolean publicar,
            @Size(max = 200, message = "O título deve ter no máximo 200 caracteres")
            @Schema(description = "Título do artigo, se publicar.", example = "Horário da churrasqueira em dia de semana")
            String titulo,
            @Schema(description = "Categoria do artigo, se publicar.", example = "REGRA")
            CategoriaArtigo categoria
    ) {}
}
