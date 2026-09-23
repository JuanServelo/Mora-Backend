package comunicacao.service;

import comunicacao.client.AuthApiClient;
import comunicacao.dto.AvisoResponse;
import comunicacao.dto.UsuarioResumo;
import comunicacao.exception.OperacaoInvalidaException;
import comunicacao.exception.RecursoNaoEncontradoException;
import comunicacao.model.Aviso;
import comunicacao.model.enums.TipoNotificacao;
import comunicacao.repository.AvisoRepository;
import comunicacao.security.AuthContext;
import comunicacao.security.CondominioUtils;
import comunicacao.security.PerfilUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AvisoService {

    private static final Logger log = LoggerFactory.getLogger(AvisoService.class);

    private final AvisoRepository avisoRepository;
    private final AvisoLeituraService leituraService;
    private final NotificacaoService notificacaoService;
    private final AuthApiClient authApi;

    public AvisoResponse criar(Aviso aviso) {
        PerfilUtils.exigirGestor();
        validarPeriodo(aviso);

        var claims = AuthContext.get();
        aviso.setId(null);
        aviso.setCondominioId(CondominioUtils.condominioIdEfetivo());
        if (aviso.getAutor() == null && claims != null) {
            aviso.setAutor(claims.email());
        }
        aviso.setCriadoEm(LocalDateTime.now());
        aviso.setAtualizadoEm(LocalDateTime.now());

        Aviso salvo = avisoRepository.save(aviso);
        if (salvo.isPublicado()) notificarDestinatarios(salvo);
        return AvisoResponse.de(salvo, false, 0L);
    }

    public AvisoResponse publicar(UUID id) {
        PerfilUtils.exigirGestor();
        Aviso aviso = buscarEntidade(id);
        boolean eraRascunho = !aviso.isPublicado();
        aviso.setPublicado(true);
        aviso.setAtualizadoEm(LocalDateTime.now());
        Aviso salvo = avisoRepository.save(aviso);
        // Republicar nao deve notificar de novo quem ja foi avisado.
        if (eraRascunho) notificarDestinatarios(salvo);
        return AvisoResponse.de(salvo, false, leituraService.contarLeituras(id));
    }

    public AvisoResponse despublicar(UUID id) {
        PerfilUtils.exigirGestor();
        Aviso aviso = buscarEntidade(id);
        aviso.setPublicado(false);
        aviso.setAtualizadoEm(LocalDateTime.now());
        return AvisoResponse.de(avisoRepository.save(aviso), false, leituraService.contarLeituras(id));
    }

    public AvisoResponse encerrar(UUID id) {
        PerfilUtils.exigirGestor();
        Aviso aviso = buscarEntidade(id);
        aviso.setDataFim(LocalDate.now().minusDays(1));
        aviso.setAtualizadoEm(LocalDateTime.now());
        return AvisoResponse.de(avisoRepository.save(aviso), false, leituraService.contarLeituras(id));
    }

    /**
     * Atualiza o aviso.
     *
     * O campo publicado entra na copia: sem ele o botao de publicar da tela de
     * gestao devolvia 200 sem ter mudado nada.
     */
    public AvisoResponse atualizar(UUID id, Aviso dados) {
        PerfilUtils.exigirGestor();
        validarPeriodo(dados);

        Aviso aviso = buscarEntidade(id);
        boolean eraRascunho = !aviso.isPublicado();

        aviso.setTitulo(dados.getTitulo());
        aviso.setMensagem(dados.getMensagem());
        aviso.setDataInicio(dados.getDataInicio());
        aviso.setDataFim(dados.getDataFim());
        aviso.setPublicoAlvo(dados.getPublicoAlvo());
        if (dados.getAutor() != null) aviso.setAutor(dados.getAutor());
        aviso.setPublicado(dados.isPublicado());
        aviso.setAtualizadoEm(LocalDateTime.now());

        Aviso salvo = avisoRepository.save(aviso);
        if (eraRascunho && salvo.isPublicado()) notificarDestinatarios(salvo);
        return AvisoResponse.de(salvo, false, leituraService.contarLeituras(id));
    }

    public void excluir(UUID id) {
        PerfilUtils.exigirGestor();
        if (!avisoRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Aviso nao encontrado com id: " + id);
        }
        avisoRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Aviso buscarEntidade(UUID id) {
        return avisoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aviso nao encontrado com id: " + id));
    }

    @Transactional(readOnly = true)
    public AvisoResponse buscarPorId(UUID id, String usuarioId) {
        Aviso aviso = buscarEntidade(id);
        boolean lido = leituraService.avisosLidosPor(usuarioId).contains(id);
        return AvisoResponse.de(aviso, lido, leituraService.contarLeituras(id));
    }

    @Transactional(readOnly = true)
    public List<AvisoResponse> listarTodos(String condominioId, String usuarioId) {
        List<Aviso> avisos = condominioId == null
                ? avisoRepository.findAllByOrderByCriadoEmDesc()
                : avisoRepository.findByCondominioIdOrderByCriadoEmDesc(condominioId);
        return decorar(avisos, usuarioId);
    }

    @Transactional(readOnly = true)
    public List<AvisoResponse> listarAtivos(String condominioId, String usuarioId) {
        List<Aviso> avisos = condominioId == null
                ? avisoRepository.findTodosAtivos(LocalDate.now())
                : avisoRepository.findAtivos(condominioId, LocalDate.now());
        return decorar(avisos, usuarioId);
    }

    /**
     * Anexa a cada aviso o que a tela precisa: se o usuario ja leu e, para a
     * administracao, quantos leram. Cada informacao sai de uma consulta so,
     * em vez de uma por aviso.
     */
    private List<AvisoResponse> decorar(List<Aviso> avisos, String usuarioId) {
        if (avisos.isEmpty()) return List.of();

        Set<UUID> lidos = leituraService.avisosLidosPor(usuarioId);
        boolean mostrarContagem = PerfilUtils.isAdministracao(PerfilUtils.perfilAtual());
        Map<UUID, Long> contagem = mostrarContagem
                ? leituraService.contagemPorAviso(avisos.stream().map(Aviso::getId).toList())
                : Map.of();

        return avisos.stream()
                .map(a -> AvisoResponse.de(a, lidos.contains(a.getId()),
                        mostrarContagem ? contagem.getOrDefault(a.getId(), 0L) : null))
                .toList();
    }

    private void validarPeriodo(Aviso aviso) {
        if (aviso.getDataInicio() != null && aviso.getDataFim() != null
                && aviso.getDataInicio().isAfter(aviso.getDataFim())) {
            throw new OperacaoInvalidaException("A data de inicio nao pode ser posterior a data de fim.");
        }
    }

    /**
     * Notifica quem deve ver o aviso.
     *
     * O aviso aparece na tela inicial de qualquer forma; a notificacao e o que
     * alcanca quem nao abriu o app hoje. Uma falha aqui nao desfaz a
     * publicacao — o comunicado e o que importa.
     */
    private void notificarDestinatarios(Aviso aviso) {
        try {
            String publicoAlvo = aviso.getPublicoAlvo() == null ? "TODOS" : aviso.getPublicoAlvo().name();
            List<UsuarioResumo> destinatarios = authApi.destinatarios(publicoAlvo);
            notificacaoService.criarParaVarios(
                    destinatarios.stream().map(UsuarioResumo::id).toList(),
                    aviso.getCondominioId(),
                    TipoNotificacao.AVISO,
                    aviso.getTitulo(),
                    aviso.getMensagem(),
                    aviso.getId().toString());
        } catch (Exception e) {
            log.warn("Aviso {} publicado, mas as notificacoes falharam: {}", aviso.getId(), e.getMessage());
        }
    }
}
