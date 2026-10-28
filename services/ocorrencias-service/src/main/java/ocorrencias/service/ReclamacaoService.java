package ocorrencias.service;

import lombok.RequiredArgsConstructor;
import ocorrencias.clients.AuthClient;
import ocorrencias.dto.AtendimentoRequest;
import ocorrencias.dto.NovaReclamacaoRequest;
import ocorrencias.dto.ReclamacaoResponse;
import ocorrencias.dto.ReclamacaoResponse.OrdemVinculada;
import ocorrencias.exception.DadosInvalidosException;
import ocorrencias.exception.RecursoNaoEncontradoException;
import ocorrencias.model.Reclamacao;
import ocorrencias.model.ReclamacaoInteracao;
import ocorrencias.model.enums.CategoriaReclamacao;
import ocorrencias.model.enums.StatusReclamacao;
import ocorrencias.notificacao.NotificacaoEvento;
import ocorrencias.notificacao.NotificacaoPublisher;
import ocorrencias.repository.OrdemServicoRepository;
import ocorrencias.repository.ReclamacaoRepository;
import ocorrencias.security.Autorizacao;
import ocorrencias.security.JwtClaims;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ReclamacaoService {

    private final ReclamacaoRepository reclamacoes;
    private final OrdemServicoRepository ordens;
    private final AuthClient authClient;
    private final NotificacaoPublisher notificacoes;

    /** O morador registra: nasce `PENDENTE`, com protocolo. */
    public ReclamacaoResponse registrar(NovaReclamacaoRequest req) {
        JwtClaims eu = Autorizacao.atual();
        String condominioId = Autorizacao.exigirCondominio();

        String categoria = req.categoria().trim().toUpperCase();
        if (!CategoriaReclamacao.valida(categoria)) {
            throw new DadosInvalidosException("Categoria desconhecida: " + req.categoria());
        }

        Reclamacao r = new Reclamacao();
        r.setUsuarioId(eu.authUserId());
        r.setCondominioId(condominioId);
        r.setProtocolo(Codigos.gerarUnico("REC", reclamacoes::existsByProtocolo));
        r.setCategoria(categoria);
        r.setDescricao(req.descricao().trim());
        r.setAnexoUrl(vazioParaNulo(req.anexoUrl()));
        r.setStatus(StatusReclamacao.PENDENTE);

        return ReclamacaoResponse.de(reclamacoes.save(r), null, List.of(), false);
    }

    /** O morador vê apenas as próprias. */
    @Transactional(readOnly = true)
    public List<ReclamacaoResponse> minhas() {
        List<Reclamacao> lista = reclamacoes.findByUsuarioIdOrderByCriadoEmDesc(Autorizacao.atual().authUserId());
        Map<Long, List<OrdemVinculada>> vinculos = ordensPorReclamacao(lista);
        return lista.stream()
                .map(r -> ReclamacaoResponse.de(r, null, vinculos.get(r.getId()), false))
                .toList();
    }

    /** A gestão vê todas as do condomínio (o Admin Geral, de qualquer um). */
    @Transactional(readOnly = true)
    public List<ReclamacaoResponse> listarDaGestao(String condominioPedido, StatusReclamacao status,
                                                   String authorization) {
        Autorizacao.exigirVisaoDeGestao("consultar as ocorrências do condomínio");
        String condominioId = Autorizacao.condominioDeLeitura(condominioPedido);

        List<Reclamacao> lista = reclamacoes.listarDaGestao(condominioId, status);
        Map<Long, List<OrdemVinculada>> vinculos = ordensPorReclamacao(lista);
        Map<String, AuthClient.Usuario> usuarios = lista.isEmpty()
                ? Map.of()
                : authClient.usuariosPorId(condominioId, authorization);

        return lista.stream()
                .map(r -> ReclamacaoResponse.de(r, usuario(usuarios, r.getUsuarioId()), vinculos.get(r.getId()), true))
                .toList();
    }

    /** Uma ocorrência: para quem a abriu ou para a gestão do condomínio dela. */
    @Transactional(readOnly = true)
    public ReclamacaoResponse detalhar(Long id, String authorization) {
        Reclamacao r = buscar(id);
        boolean dono = r.getUsuarioId().equals(Autorizacao.atual().authUserId());
        boolean gestao = Autorizacao.gestaoAlcanca(r.getCondominioId());
        if (!dono && !gestao) throw naoEncontrada();

        ReclamacaoResponse.Usuario autor = gestao
                ? usuario(authClient.usuariosPorId(r.getCondominioId(), authorization), r.getUsuarioId())
                : null;
        return ReclamacaoResponse.de(r, autor, ordensPorReclamacao(List.of(r)).get(r.getId()), gestao);
    }

    /**
     * O síndico responde e, se quiser, muda o status.
     *
     * Toda resposta entra no histórico com autor e data; quando muda o status,
     * a mensagem é a justificativa. O morador é notificado.
     */
    public ReclamacaoResponse atender(Long id, AtendimentoRequest req, String authorization) {
        Autorizacao.exigirSindico("responder ocorrências");
        JwtClaims eu = Autorizacao.atual();
        Reclamacao r = buscar(id);
        if (!Autorizacao.gestaoAlcanca(r.getCondominioId())) throw naoEncontrada();

        StatusReclamacao anterior = r.getStatus();
        StatusReclamacao novo = req.status() == null ? anterior : req.status();
        boolean mudou = novo != anterior;

        registrarInteracao(r, eu, req.mensagem().trim(), mudou ? anterior : null, mudou ? novo : null);
        if (mudou) mudarStatus(r, novo);
        reclamacoes.saveAndFlush(r);

        ReclamacaoInteracao ultima = r.getInteracoes().get(r.getInteracoes().size() - 1);
        notificarMorador(r, eu, NotificacaoEvento.OCORRENCIA_RESPONDIDA,
                mudou
                        ? "Ocorrência " + r.getProtocolo() + ": " + rotulo(novo)
                        : "Ocorrência " + r.getProtocolo() + " respondida",
                req.mensagem().trim(),
                "interacao:" + ultima.getId());

        return detalharGestao(r, authorization);
    }

    /** Usado pela OS: registra no histórico da ocorrência que ela foi aberta. */
    void registrarInteracao(Reclamacao r, JwtClaims autor, String mensagem,
                            StatusReclamacao anterior, StatusReclamacao novo) {
        ReclamacaoInteracao i = new ReclamacaoInteracao();
        i.setAutorId(autor.authUserId());
        i.setAutorEmail(autor.email());
        i.setAutorPerfil(autor.perfil());
        i.setMensagem(mensagem);
        i.setStatusAnterior(anterior);
        i.setStatusNovo(novo);
        r.adicionarInteracao(i);
    }

    void mudarStatus(Reclamacao r, StatusReclamacao novo) {
        r.setStatus(novo);
        // Reabrir apaga a data de resolução: o tempo até resolver passa a
        // contar da próxima vez que ela for resolvida.
        r.setResolvidoEm(novo == StatusReclamacao.RESOLVIDO ? LocalDateTime.now() : null);
    }

    /** Avisa quem abriu — a não ser que tenha sido ele mesmo quem agiu. */
    void notificarMorador(Reclamacao r, JwtClaims autor, String tipo, String titulo, String mensagem, String evento) {
        if (r.getUsuarioId().equals(autor.authUserId())) return;
        notificacoes.publicar(new NotificacaoEvento(
                r.getCondominioId(),
                r.getUsuarioId(),
                NotificacaoEvento.ORIGEM,
                tipo,
                titulo,
                mensagem,
                Map.of("reclamacaoId", r.getId(), "protocolo", r.getProtocolo()),
                "ocorrencias:reclamacao:" + r.getId() + ":" + evento
        ));
    }

    Reclamacao buscar(Long id) {
        return reclamacoes.findWithInteracoesById(id).orElseThrow(ReclamacaoService::naoEncontrada);
    }

    private ReclamacaoResponse detalharGestao(Reclamacao r, String authorization) {
        ReclamacaoResponse.Usuario autor = usuario(authClient.usuariosPorId(r.getCondominioId(), authorization), r.getUsuarioId());
        return ReclamacaoResponse.de(r, autor, ordensPorReclamacao(List.of(r)).get(r.getId()), true);
    }

    private Map<Long, List<OrdemVinculada>> ordensPorReclamacao(List<Reclamacao> lista) {
        if (lista.isEmpty()) return Map.of();
        List<Long> ids = lista.stream().map(Reclamacao::getId).toList();
        return ordens.findByReclamacaoIdInOrderByCriadoEmAsc(ids).stream()
                .collect(Collectors.groupingBy(o -> o.getReclamacao().getId(),
                        Collectors.mapping(OrdemVinculada::de, Collectors.toList())));
    }

    private static ReclamacaoResponse.Usuario usuario(Map<String, AuthClient.Usuario> usuarios, String id) {
        AuthClient.Usuario u = usuarios.get(id);
        return u == null ? null : new ReclamacaoResponse.Usuario(u.nome(), u.email());
    }

    static String rotulo(StatusReclamacao s) {
        return switch (s) {
            case PENDENTE -> "pendente";
            case EM_ANALISE -> "em análise";
            case RESOLVIDO -> "resolvida";
        };
    }

    static RecursoNaoEncontradoException naoEncontrada() {
        return new RecursoNaoEncontradoException("Ocorrência não encontrada.");
    }

    private static String vazioParaNulo(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
