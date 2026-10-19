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

@RestController
@RequestMapping("/avisos")
@RequiredArgsConstructor
public class AvisoController {

    private final AvisoService avisoService;
    private final AvisoLeituraService leituraService;
    private final RelatorioLeituraService relatorioService;
    private final ImagemAvisoService imagemService;

    @PostMapping
    public ResponseEntity<Aviso> criar(@RequestBody Aviso aviso) {
        return ResponseEntity.status(HttpStatus.CREATED).body(avisoService.criar(aviso));
    }

    @GetMapping
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
    public Aviso buscarPorId(@PathVariable UUID id) {
        return avisoService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    public Aviso atualizar(@PathVariable UUID id, @RequestBody Aviso dados) {
        return avisoService.atualizar(id, dados);
    }

    @PatchMapping("/{id}/publicar")
    public Aviso publicar(@PathVariable UUID id) {
        return avisoService.publicar(id);
    }

    @PatchMapping("/{id}/encerrar")
    public Aviso encerrar(@PathVariable UUID id) {
        return avisoService.encerrar(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable UUID id) {
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
    public ResponseEntity<Map<String, Object>> marcarLido(@PathVariable UUID id) {
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
    public Map<String, Object> relatorioLeitura(
            @PathVariable UUID id,
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
