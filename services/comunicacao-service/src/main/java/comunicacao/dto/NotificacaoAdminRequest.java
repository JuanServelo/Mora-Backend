package comunicacao.dto;

import comunicacao.model.enums.TipoNotificacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Envio manual de notificação pela administração. */
public record NotificacaoAdminRequest(
        @NotNull(message = "Informe ao menos um destinatário")
        List<String> destinatarioIds,

        TipoNotificacao tipo,

        @NotBlank(message = "Título é obrigatório")
        String titulo,

        String mensagem,

        String referenciaId
) {}
