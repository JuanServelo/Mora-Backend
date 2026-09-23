package comunicacao.service;

import comunicacao.client.AuthApiClient;
import comunicacao.dto.ConversaResponse;
import comunicacao.dto.MensagemResponse;
import comunicacao.dto.UsuarioResumo;
import comunicacao.exception.OperacaoInvalidaException;
import comunicacao.exception.RecursoNaoEncontradoException;
import comunicacao.model.ChatMensagem;
import comunicacao.model.enums.TipoNotificacao;
import comunicacao.repository.ChatMensagemRepository;
import comunicacao.security.AcessoNegadoException;
import comunicacao.security.CondominioUtils;
import comunicacao.security.PerfilUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Conversa direta entre morador e administração.
 *
 * Serve para assuntos pontuais que não justificam abrir uma ocorrência formal.
 * Por decisão de escopo o chat é morador↔administração: um dos dois lados
 * precisa ser síndico, admin ou porteiro.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ChatService {

    private final ChatMensagemRepository chatRepository;
    private final AuthApiClient authApi;
    private final NotificacaoService notificacaoService;

    public MensagemResponse enviar(String remetenteId, String destinatarioId, String texto) {
        if (remetenteId.equals(destinatarioId)) {
            throw new OperacaoInvalidaException("Não é possível enviar mensagem para si mesmo");
        }

        UsuarioResumo destinatario = authApi.buscarPorId(destinatarioId);
        if (destinatario == null || destinatario.perfil() == null) {
            throw new RecursoNaoEncontradoException("Destinatário não encontrado no condomínio.");
        }
        exigirParticipacaoDaAdministracao(destinatario.perfil());

        ChatMensagem msg = new ChatMensagem();
        msg.setRemetenteId(remetenteId);
        msg.setDestinatarioId(destinatarioId);
        msg.setCondominioId(CondominioUtils.condominioIdEfetivo());
        msg.setTexto(texto.trim());
        ChatMensagem salva = chatRepository.save(msg);

        UsuarioResumo remetente = authApi.buscarPorId(remetenteId);
        String nomeRemetente = remetente == null ? "Alguém" : remetente.nome();
        notificacaoService.criar(
                destinatarioId,
                msg.getCondominioId(),
                TipoNotificacao.CHAT,
                "Nova mensagem de " + nomeRemetente,
                resumir(msg.getTexto()),
                remetenteId);

        return MensagemResponse.de(salva, remetenteId, nomeRemetente);
    }

    /**
     * O chat liga morador e administração. Se nem quem envia nem quem recebe
     * opera o condomínio, a conversa seria morador↔morador — fora do escopo
     * definido para o requisito.
     */
    private void exigirParticipacaoDaAdministracao(String perfilDestinatario) {
        if (PerfilUtils.isAdministracao(PerfilUtils.perfilAtual())) return;
        if (PerfilUtils.isAdministracao(perfilDestinatario)) return;
        throw new AcessoNegadoException(
                "O chat é entre morador e administração. Escolha um síndico ou a portaria.");
    }

    public MensagemResponse marcarLida(String id, String leitorId) {
        ChatMensagem msg = chatRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Mensagem não encontrada com id: " + id));
        if (!leitorId.equals(msg.getDestinatarioId())) {
            throw new AcessoNegadoException("Apenas o destinatário pode marcar a mensagem como lida");
        }
        if (!msg.isLida()) {
            msg.setLida(true);
            msg.setLidaEm(LocalDateTime.now());
            chatRepository.save(msg);
        }
        return MensagemResponse.de(msg, leitorId, null);
    }

    public void marcarConversaLida(String usuarioId, String outroUsuarioId) {
        LocalDateTime agora = LocalDateTime.now();
        chatRepository.findConversa(usuarioId, outroUsuarioId).stream()
                .filter(m -> usuarioId.equals(m.getDestinatarioId()) && !m.isLida())
                .forEach(m -> {
                    m.setLida(true);
                    m.setLidaEm(agora);
                });
    }

    @Transactional(readOnly = true)
    public List<MensagemResponse> buscarConversa(String usuarioId, String outroUsuarioId) {
        List<ChatMensagem> mensagens = chatRepository.findConversa(usuarioId, outroUsuarioId);
        Map<String, UsuarioResumo> porId = authApi.buscarPorIds(List.of(usuarioId, outroUsuarioId));
        return mensagens.stream()
                .map(m -> {
                    UsuarioResumo autor = porId.get(m.getRemetenteId());
                    return MensagemResponse.de(m, usuarioId, autor == null ? null : autor.nome());
                })
                .toList();
    }

    /**
     * Caixa de entrada: uma linha por interlocutor, com a última mensagem e
     * quantas faltam ler. O agrupamento acontece aqui porque a consulta já
     * traz o histórico do usuário ordenado — a primeira vez que um
     * interlocutor aparece é, por construção, a mensagem mais recente dele.
     */
    @Transactional(readOnly = true)
    public List<ConversaResponse> listarConversas(String usuarioId) {
        List<ChatMensagem> mensagens = chatRepository.findEnvolvendo(usuarioId);
        if (mensagens.isEmpty()) return List.of();

        Map<String, ChatMensagem> ultimaPorOutro = new LinkedHashMap<>();
        Map<String, Long> naoLidasPorOutro = new HashMap<>();

        for (ChatMensagem m : mensagens) {
            String outro = m.getRemetenteId().equals(usuarioId) ? m.getDestinatarioId() : m.getRemetenteId();
            ultimaPorOutro.putIfAbsent(outro, m);
            if (usuarioId.equals(m.getDestinatarioId()) && !m.isLida()) {
                naoLidasPorOutro.merge(outro, 1L, Long::sum);
            }
        }

        Map<String, UsuarioResumo> porId = authApi.buscarPorIds(ultimaPorOutro.keySet());

        return ultimaPorOutro.entrySet().stream()
                .map(e -> {
                    ChatMensagem ultima = e.getValue();
                    return new ConversaResponse(
                            porId.getOrDefault(e.getKey(), UsuarioResumo.desconhecido(e.getKey())),
                            resumir(ultima.getTexto()),
                            ultima.getEnviadoEm(),
                            ultima.getRemetenteId().equals(usuarioId),
                            naoLidasPorOutro.getOrDefault(e.getKey(), 0L));
                })
                .toList();
    }

    /** Com quem este usuário pode iniciar uma conversa. */
    @Transactional(readOnly = true)
    public List<UsuarioResumo> contatos() {
        return authApi.contatos();
    }

    @Transactional(readOnly = true)
    public long contarNaoLidas(String destinatarioId) {
        return chatRepository.countByDestinatarioIdAndLidaFalse(destinatarioId);
    }

    /** Prévia para a caixa de entrada e para o corpo da notificação. */
    private static String resumir(String texto) {
        if (texto == null) return null;
        String limpo = texto.replaceAll("\s+", " ").trim();
        return limpo.length() <= 120 ? limpo : limpo.substring(0, 117) + "...";
    }
}
