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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springdoc.core.annotations.ParameterObject;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/notificacoes")
@RequiredArgsConstructor
@Tag(name = "Notificações", description = "A caixa de notificações de cada usuário: o que vem do comunicacao e o que outros serviços publicam (RF-13).")
public class NotificacaoController {

    private final NotificacaoService notificacaoService;
    private final ChatService chatService;
    private final ConversaService conversaService;

    @GetMapping
    @Operation(summary = "Minha caixa de notificações", description = "As notificações de quem chama, mais novas primeiro, paginadas (padrão 20 por página).")
    @ApiResponse(responseCode = "200", description = "Página de notificações.", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
            {
              "content": [ { "id": "5b9e2f1c-7d4a-4c8e-a3b6-9f0d1e2c7a45", "destinatarioId": "4", "condominioId": "cond-parque-verde", "tipo": "MULTA_APLICADA",
                  "titulo": "Multa aplicada à sua unidade", "mensagem": "Barulho após as 22h em dia de semana — R$ 150,00. Você pode recorrer até 28/10.",
                  "referenciaId": "9c1f4e2a-6b7d-4f3a-a2c8-51e0d7b3f6a9", "lida": false, "criadoEm": "2026-10-13T14:20:05", "lidaEm": null,
                  "origem": "financeiro", "chaveUnica": "financeiro:MULTA_APLICADA:9c1f4e2a-6b7d-4f3a-a2c8-51e0d7b3f6a9",
                  "dados": { "multaId": "9c1f4e2a-6b7d-4f3a-a2c8-51e0d7b3f6a9", "valorCentavos": 15000 } } ],
              "totalElements": 7,
              "totalPages": 1,
              "number": 0,
              "size": 20,
              "first": true,
              "last": true,
              "empty": false
            }""")))
    public Page<Notificacao> listar(@ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return notificacaoService.listar(currentUserId(), pageable);
    }

    @GetMapping("/nao-lidas")
    @Operation(summary = "Notificações não lidas", description = "As notificações que quem chama ainda não leu.")
    @ApiResponse(responseCode = "200", description = "Notificações.", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
            [ { "id": "5b9e2f1c-7d4a-4c8e-a3b6-9f0d1e2c7a45", "destinatarioId": "4", "condominioId": "cond-parque-verde", "tipo": "MULTA_APLICADA",
                  "titulo": "Multa aplicada à sua unidade", "mensagem": "Barulho após as 22h em dia de semana — R$ 150,00. Você pode recorrer até 28/10.",
                  "referenciaId": "9c1f4e2a-6b7d-4f3a-a2c8-51e0d7b3f6a9", "lida": false, "criadoEm": "2026-10-13T14:20:05", "lidaEm": null,
                  "origem": "financeiro", "chaveUnica": "financeiro:MULTA_APLICADA:9c1f4e2a-6b7d-4f3a-a2c8-51e0d7b3f6a9",
                  "dados": { "multaId": "9c1f4e2a-6b7d-4f3a-a2c8-51e0d7b3f6a9", "valorCentavos": 15000 } } ]""")))
    public List<Notificacao> listarNaoLidas() {
        return notificacaoService.listarNaoLidas(currentUserId());
    }

    @GetMapping("/contador")
    @Operation(summary = "Contador de não lidas", description = "Quantas notificações quem chama ainda não leu.")
    @ApiResponse(responseCode = "200", description = "Contagem.", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
            { "naoLidas": 3 }""")))
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
    @Operation(summary = "Resumo para o sino", description = """
            Notificações não lidas e conversas com mensagem nova, numa chamada. As duas contagens existem porque a \
            primeira mensagem de um morador à administração não gera notificação para ninguém.""")
    @ApiResponse(responseCode = "200", description = "Contagens.", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
            { "naoLidas": 3, "conversasNaoLidas": 1 }""")))
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
    @Operation(summary = "Marcar como lida", description = "Marca uma notificação de quem chama como lida.")
    @Parameter(name = "id", description = "Id da notificação.", example = "5b9e2f1c-7d4a-4c8e-a3b6-9f0d1e2c7a45")
    @ApiResponse(responseCode = "200", description = "Notificação marcada.")
    public Notificacao marcarLida(@PathVariable String id) {
        return notificacaoService.marcarLida(id);
    }

    @PatchMapping("/todas-lidas")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    @Operation(summary = "Marcar todas como lidas", description = "Zera o contador do sino de quem chama.")
    @ApiResponse(responseCode = "204", description = "Marcadas.")
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
    @Operation(summary = "Enviar notificação a um usuário", description = """
            A gestão envia uma notificação a um usuário do condomínio. O corpo é o texto da mensagem, como texto puro. \
            Perfil: ADMIN_SINDICO.""")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Texto da notificação, como texto puro.", required = true,
            content = {@Content(mediaType = "text/plain", examples = @ExampleObject(value = "A coleta de lixo reciclável passa a ser às terças e sextas.")),
                @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"A coleta de lixo reciclável passa a ser às terças e sextas.\""))})
    @ApiResponse(responseCode = "200", description = "Notificação enviada.")
    public ResponseEntity<Notificacao> criarAdmin(
            @Parameter(description = "Id de quem recebe (auth-api).", example = "4") @RequestParam String destinatarioId,
            @Parameter(description = "Condomínio do destinatário.", example = "cond-parque-verde") @RequestParam String condominioId,
            @Parameter(description = "Tipo da notificação.", example = "SISTEMA") @RequestParam TipoNotificacao tipo,
            @Parameter(description = "Título mostrado no sino.", example = "Coleta de lixo reciclável") @RequestParam String titulo,
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
