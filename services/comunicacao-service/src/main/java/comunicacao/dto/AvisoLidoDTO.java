package comunicacao.dto;

import comunicacao.model.Aviso;
import comunicacao.model.enums.PublicoAlvo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Um aviso, do ponto de vista de quem o recebe.
 *
 * Carrega todos os campos do aviso mais o que este usuário já fez com ele. A
 * junção acontece aqui, e não na tela: o aviso vem de uma tabela e a leitura de
 * outra, e se cada tela cruzasse por conta própria cada uma cruzaria de um
 * jeito — a lista do morador e o popup da tela inicial já discordariam entre si.
 *
 * É um superconjunto da entidade, então quem consumia `GET /avisos/ativos`
 * esperando o aviso cru continua encontrando todos os campos onde estavam.
 */
public record AvisoLidoDTO(
        UUID id,
        String titulo,
        String mensagem,
        LocalDate dataInicio,
        LocalDate dataFim,
        PublicoAlvo publicoAlvo,
        String condominioId,
        String autor,
        String imagemUrl,
        boolean publicado,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm,
        boolean lido,
        LocalDateTime lidoEm
) {

    public static AvisoLidoDTO de(Aviso aviso, LocalDateTime lidoEm) {
        return new AvisoLidoDTO(
                aviso.getId(),
                aviso.getTitulo(),
                aviso.getMensagem(),
                aviso.getDataInicio(),
                aviso.getDataFim(),
                aviso.getPublicoAlvo(),
                aviso.getCondominioId(),
                aviso.getAutor(),
                aviso.getImagemUrl(),
                aviso.isPublicado(),
                aviso.getCriadoEm(),
                aviso.getAtualizadoEm(),
                lidoEm != null,
                lidoEm
        );
    }
}
