package comunicacao.dto;

import comunicacao.model.Aviso;
import comunicacao.model.enums.PublicoAlvo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Aviso como o frontend precisa dele.
 *
 * A entidade sozinha não basta: o morador precisa saber se já leu, e o síndico
 * precisa do indicador de leitura na própria linha da lista.
 */
public record AvisoResponse(
        UUID id,
        String titulo,
        String mensagem,
        LocalDate dataInicio,
        LocalDate dataFim,
        PublicoAlvo publicoAlvo,
        String condominioId,
        String autor,
        boolean publicado,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm,
        /** Se o usuário que fez a requisição já registrou leitura. */
        boolean lido,
        /** Quantos já leram. Null quando quem consulta não é da administração. */
        Long totalLeituras
) {
    public static AvisoResponse de(Aviso a, boolean lido, Long totalLeituras) {
        return new AvisoResponse(
                a.getId(), a.getTitulo(), a.getMensagem(),
                a.getDataInicio(), a.getDataFim(), a.getPublicoAlvo(),
                a.getCondominioId(), a.getAutor(), a.isPublicado(),
                a.getCriadoEm(), a.getAtualizadoEm(),
                lido, totalLeituras);
    }
}
