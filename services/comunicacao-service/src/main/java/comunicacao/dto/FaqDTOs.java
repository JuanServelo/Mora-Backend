package comunicacao.dto;

import comunicacao.model.enums.CategoriaArtigo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Entradas e saídas da avaliação de artigos e das perguntas da FAQ. */
public final class FaqDTOs {

    private FaqDTOs() {}

    public record AvaliacaoRequest(
            @NotNull(message = "Informe se o artigo ajudou") Boolean util
    ) {}

    /** Totais de um artigo e, para quem pediu, o próprio voto (null = não votou). */
    public record AvaliacaoResumo(UUID artigoId, long uteis, long naoUteis, Boolean meuVoto) {}

    public record PerguntaRequest(
            @NotBlank(message = "Escreva a sua pergunta")
            @Size(min = 10, max = 1000, message = "A pergunta deve ter entre 10 e 1000 caracteres")
            String texto,
            CategoriaArtigo categoria
    ) {}

    public record RespostaRequest(
            @NotBlank(message = "Escreva a resposta")
            @Size(max = 5000, message = "A resposta deve ter no máximo 5000 caracteres")
            String resposta,
            boolean publicar,
            @Size(max = 200, message = "O título deve ter no máximo 200 caracteres")
            String titulo,
            CategoriaArtigo categoria
    ) {}
}
