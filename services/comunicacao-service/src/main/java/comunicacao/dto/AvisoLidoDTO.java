package comunicacao.dto;

import comunicacao.model.Aviso;
import comunicacao.model.enums.PublicoAlvo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

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
 *
 * `destinatario` diz se o aviso é **para** quem pediu. A gestão recebe todos os
 * avisos do condomínio — precisa ver o que publicou —, mas não é destinatária
 * deles: sem este campo, a tela inicial do síndico listava os próprios
 * comunicados como "não lidos".
 */
public record AvisoLidoDTO(
        @Schema(description = "Id do aviso.", example = "766730a5-0658-475f-89fe-aad79bb287c1")
        UUID id,
        @Schema(description = "Título do comunicado.", example = "Manutenção da caixa d'água")
        String titulo,
        @Schema(description = "Texto do comunicado.", example = "Na quinta-feira, das 8h às 12h, o abastecimento será interrompido para a limpeza semestral da caixa d'água.")
        String mensagem,
        @Schema(description = "Primeiro dia de vigência.", example = "2026-10-20")
        LocalDate dataInicio,
        @Schema(description = "Último dia de vigência.", example = "2026-10-31")
        LocalDate dataFim,
        @Schema(description = "Quem recebe.", example = "MORADORES")
        PublicoAlvo publicoAlvo,
        @Schema(description = "Condomínio do aviso.", example = "cond-parque-verde")
        String condominioId,
        @Schema(description = "Quem publicou.", example = "sindico.verde@mora.test")
        String autor,
        @Schema(description = "Imagem do aviso, se houver.", example = "/uploads/avisos/3f2b9c1e-8a4d-4e7f-b6c2-1d9e0a5f7b3c.jpg")
        String imagemUrl,
        @Schema(description = "Sempre true aqui: só vigentes publicados.", example = "true")
        boolean publicado,
        @Schema(description = "Quando foi criado.", example = "2026-10-18T09:15:00")
        LocalDateTime criadoEm,
        @Schema(description = "Última alteração.", example = "2026-10-18T09:15:00")
        LocalDateTime atualizadoEm,
        @Schema(description = "Se quem pediu já confirmou a leitura.", example = "true")
        boolean lido,
        @Schema(description = "Quando confirmou.", example = "2026-10-19T07:48:12")
        LocalDateTime lidoEm,
        @Schema(description = "Se o aviso é para quem pediu. A gestão recebe todos, com false.", example = "true")
        boolean destinatario
) {

    public static AvisoLidoDTO de(Aviso aviso, LocalDateTime lidoEm, boolean destinatario) {
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
                lidoEm,
                destinatario
        );
    }
}
