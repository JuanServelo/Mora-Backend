package comunicacao.dto;

import java.time.LocalDateTime;

/** Uma linha da lista "quem já leu". */
public record LeitorResponse(
        String usuarioId,
        String nome,
        String perfil,
        String unidade,
        LocalDateTime lidoEm
) {}
