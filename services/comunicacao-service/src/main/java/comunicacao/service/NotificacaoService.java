package comunicacao.service;

import comunicacao.exception.RecursoNaoEncontradoException;
import comunicacao.model.Notificacao;
import comunicacao.model.enums.TipoNotificacao;
import comunicacao.repository.NotificacaoRepository;
import comunicacao.exception.DadosInvalidosException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificacaoService {

    private final NotificacaoRepository notificacaoRepository;

    public Notificacao criar(String destinatarioId, String condominioId,
                             TipoNotificacao tipo, String titulo, String mensagem,
                             String referenciaId) {
        Notificacao n = new Notificacao();
        n.setDestinatarioId(destinatarioId);
        n.setCondominioId(condominioId);
        n.setTipo(tipo);
        n.setTitulo(titulo);
        n.setMensagem(mensagem);
        n.setReferenciaId(referenciaId);
        return notificacaoRepository.save(n);
    }

    /**
     * Uma notificação publicada por outro serviço.
     *
     * Os campos são os que o financeiro já manda: o formato da rota não mudou
     * quando o comunicacao passou de Node para Java.
     */
    public record Publicacao(
            String condominioId,
            String usuarioId,
            String origem,
            String tipo,
            String titulo,
            String mensagem,
            Map<String, Object> dados,
            String chaveUnica
    ) {}

    /** `repetida` é verdadeiro quando a chave já tinha sido publicada antes. */
    public record Resultado(Notificacao notificacao, boolean repetida) {}

    /**
     * Grava a notificação de outro serviço — uma vez só por `chaveUnica`.
     *
     * Reenvio com a mesma chave devolve a notificação já existente, e não erro:
     * um erro faria o publicador entender que falhou e, se tentasse de novo,
     * tentaria para sempre. Duas publicações simultâneas com a mesma chave
     * podem passar as duas pela conferência abaixo; aí o índice único recusa a
     * segunda, e o controller a trata como repetida.
     */
    public Resultado publicarDeServico(Publicacao p) {
        if (isBlank(p.usuarioId()) || isBlank(p.condominioId())) {
            throw new DadosInvalidosException("Informe usuarioId e condominioId.");
        }
        if (isBlank(p.titulo())) {
            throw new DadosInvalidosException("Informe o título da notificação.");
        }
        TipoNotificacao tipo;
        try {
            tipo = TipoNotificacao.valueOf(String.valueOf(p.tipo()).toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new DadosInvalidosException("Tipo de notificação desconhecido: " + p.tipo());
        }

        String chave = isBlank(p.chaveUnica()) ? null : p.chaveUnica().trim();
        if (chave != null) {
            var existente = notificacaoRepository.findByChaveUnica(chave);
            if (existente.isPresent()) return new Resultado(existente.get(), true);
        }

        Notificacao n = new Notificacao();
        n.setDestinatarioId(p.usuarioId());
        n.setCondominioId(p.condominioId());
        n.setTipo(tipo);
        n.setTitulo(p.titulo());
        n.setMensagem(isBlank(p.mensagem()) ? p.titulo() : p.mensagem());
        n.setOrigem(isBlank(p.origem()) ? null : p.origem());
        n.setChaveUnica(chave);
        n.setDados(p.dados());
        n.setReferenciaId(referenciaDe(p.dados()));
        return new Resultado(notificacaoRepository.saveAndFlush(n), false);
    }

    /** Busca pela chave — usada quando o índice único recusa uma publicação simultânea. */
    @Transactional(readOnly = true)
    public Notificacao buscarPorChave(String chaveUnica) {
        return notificacaoRepository.findByChaveUnica(chaveUnica).orElse(null);
    }

    /** O id da fatura ou da multa vira a referência que a tela usa para abrir o destino. */
    private static String referenciaDe(Map<String, Object> dados) {
        if (dados == null) return null;
        Object ref = dados.getOrDefault("faturaId", dados.get("multaId"));
        return ref == null ? null : String.valueOf(ref);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    public Notificacao marcarLida(String id) {
        Notificacao n = buscarPorId(id);
        if (!n.isLida()) {
            n.setLida(true);
            n.setLidaEm(LocalDateTime.now());
            notificacaoRepository.save(n);
        }
        return n;
    }

    public void marcarTodasLidas(String destinatarioId) {
        notificacaoRepository
                .findByDestinatarioIdAndLidaFalseOrderByCriadoEmDesc(destinatarioId)
                .forEach(n -> {
                    n.setLida(true);
                    n.setLidaEm(LocalDateTime.now());
                });
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
}
