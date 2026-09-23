package comunicacao.service;

import comunicacao.exception.RecursoNaoEncontradoException;
import comunicacao.model.Notificacao;
import comunicacao.model.PreferenciaNotificacao;
import comunicacao.model.enums.TipoNotificacao;
import comunicacao.repository.NotificacaoRepository;
import comunicacao.repository.PreferenciaNotificacaoRepository;
import comunicacao.security.AcessoNegadoException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificacaoService {

    private final NotificacaoRepository notificacaoRepository;
    private final PreferenciaNotificacaoRepository preferenciaRepository;

    /**
     * Cria uma notificação, respeitando o que o destinatário optou por não
     * receber. Devolve null quando a categoria está silenciada — quem dispara
     * não precisa saber da preferência para funcionar.
     */
    public Notificacao criar(String destinatarioId, String condominioId,
                             TipoNotificacao tipo, String titulo, String mensagem,
                             String referenciaId) {
        if (destinatarioId == null) return null;
        if (tipo != null && preferenciaRepository.existsByUsuarioIdAndTipo(destinatarioId, tipo)) {
            return null;
        }
        Notificacao n = new Notificacao();
        n.setDestinatarioId(destinatarioId);
        n.setCondominioId(condominioId);
        n.setTipo(tipo);
        n.setTitulo(titulo);
        n.setMensagem(mensagem);
        n.setReferenciaId(referenciaId);
        return notificacaoRepository.save(n);
    }

    /** Mesma notificação para vários destinatários — usado ao publicar um aviso. */
    public int criarParaVarios(Collection<String> destinatarioIds, String condominioId,
                               TipoNotificacao tipo, String titulo, String mensagem,
                               String referenciaId) {
        int criadas = 0;
        for (String id : destinatarioIds) {
            if (criar(id, condominioId, tipo, titulo, mensagem, referenciaId) != null) criadas++;
        }
        return criadas;
    }

    /** Só o dono marca a própria notificação como lida. */
    public Notificacao marcarLida(String id, String usuarioId) {
        Notificacao n = buscarPorId(id);
        if (!n.getDestinatarioId().equals(usuarioId)) {
            throw new AcessoNegadoException("Esta notificação não é sua.");
        }
        if (!n.isLida()) {
            n.setLida(true);
            n.setLidaEm(LocalDateTime.now());
            notificacaoRepository.save(n);
        }
        return n;
    }

    public void marcarTodasLidas(String destinatarioId) {
        LocalDateTime agora = LocalDateTime.now();
        notificacaoRepository
                .findByDestinatarioIdAndLidaFalseOrderByCriadoEmDesc(destinatarioId)
                .forEach(n -> {
                    n.setLida(true);
                    n.setLidaEm(agora);
                });
    }

    public void excluir(String id, String usuarioId) {
        Notificacao n = buscarPorId(id);
        if (!n.getDestinatarioId().equals(usuarioId)) {
            throw new AcessoNegadoException("Esta notificação não é sua.");
        }
        notificacaoRepository.delete(n);
    }

    @Transactional(readOnly = true)
    public Notificacao buscarPorId(String id) {
        return notificacaoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Notificação não encontrada com id: " + id));
    }

    @Transactional(readOnly = true)
    public Page<Notificacao> listar(String destinatarioId, Pageable pageable) {
        return notificacaoRepository.findByDestinatarioIdOrderByCriadoEmDesc(destinatarioId, pageable);
    }

    @Transactional(readOnly = true)
    public List<Notificacao> listarNaoLidas(String destinatarioId) {
        return notificacaoRepository.findByDestinatarioIdAndLidaFalseOrderByCriadoEmDesc(destinatarioId);
    }

    @Transactional(readOnly = true)
    public long contarNaoLidas(String destinatarioId) {
        return notificacaoRepository.countByDestinatarioIdAndLidaFalse(destinatarioId);
    }

    /* ------------------------------------------------------ preferências --- */

    @Transactional(readOnly = true)
    public Set<TipoNotificacao> silenciadas(String usuarioId) {
        return preferenciaRepository.findByUsuarioId(usuarioId).stream()
                .map(PreferenciaNotificacao::getTipo)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(TipoNotificacao.class)));
    }

    /**
     * Substitui as preferências pelo conjunto informado. Regravar tudo evita o
     * estado meio-atualizado que um merge campo a campo produziria se a tela
     * enviasse uma lista defasada.
     */
    public Set<TipoNotificacao> salvarPreferencias(String usuarioId, Collection<TipoNotificacao> silenciadas) {
        preferenciaRepository.deleteByUsuarioId(usuarioId);
        preferenciaRepository.flush();

        Set<TipoNotificacao> novas = silenciadas == null
                ? EnumSet.noneOf(TipoNotificacao.class)
                : silenciadas.stream().filter(java.util.Objects::nonNull)
                        .collect(Collectors.toCollection(() -> EnumSet.noneOf(TipoNotificacao.class)));

        novas.forEach(tipo -> {
            PreferenciaNotificacao p = new PreferenciaNotificacao();
            p.setUsuarioId(usuarioId);
            p.setTipo(tipo);
            preferenciaRepository.save(p);
        });
        return novas;
    }
}
