package comunicacao.dto;

import java.time.LocalDateTime;

/** Uma linha da caixa de entrada. */
public record ConversaResponse(
        UsuarioResumo participante,
        String ultimaMensagem,
        LocalDateTime ultimaEm,
        /** Se a última mensagem foi escrita pelo próprio usuário. */
        boolean ultimaMinha,
        long naoLidas
) {}
