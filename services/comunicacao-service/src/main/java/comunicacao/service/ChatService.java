package comunicacao.service;

import comunicacao.exception.OperacaoInvalidaException;
import comunicacao.exception.RecursoNaoEncontradoException;
import comunicacao.model.ChatMensagem;
import comunicacao.repository.ChatMensagemRepository;
import comunicacao.security.Autorizacao;
import comunicacao.security.CondominioUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatService {

    private final ChatMensagemRepository chatRepository;

    public ChatMensagem enviar(String remetenteId, String destinatarioId, String texto) {
        // Visitante e terceirizado existem no cadastro para serem registrados na
        // portaria. Conversar pelo sistema não faz parte do acesso deles.
        Autorizacao.exigirAcessoAoSistema();

        if (remetenteId.equals(destinatarioId)) {
            throw new OperacaoInvalidaException("Não é possível enviar mensagem para si mesmo");
        }
        ChatMensagem msg = new ChatMensagem();
        msg.setRemetenteId(remetenteId);
        msg.setDestinatarioId(destinatarioId);
        msg.setCondominioId(CondominioUtils.condominioIdEfetivo());
        msg.setTexto(texto);
        return chatRepository.save(msg);
    }

    public ChatMensagem marcarLida(String id, String leitorId) {
        ChatMensagem msg = buscarPorId(id);
        if (!leitorId.equals(msg.getDestinatarioId())) {
            throw new OperacaoInvalidaException("Apenas o destinatário pode marcar a mensagem como lida");
        }
        if (!msg.isLida()) {
            msg.setLida(true);
            msg.setLidaEm(LocalDateTime.now());
            chatRepository.save(msg);
        }
        return msg;
    }

    public void marcarConversaLida(String destinatarioId, String remetenteId) {
        chatRepository.findConversa(destinatarioId, remetenteId).stream()
                .filter(m -> destinatarioId.equals(m.getDestinatarioId()) && !m.isLida())
                .forEach(m -> {
                    m.setLida(true);
                    m.setLidaEm(LocalDateTime.now());
                });
    }

    @Transactional(readOnly = true)
    public ChatMensagem buscarPorId(String id) {
        return chatRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Mensagem não encontrada com id: " + id));
    }

    @Transactional(readOnly = true)
    public List<ChatMensagem> buscarConversa(String usuarioA, String usuarioB) {
        return chatRepository.findConversa(usuarioA, usuarioB);
    }

    @Transactional(readOnly = true)
    public List<ChatMensagem> listarNaoLidas(String destinatarioId) {
        return chatRepository.findByDestinatarioIdAndLidaFalse(destinatarioId);
    }

    @Transactional(readOnly = true)
    public long contarNaoLidas(String destinatarioId) {
        return chatRepository.countByDestinatarioIdAndLidaFalse(destinatarioId);
    }
}
