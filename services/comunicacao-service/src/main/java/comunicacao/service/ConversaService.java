package comunicacao.service;

import comunicacao.clients.AuthClient;
import comunicacao.exception.AcessoNegadoException;
import comunicacao.exception.DadosInvalidosException;
import comunicacao.exception.OperacaoInvalidaException;
import comunicacao.exception.RecursoNaoEncontradoException;
import comunicacao.model.Conversa;
import comunicacao.model.ConversaParticipante;
import comunicacao.model.Mensagem;
import comunicacao.model.enums.TipoConversa;
import comunicacao.model.enums.TipoNotificacao;
import comunicacao.repository.ConversaParticipanteRepository;
import comunicacao.repository.ConversaRepository;
import comunicacao.repository.MensagemRepository;
import comunicacao.security.AuthContext;
import comunicacao.security.Autorizacao;
import comunicacao.security.CondominioUtils;
import comunicacao.security.JwtClaims;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Conversas do RF-13.
 *
 * Convive com o `ChatService`, que é outra coisa: lá são mensagens diretas
 * entre dois usuários identificados por id. Aqui existe a **conversa** como
 * entidade, com assunto, participantes e encerramento — e o formato
 * `ADMINISTRACAO`, em que o morador fala com a administração sem escolher uma
 * pessoa.
 *
 * As checagens de acesso moram aqui, e não no controller: um endpoint novo
 * passaria por cima delas sem que ninguém percebesse.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ConversaService {

    private static final Logger log = LoggerFactory.getLogger(ConversaService.class);
    private static final int LIMITE_CORPO = 4000;

    private final ConversaRepository conversaRepository;
    private final ConversaParticipanteRepository participanteRepository;
    private final MensagemRepository mensagemRepository;
    private final NotificacaoService notificacaoService;
    private final AuthClient authClient;

    /* ------------------------------------------------------------ escrita --- */

    /**
     * Abre uma conversa já com a primeira mensagem.
     *
     * As duas coisas juntas, numa transação só: conversa sem mensagem nenhuma
     * apareceria na caixa do destinatário como aviso de algo que não dá para
     * ler.
     */
    public Map<String, Object> abrir(NovaConversa entrada, String authorization) {
        exigirUsuario();

        TipoConversa tipo = entrada.tipo() != null
                ? entrada.tipo()
                : (Autorizacao.ehGestao() ? TipoConversa.DIRETA : TipoConversa.ADMINISTRACAO);

        String assunto = textoObrigatorio(entrada.assunto(), "Informe o assunto da conversa.", 150);
        String corpo = textoObrigatorio(entrada.corpo(), "Escreva a primeira mensagem.", LIMITE_CORPO);

        // Quem não é gestão só fala com a administração. Não é restrição de
        // tela: a listagem de usuários que o morador alcança é recortada na
        // unidade dele, e o síndico não está nela — não haveria destinatário
        // a escolher.
        if (tipo == TipoConversa.DIRETA && !Autorizacao.ehGestao()) {
            throw new AcessoNegadoException(
                    "Converse com a administração do condomínio; ela encaminha a quem for preciso.");
        }

        List<String> participantes = new ArrayList<>();
        participantes.add(usuarioAtual());

        if (tipo == TipoConversa.DIRETA) {
            participantes.add(validarDestinatario(entrada.destinatarioId(), authorization));
        }

        Conversa conversa = new Conversa();
        conversa.setCondominioId(condominioAtual());
        conversa.setTipo(tipo);
        conversa.setAssunto(assunto);
        conversa.setCriadaPor(usuarioAtual());
        conversa = conversaRepository.save(conversa);

        for (String usuarioId : participantes) {
            garantirParticipante(conversa.getId(), usuarioId);
        }

        Mensagem mensagem = gravarMensagem(conversa, corpo);
        marcarLeitura(conversa.getId(), usuarioAtual());

        notificarOutros(conversa, mensagem, participantes);

        return Map.of("sucesso", true, "conversa", conversa, "mensagem", mensagem);
    }

    /** Responde numa conversa existente. */
    public Map<String, Object> responder(Long conversaId, String corpoBruto) {
        exigirUsuario();
        String corpo = textoObrigatorio(corpoBruto, "Escreva a mensagem.", LIMITE_CORPO);

        Conversa conversa = autorizar(conversaId);

        if (conversa.getEncerradaEm() != null) {
            throw new OperacaoInvalidaException("Esta conversa foi encerrada.");
        }

        // A gestão que responde uma conversa da administração entra como
        // participante agora. Antes disso ela via a conversa pela regra do
        // condomínio, sem linha própria — e sem linha não há marca de leitura.
        garantirParticipante(conversa.getId(), usuarioAtual());

        Mensagem mensagem = gravarMensagem(conversa, corpo);
        marcarLeitura(conversa.getId(), usuarioAtual());

        List<String> participantes = participanteRepository.findByConversaId(conversa.getId())
                .stream().map(ConversaParticipante::getUsuarioId).toList();
        notificarOutros(conversa, mensagem, participantes);

        return Map.of("sucesso", true, "mensagem", mensagem);
    }

    /**
     * Encerra a conversa.
     *
     * Ato da administração: o morador que abriu não fecha o assunto sozinho,
     * senão a gestão perde a pendência da lista sem ter respondido.
     */
    public Map<String, Object> encerrar(Long conversaId) {
        Conversa conversa = autorizar(conversaId);

        if (!Autorizacao.ehGestao()) {
            throw new AcessoNegadoException("Somente a administração encerra uma conversa.");
        }

        conversa.setEncerradaEm(LocalDateTime.now());
        return Map.of("sucesso", true, "conversa", conversaRepository.save(conversa));
    }

    /**
     * Remove uma mensagem — logicamente.
     *
     * 404 tanto para "não existe" quanto para "não é sua": distinguir contaria
     * a quem tentou que a mensagem de outra pessoa existe.
     */
    public Map<String, Object> removerMensagem(Long mensagemId) {
        Mensagem m = mensagemRepository.findById(mensagemId)
                .filter(x -> x.getAutorId().equals(usuarioAtual()))
                .filter(x -> x.getRemovidaEm() == null)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Mensagem não encontrada."));

        m.setRemovidaEm(LocalDateTime.now());
        return Map.of("sucesso", true, "mensagem", mensagemRepository.save(m));
    }

    /* ------------------------------------------------------------ leitura --- */

    @Transactional(readOnly = true)
    public Map<String, Object> listar(boolean incluirEncerradas, String authorization) {
        exigirUsuario();

        List<Conversa> itens = conversaRepository.listarVisiveis(
                usuarioAtual(), condominioAtual(), Autorizacao.ehGestao(), incluirEncerradas);

        Map<String, AuthClient.Usuario> pessoas = diretorio(authorization);

        List<Map<String, Object>> saida = new ArrayList<>();
        for (Conversa c : itens) {
            Mensagem ultima = mensagemRepository
                    .findFirstByConversaIdAndRemovidaEmIsNullOrderByCriadaEmDesc(c.getId());

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", c.getId());
            item.put("assunto", c.getAssunto());
            item.put("tipo", c.getTipo());
            item.put("criadaPor", c.getCriadaPor());
            item.put("criadaEm", c.getCriadaEm());
            item.put("ultimaMensagemEm", c.getUltimaMensagemEm());
            item.put("encerradaEm", c.getEncerradaEm());
            item.put("contraparte", contraparteDe(c, pessoas));
            // O resumo é só o começo da última mensagem; a conversa inteira
            // vem quando alguém abre.
            item.put("resumo", ultima == null ? null : recortar(ultima.getCorpo(), 120));
            item.put("naoLidas", contarNaoLidasDa(c.getId()));
            saida.add(item);
        }

        return Map.of("sucesso", true, "conversas", saida);
    }

    /** Abre a conversa e marca como lida — abrir é ler. */
    public Map<String, Object> detalhar(Long conversaId, String authorization) {
        Conversa conversa = autorizar(conversaId);

        List<Mensagem> lista = mensagemRepository.findByConversaIdOrderByCriadaEmAsc(conversaId);

        // Só tem efeito para quem já é participante: a gestão que apenas espiou
        // a conversa da administração sem responder não vira participante por
        // abrir.
        marcarLeitura(conversaId, usuarioAtual());

        Map<String, AuthClient.Usuario> pessoas = diretorio(authorization);
        List<ConversaParticipante> participantes = participanteRepository.findByConversaId(conversaId);

        Map<String, Object> cabecalho = new LinkedHashMap<>();
        cabecalho.put("id", conversa.getId());
        cabecalho.put("assunto", conversa.getAssunto());
        cabecalho.put("tipo", conversa.getTipo());
        cabecalho.put("criadaPor", conversa.getCriadaPor());
        cabecalho.put("criadaEm", conversa.getCriadaEm());
        cabecalho.put("ultimaMensagemEm", conversa.getUltimaMensagemEm());
        cabecalho.put("encerradaEm", conversa.getEncerradaEm());
        cabecalho.put("contraparte", contraparteDe(conversa, pessoas));

        // O autor vai em cada mensagem porque uma conversa com a administração
        // pode ter mais de uma pessoa da gestão respondendo ao longo do tempo —
        // e `autorPerfil`, gravado junto, diz o papel, não quem era.
        List<Map<String, Object>> mensagens = lista.stream().map(m -> {
            Map<String, Object> mm = new LinkedHashMap<>();
            mm.put("id", m.getId());
            mm.put("autorId", m.getAutorId());
            mm.put("autorPerfil", m.getAutorPerfil());
            mm.put("corpo", m.getRemovidaEm() == null ? m.getCorpo() : null);
            mm.put("removidaEm", m.getRemovidaEm());
            mm.put("criadaEm", m.getCriadaEm());
            mm.put("autor", pessoas.get(m.getAutorId()));
            return mm;
        }).toList();

        List<Map<String, Object>> quem = participantes.stream().map(p -> {
            Map<String, Object> pp = new LinkedHashMap<>();
            pp.put("usuarioId", p.getUsuarioId());
            pp.put("ultimaLeituraEm", p.getUltimaLeituraEm());
            pp.put("pessoa", pessoas.get(p.getUsuarioId()));
            return pp;
        }).toList();

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("sucesso", true);
        r.put("conversa", cabecalho);
        r.put("participantes", quem);
        r.put("mensagens", mensagens);
        return r;
    }

    /**
     * Conversas com mensagem não lida — entra no contador do sino.
     *
     * Existe separado do contador de notificações porque a conversa da
     * administração ainda sem resposta **não gera notificação para ninguém**:
     * não há participante da gestão a quem endereçá-la. Sem esta contagem, a
     * primeira mensagem de um morador não acenderia nada para o síndico.
     */
    @Transactional(readOnly = true)
    public long contarConversasNaoLidas() {
        if (AuthContext.get() == null) return 0;

        return conversaRepository
                .listarVisiveis(usuarioAtual(), condominioAtual(), Autorizacao.ehGestao(), false)
                .stream()
                .filter(c -> contarNaoLidasDa(c.getId()) > 0)
                .count();
    }

    /** O diretório que a gestão usa para escolher o destinatário. */
    @Transactional(readOnly = true)
    public Map<String, Object> contatos(String authorization) {
        Autorizacao.exigirVisaoDeGestao("ver os contatos do condomínio");

        List<AuthClient.Usuario> usuarios = authClient
                .listarUsuariosDoCondominio(condominioAtual(), authorization);

        if (usuarios == null) {
            throw new OperacaoInvalidaException("Não foi possível carregar os contatos agora.");
        }

        List<AuthClient.Usuario> alcancaveis = usuarios.stream()
                .filter(AuthClient.Usuario::alcancavel)
                .filter(u -> !u.id().equals(usuarioAtual()))
                .toList();

        return Map.of("sucesso", true, "contatos", alcancaveis);
    }

    /* -------------------------------------------------------------- apoio --- */

    /**
     * Confere que o usuário pode ver a conversa.
     *
     * Duas portas: ser participante, ou ser gestão do condomínio numa conversa
     * `ADMINISTRACAO`. Qualquer outra combinação é **404, e não 403** — dizer
     * "existe, mas não é sua" já entrega que existe.
     */
    private Conversa autorizar(Long conversaId) {
        exigirUsuario();
        if (conversaId == null) throw new DadosInvalidosException("Identificador de conversa inválido.");

        // O escopo de condomínio vale antes de qualquer outra regra.
        Conversa conversa = conversaRepository
                .findByIdAndCondominioId(conversaId, condominioAtual())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conversa não encontrada."));

        boolean participa = participanteRepository
                .existsByConversaIdAndUsuarioId(conversaId, usuarioAtual());
        boolean porRegraDeGestao =
                Autorizacao.ehGestao() && conversa.getTipo() == TipoConversa.ADMINISTRACAO;

        if (!participa && !porRegraDeGestao) {
            throw new RecursoNaoEncontradoException("Conversa não encontrada.");
        }
        return conversa;
    }

    private String validarDestinatario(String destinatarioId, String authorization) {
        if (destinatarioId == null || destinatarioId.isBlank()) {
            throw new DadosInvalidosException("Informe o destinatário.");
        }
        if (destinatarioId.equals(usuarioAtual())) {
            throw new DadosInvalidosException("Escolha outra pessoa como destinatário.");
        }

        List<AuthClient.Usuario> lista = authClient
                .listarUsuariosDoCondominio(condominioAtual(), authorization);

        // Lista nula é o auth-api fora do ar; ausente na lista é usuário fora
        // do condomínio. Tratar os dois igual transformaria uma falha
        // temporária em "esse usuário não existe", que é mentira.
        if (lista == null) {
            throw new OperacaoInvalidaException(
                    "Não foi possível confirmar o destinatário agora. Tente de novo.");
        }

        AuthClient.Usuario destino = lista.stream()
                .filter(u -> destinatarioId.equals(u.id()))
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Destinatário não encontrado neste condomínio."));

        if (!destino.alcancavel()) {
            throw new DadosInvalidosException(
                    "Esse usuário não acessa o sistema e não recebe mensagens.");
        }
        return destino.id();
    }

    private Mensagem gravarMensagem(Conversa conversa, String corpo) {
        Mensagem m = new Mensagem();
        m.setConversaId(conversa.getId());
        m.setAutorId(usuarioAtual());
        m.setAutorPerfil(Autorizacao.perfilAtual());
        m.setCorpo(corpo);
        m = mensagemRepository.save(m);

        conversa.setUltimaMensagemEm(m.getCriadaEm());
        conversaRepository.save(conversa);
        return m;
    }

    private void garantirParticipante(Long conversaId, String usuarioId) {
        if (participanteRepository.existsByConversaIdAndUsuarioId(conversaId, usuarioId)) return;

        ConversaParticipante p = new ConversaParticipante();
        p.setConversaId(conversaId);
        p.setUsuarioId(usuarioId);
        participanteRepository.save(p);
    }

    private void marcarLeitura(Long conversaId, String usuarioId) {
        participanteRepository.findByConversaIdAndUsuarioId(conversaId, usuarioId)
                .ifPresent(p -> {
                    p.setUltimaLeituraEm(LocalDateTime.now());
                    participanteRepository.save(p);
                });
    }

    private long contarNaoLidasDa(Long conversaId) {
        LocalDateTime desde = participanteRepository
                .findByConversaIdAndUsuarioId(conversaId, usuarioAtual())
                .map(ConversaParticipante::getUltimaLeituraEm)
                .orElse(null);

        return desde == null
                ? mensagemRepository.contarTodasDeOutros(conversaId, usuarioAtual())
                : mensagemRepository.contarNaoLidasDesde(conversaId, usuarioAtual(), desde);
    }

    /**
     * Mapa `usuarioId -> pessoa` do condomínio.
     *
     * Só para a gestão: a listagem que o morador alcança é recortada na unidade
     * dele e não traz a administração. Para ele a contraparte é sempre "a
     * administração", então nome e foto de quem responde não acrescentam nada.
     *
     * Uma chamada por listagem, não uma por conversa.
     */
    private Map<String, AuthClient.Usuario> diretorio(String authorization) {
        if (!Autorizacao.ehGestao()) return Map.of();

        List<AuthClient.Usuario> usuarios = authClient
                .listarUsuariosDoCondominio(condominioAtual(), authorization);
        if (usuarios == null) return Map.of();

        Map<String, AuthClient.Usuario> mapa = new HashMap<>();
        for (AuthClient.Usuario u : usuarios) {
            if (u.id() != null) mapa.put(u.id(), u);
        }
        return mapa;
    }

    /**
     * Com quem é a conversa, na perspectiva de quem está olhando.
     *
     * Não é "quem criou": numa conversa direta que a própria gestão abriu, quem
     * criou é ela mesma, e o nome dela como título não identifica nada. O que
     * identifica é o outro lado.
     *
     * A conversa com a administração ainda sem resposta tem um participante só
     * — o morador. Daí o criador entrar como reserva quando a lista de
     * participantes não oferece ninguém além de quem pergunta.
     */
    private AuthClient.Usuario contraparteDe(Conversa c, Map<String, AuthClient.Usuario> pessoas) {
        if (pessoas.isEmpty()) return null;

        String eu = usuarioAtual();
        String outro = participanteRepository.findByConversaId(c.getId()).stream()
                .map(ConversaParticipante::getUsuarioId)
                .filter(id -> !id.equals(eu))
                .findFirst()
                .orElseGet(() -> c.getCriadaPor().equals(eu) ? null : c.getCriadaPor());

        return outro == null ? null : pessoas.get(outro);
    }

    /**
     * Avisa os outros participantes de que chegou mensagem.
     *
     * Falha aqui não derruba o envio: a mensagem já está gravada, e a conversa
     * a mostra assim que o destinatário abrir. Perder a notificação incomoda;
     * perder a mensagem por causa dela seria bem pior.
     */
    private void notificarOutros(Conversa conversa, Mensagem mensagem, List<String> participantes) {
        String eu = usuarioAtual();
        String resumo = recortar(mensagem.getCorpo(), 140);

        for (String destino : participantes) {
            if (destino.equals(eu)) continue;
            try {
                notificacaoService.criar(destino, conversa.getCondominioId(),
                        TipoNotificacao.CHAT, conversa.getAssunto(), resumo,
                        String.valueOf(conversa.getId()));
            } catch (Exception e) {
                log.warn("falha ao notificar mensagem {}: {}", mensagem.getId(), e.getMessage());
            }
        }
    }

    private static String recortar(String texto, int limite) {
        if (texto == null) return null;
        return texto.length() > limite ? texto.substring(0, limite) + "…" : texto;
    }

    private static String textoObrigatorio(String valor, String mensagemDeErro, int limite) {
        String t = valor == null ? "" : valor.trim();
        if (t.isEmpty()) throw new DadosInvalidosException(mensagemDeErro);
        if (t.length() > limite) {
            throw new DadosInvalidosException("O texto passa de " + limite + " caracteres.");
        }
        return t;
    }

    private static void exigirUsuario() {
        Autorizacao.exigirAcessoAoSistema();
        if (AuthContext.get() == null || AuthContext.get().authUserId() == null) {
            throw new OperacaoInvalidaException("Usuário não autenticado");
        }
    }

    private static String usuarioAtual() {
        JwtClaims c = AuthContext.get();
        return c == null ? null : c.authUserId();
    }

    private static String condominioAtual() {
        String id = CondominioUtils.condominioIdEfetivo();
        if (id == null) {
            throw new DadosInvalidosException("Seu usuário não está vinculado a um condomínio.");
        }
        return id;
    }

    /** Corpo do POST que abre a conversa. */
    public record NovaConversa(
            TipoConversa tipo,
            String assunto,
            String corpo,
            String destinatarioId
    ) {}
}
