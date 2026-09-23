package comunicacao.dto;

import comunicacao.model.ChatMensagem;

import java.time.LocalDateTime;

public record MensagemResponse(
        String id,
        String remetenteId,
        String remetenteNome,
        String destinatarioId,
        String texto,
        boolean lida,
        LocalDateTime enviadoEm,
        LocalDateTime lidaEm,
        /** Verdadeiro quando quem consulta é o remetente — o front alinha à direita. */
        boolean minha
) {
    public static MensagemResponse de(ChatMensagem m, String usuarioAtual, String remetenteNome) {
        return new MensagemResponse(
                m.getId(), m.getRemetenteId(), remetenteNome, m.getDestinatarioId(),
                m.getTexto(), m.isLida(), m.getEnviadoEm(), m.getLidaEm(),
                m.getRemetenteId().equals(usuarioAtual));
    }
}
