package comunicacao.controller;

import comunicacao.dto.AvisoLidoDTO;
import comunicacao.exception.OperacaoInvalidaException;
import comunicacao.model.Aviso;
import comunicacao.security.AuthContext;
import comunicacao.security.Autorizacao;
import comunicacao.security.CondominioUtils;
import comunicacao.security.JwtClaims;
import comunicacao.security.PublicoAlvoRegra;
import comunicacao.exception.DadosInvalidosException;
import comunicacao.exception.RecursoNaoEncontradoException;
import comunicacao.service.AvisoLeituraService;
import comunicacao.service.AvisoService;
import comunicacao.service.ImagemAvisoService;
import comunicacao.service.RelatorioLeituraService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/avisos")
@RequiredArgsConstructor
@Tag(name = "Avisos", description = "Comunicados da administração (RF-12): publicação, vigência, público-alvo, confirmação de leitura e imagem.")
public class AvisoController {

    private final AvisoService avisoService;
    private final AvisoLeituraService leituraService;
    private final RelatorioLeituraService relatorioService;
    private final ImagemAvisoService imagemService;

    @PostMapping
    @Operation(summary = "Publicar aviso", description = """
            Cria um aviso no condomínio de quem chama. Com `publicado: false`, fica como rascunho, visível só para a gestão. \
            O condomínio sai do token, e o autor, se vier vazio, vira o e-mail do token. Perfil: ADMIN_SINDICO.""")
    @ApiResponse(responseCode = "201", description = "Aviso criado.")
    public ResponseEntity<Aviso> criar(@RequestBody Aviso aviso) {
        return ResponseEntity.status(HttpStatus.CREATED).body(avisoService.criar(aviso));
    }

    @GetMapping
    @Operation(summary = "Listar avisos do condomínio", description = """
            Todos os avisos, inclusive rascunhos e vencidos, mais novos primeiro. É a visão da gestão. \
            Perfis: ADMIN_SINDICO e ADMIN_GERAL (este, de todos os condomínios).""")
    @ApiResponse(responseCode = "200", description = "Avisos.")
    public List<Aviso> listar() {
        return avisoService.listarTodos(CondominioUtils.condominioIdEfetivo());
    }

    /**
     * Os avisos vigentes, marcados com o que este usuário já leu.
     *
     * A lista vem de `avisos` e o "já li" de `aviso_leituras`; juntar aqui evita
     * a tela fazer duas chamadas e cruzar sozinha.
     */
    //
    // Cada um recebe só os avisos de que é destinatário: o morador deixa de ver
    // comunicado dirigido a funcionário. A gestão recebe todos — precisa ver o
    // que publicou —, marcados com `destinatario: false`.
    @GetMapping("/ativos")
    @Operation(summary = "Avisos vigentes para mim", description = """
            Os avisos publicados e vigentes de que quem chama é destinatário, cada um marcado com `lido` e `lidoEm`. \
            Morador não recebe aviso para funcionários. A gestão recebe todos, com `destinatario: false`. \
            Qualquer perfil, menos CONVIDADO e TERCEIRO.""")
    @ApiResponse(responseCode = "200", description = "Avisos vigentes.")
    public List<AvisoLidoDTO> listarAtivos() {
        String userId = currentUserId();
        String perfil = Autorizacao.perfilAtual();
        boolean gestao = Autorizacao.ehGestao();
        Map<UUID, LocalDateTime> quandoLeu = leituraService.quandoLeu(userId);
        return avisoService.listarAtivos(CondominioUtils.condominioIdEfetivo()).stream()
                .map(a -> AvisoLidoDTO.de(a, quandoLeu.get(a.getId()),
                        PublicoAlvoRegra.ehDestinatario(a.getPublicoAlvo(), perfil)))
                .filter(dto -> dto.destinatario() || gestao)
                .toList();
    }

    /**
     * Panorama de leitura: os avisos vigentes, cada um com quantos confirmaram.
     *
     * Declarada antes de `/{id}`, mas o Spring já prefere o caminho literal ao
     * com variável — `leituras` nunca é lido como id.
     */
    @GetMapping("/leituras")
    @Operation(summary = "Panorama de leitura", description = """
            Os avisos vigentes do condomínio, cada um com o total de confirmações, contado numa consulta agrupada. \
            Perfis: ADMIN_SINDICO e ADMIN_GERAL.""")
    @ApiResponse(responseCode = "200", description = "Avisos com o total de confirmações.", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
            {
              "sucesso": true,
              "avisos": [
                {
                  "id": "766730a5-0658-475f-89fe-aad79bb287c1",
                  "titulo": "Manutenção da caixa d'água",
                  "publicoAlvo": "MORADORES",
                  "dataInicio": "2026-10-20",
                  "dataFim": "2026-10-31",
                  "autor": "sindico.verde@mora.test",
                  "imagemUrl": null,
                  "confirmacoes": 2
                }
              ]
            }""")))
    public Map<String, Object> panorama() {
        return Map.of("sucesso", true, "avisos", relatorioService.panorama());
    }

    /**
     * Envia a imagem de um aviso. Devolve a URL a gravar em `imagemUrl`.
     *
     * Só quem publica envia imagem: sem esta trava, qualquer morador usaria o
     * serviço como depósito de arquivos.
     */
    @PostMapping(value = "/imagem", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Enviar imagem do aviso", description = """
            Recebe PNG ou JPEG de até 5 MB e 4000 px por lado, no campo `imagem`. O arquivo gravado é o re-encode da \
            imagem, nunca os bytes recebidos: texto renomeado para .png é recusado. Devolve a `url` a gravar em \
            `imagemUrl`. Perfil: ADMIN_SINDICO.""")
    @ApiResponse(responseCode = "201", description = "Imagem gravada.", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
            {
              "sucesso": true,
              "url": "/uploads/avisos/3f2b9c1e-8a4d-4e7f-b6c2-1d9e0a5f7b3c.jpg"
            }""")))
    public ResponseEntity<Map<String, Object>> enviarImagem(@RequestParam("imagem") MultipartFile imagem) {
        Autorizacao.exigirGestaoDoCondominio("enviar imagens de comunicados");
        try {
            String url = imagemService.guardar(imagem.getBytes());
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("sucesso", true, "url", url));
        } catch (IOException e) {
            throw new DadosInvalidosException("Não foi possível ler o arquivo enviado.");
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe do aviso", description = """
            Um aviso do condomínio de quem chama. Aviso de outro condomínio responde 404, não 403. \
            Perfis: ADMIN_SINDICO e ADMIN_GERAL.""")
    @ApiResponse(responseCode = "200", description = "Aviso.")
    public Aviso buscarPorId(@Parameter(description = "Id do aviso.", example = "766730a5-0658-475f-89fe-aad79bb287c1") @PathVariable UUID id) {
        return avisoService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar aviso", description = "Substitui título, mensagem, vigência, público e imagem. Perfil: ADMIN_SINDICO.")
    @ApiResponse(responseCode = "200", description = "Aviso atualizado.")
    public Aviso atualizar(@Parameter(description = "Id do aviso.", example = "766730a5-0658-475f-89fe-aad79bb287c1") @PathVariable UUID id, @RequestBody Aviso dados) {
        return avisoService.atualizar(id, dados);
    }

    @PatchMapping("/{id}/publicar")
    @Operation(summary = "Publicar rascunho", description = "Torna o aviso visível aos destinatários durante a vigência. Perfil: ADMIN_SINDICO.")
    @ApiResponse(responseCode = "200", description = "Aviso publicado.")
    public Aviso publicar(@Parameter(description = "Id do aviso.", example = "766730a5-0658-475f-89fe-aad79bb287c1") @PathVariable UUID id) {
        return avisoService.publicar(id);
    }

    @PatchMapping("/{id}/encerrar")
    @Operation(summary = "Encerrar aviso", description = "Antecipa o fim da vigência para hoje: o aviso some da tela dos moradores. Perfil: ADMIN_SINDICO.")
    @ApiResponse(responseCode = "200", description = "Aviso encerrado.")
    public Aviso encerrar(@Parameter(description = "Id do aviso.", example = "766730a5-0658-475f-89fe-aad79bb287c1") @PathVariable UUID id) {
        return avisoService.encerrar(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Excluir aviso", description = "Apaga o aviso e as confirmações de leitura dele. Perfil: ADMIN_SINDICO.")
    @ApiResponse(responseCode = "204", description = "Excluído.")
    public void excluir(@Parameter(description = "Id do aviso.", example = "766730a5-0658-475f-89fe-aad79bb287c1") @PathVariable UUID id) {
        avisoService.excluir(id);
    }

    /**
     * Registra que este usuário leu o aviso.
     *
     * O aviso é buscado pelo serviço, e não direto pelo repositório, porque é
     * lá que mora o recorte de condomínio. Sem passar por ele, qualquer usuário
     * confirmava leitura de aviso de qualquer condomínio — e a confirmação
     * entrava no relatório do síndico de lá, contando alguém que nunca foi
     * destinatário.
     */
    @PostMapping("/{id}/lido")
    @Operation(summary = "Confirmar leitura", description = """
            Registra que quem chama leu o aviso. Confirmar de novo não muda nada: vale a data da primeira. \
            Quem não é destinatário recebe 404. Qualquer perfil, menos CONVIDADO e TERCEIRO.""")
    @ApiResponse(responseCode = "200", description = "Leitura registrada.", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
            {
              "avisoId": "766730a5-0658-475f-89fe-aad79bb287c1",
              "usuarioId": "4",
              "totalLeituras": 3
            }""")))
    public ResponseEntity<Map<String, Object>> marcarLido(@Parameter(description = "Id do aviso.", example = "766730a5-0658-475f-89fe-aad79bb287c1") @PathVariable UUID id) {
        Aviso aviso = avisoService.buscarPorId(id);
        // Quem não é destinatário não confirma leitura: a confirmação entraria
        // no relatório como alguém que devia ler e leu. 404, como o resto das
        // rotas por id — "existe, mas não é para você" já entrega que existe.
        if (!PublicoAlvoRegra.ehDestinatario(aviso.getPublicoAlvo(), Autorizacao.perfilAtual())) {
            throw new RecursoNaoEncontradoException("Aviso não encontrado com id: " + id);
        }

        String userId = currentUserId();
        leituraService.marcarLido(id, userId);
        return ResponseEntity.ok(Map.of(
                "avisoId", id,
                "usuarioId", userId,
                "totalLeituras", leituraService.contarLeituras(id)
        ));
    }

    /**
     * Quem deveria ler este aviso e quem já confirmou — relatório da administração.
     *
     * `totalLeituras` continua na resposta para quem já consumia a versão antiga
     * (o painel do síndico, no gestao-geral).
     */
    @GetMapping("/{id}/leituras")
    @Operation(summary = "Relatório de leitura", description = """
            Quem deveria ler o aviso (pela regra de público-alvo, sobre os usuários do condomínio no auth-api) e quem já \
            confirmou, com os que faltam primeiro. Se o auth-api não responder, `total` vem nulo e `observacao` explica: \
            nunca um total inventado. Perfis: ADMIN_SINDICO e ADMIN_GERAL.""")
    @ApiResponse(responseCode = "200", description = "Relatório.", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
            {
              "avisoId": "766730a5-0658-475f-89fe-aad79bb287c1",
              "total": 6,
              "confirmados": 2,
              "observacao": null,
              "totalLeituras": 2,
              "destinatarios": [
                { "usuarioId": "5", "nome": "Bruno Lima", "perfil": "MORADOR", "lido": false, "lidoEm": null },
                { "usuarioId": "4", "nome": "Ana Souza", "perfil": "MORADOR", "lido": true, "lidoEm": "2026-10-19T07:48:12" }
              ]
            }""")))
    public Map<String, Object> relatorioLeitura(
            @Parameter(description = "Id do aviso.", example = "766730a5-0658-475f-89fe-aad79bb287c1") @PathVariable UUID id,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        Map<String, Object> relatorio = new java.util.HashMap<>(relatorioService.relatorio(id, authorization));
        relatorio.put("totalLeituras", leituraService.contarLeituras(id));
        return relatorio;
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
