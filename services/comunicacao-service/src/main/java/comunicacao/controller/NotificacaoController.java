package comunicacao.controller;

import comunicacao.dto.NotificacaoAdminRequest;
import comunicacao.dto.PreferenciasRequest;
import comunicacao.model.Notificacao;
import comunicacao.model.enums.TipoNotificacao;
import comunicacao.security.CondominioUtils;
import comunicacao.security.PerfilUtils;
import comunicacao.service.NotificacaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notificacoes")
@RequiredArgsConstructor
public class NotificacaoController {

    private final NotificacaoService notificacaoService;

    @GetMapping
    public Page<Notificacao> listar(@PageableDefault(size = 20) Pageable pageable) {
        return notificacaoService.listar(PerfilUtils.usuarioAtual(), pageable);
    }

    @GetMapping("/nao-lidas")
    public List<Notificacao> listarNaoLidas() {
        return notificacaoService.listarNaoLidas(PerfilUtils.usuarioAtual());
    }

    @GetMapping("/contador")
    public Map<String, Long> contador() {
        return Map.of("naoLidas", notificacaoService.contarNaoLidas(PerfilUtils.usuarioAtual()));
    }

    @PatchMapping("/{id}/lida")
    public Notificacao marcarLida(@PathVariable String id) {
        return notificacaoService.marcarLida(id, PerfilUtils.usuarioAtual());
    }

    @PatchMapping("/todas-lidas")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marcarTodasLidas() {
        notificacaoService.marcarTodasLidas(PerfilUtils.usuarioAtual());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable String id) {
        notificacaoService.excluir(id, PerfilUtils.usuarioAtual());
    }

    /* ------------------------------------------------------ preferencias --- */

    /**
     * Categorias existentes e quais o usuario silenciou.
     *
     * A tela precisa das duas coisas: sem a lista de tipos ela nao sabe o que
     * oferecer, e sem as silenciadas nao sabe o que marcar.
     */
    @GetMapping("/preferencias")
    public Map<String, Object> preferencias() {
        return Map.of(
                "categorias", TipoNotificacao.values(),
                "silenciadas", notificacaoService.silenciadas(PerfilUtils.usuarioAtual()));
    }

    @PutMapping("/preferencias")
    public Map<String, Object> salvarPreferencias(@RequestBody PreferenciasRequest req) {
        return Map.of(
                "categorias", TipoNotificacao.values(),
                "silenciadas", notificacaoService.salvarPreferencias(
                        PerfilUtils.usuarioAtual(), req.silenciadas()));
    }

    /* ------------------------------------------------------------- admin --- */

    /** Envio manual pela administracao — util para comunicados pontuais. */
    @PostMapping("/admin")
    public Map<String, Object> criarAdmin(@Valid @RequestBody NotificacaoAdminRequest req) {
        PerfilUtils.exigirGestor();
        int criadas = notificacaoService.criarParaVarios(
                req.destinatarioIds(),
                CondominioUtils.condominioIdEfetivo(),
                req.tipo() == null ? TipoNotificacao.SISTEMA : req.tipo(),
                req.titulo(),
                req.mensagem(),
                req.referenciaId());
        return Map.of("criadas", criadas, "solicitadas", req.destinatarioIds().size());
    }
}
