package comunicacao.controller;

import comunicacao.exception.OperacaoInvalidaException;
import comunicacao.model.Notificacao;
import comunicacao.model.enums.TipoNotificacao;
import comunicacao.security.AuthContext;
import comunicacao.security.Autorizacao;
import comunicacao.security.JwtClaims;
import comunicacao.service.ChatService;
import comunicacao.service.ConversaService;
import comunicacao.service.NotificacaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/notificacoes")
@RequiredArgsConstructor
public class NotificacaoController {

    private final NotificacaoService notificacaoService;
    private final ChatService chatService;
    private final ConversaService conversaService;

    @GetMapping
    public Page<Notificacao> listar(@PageableDefault(size = 20) Pageable pageable) {
        return notificacaoService.listar(currentUserId(), pageable);
    }

    @GetMapping("/nao-lidas")
    public List<Notificacao> listarNaoLidas() {
        return notificacaoService.listarNaoLidas(currentUserId());
    }

    @GetMapping("/contador")
    public Map<String, Long> contador() {
        return Map.of("naoLidas", notificacaoService.contarNaoLidas(currentUserId()));
    }

    /**
     * O que o sino precisa mostrar, em uma chamada.
     *
     * São duas contagens, e não uma por redundância: a primeira mensagem que um
     * morador manda para a administração **não gera notificação para ninguém** —
     * não há destinatário nomeado a quem endereçá-la. Sem a segunda contagem, o
     * síndico não veria nada acender até abrir a tela por conta própria.
     */
    @GetMapping("/resumo")
    public Map<String, Long> resumo() {
        String userId = currentUserId();
        // Soma os dois modelos de conversa que o serviço tem: o chat direto
        // entre dois usuários e as conversas com assunto, que incluem o formato
        // ADMINISTRACAO. Para quem olha o sino as duas coisas são a mesma —
        // "tem mensagem esperando".
        long conversas = chatService.contarNaoLidas(userId)
                + conversaService.contarConversasNaoLidas();

        return Map.of(
                "naoLidas", notificacaoService.contarNaoLidas(userId),
                "conversasNaoLidas", conversas
        );
    }

    @PatchMapping("/{id}/lida")
    public Notificacao marcarLida(@PathVariable String id) {
        return notificacaoService.marcarLida(id);
    }

    @PatchMapping("/todas-lidas")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void marcarTodasLidas() {
        notificacaoService.marcarTodasLidas(currentUserId());
    }

    /**
     * Publica uma notificação para outro usuário.
     *
     * O `/admin` no caminho era só o nome: **qualquer usuário autenticado
     * chamava esta rota** e escrevia na caixa de entrada de qualquer pessoa, em
     * qualquer condomínio, com o título e o texto que quisesse. É o caminho
     * pronto para um golpe — uma notificação convincente, vinda do sistema,
     * mandando o morador fazer alguma coisa.
     *
     * O `condominioId` continua vindo por parâmetro porque a rota é da gestão,
     * mas agora só a gestão chega até aqui.
     */
    @PostMapping("/admin")
    public ResponseEntity<Notificacao> criarAdmin(
            @RequestParam String destinatarioId,
            @RequestParam String condominioId,
            @RequestParam TipoNotificacao tipo,
            @RequestParam String titulo,
            @RequestBody String mensagem) {
        Autorizacao.exigirGestaoDoCondominio("enviar notificações");
        return ResponseEntity.ok(notificacaoService.criar(
                destinatarioId, condominioId, tipo, titulo, mensagem, null));
    }

    /**
     * O id de quem está na requisição, como o token o traz.
     *
     * Sem conversão: o `auth-api` numera usuários com `integer`, e
     * `UUID.fromString("32")` derrubava esta rota com 500.
     */
    private String currentUserId() {
        JwtClaims claims = AuthContext.get();
        if (claims == null || claims.authUserId() == null) {
            throw new OperacaoInvalidaException("Usuário não autenticado");
        }
        return claims.authUserId();
    }
}
