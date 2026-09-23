package comunicacao.controller;

import comunicacao.dto.AvisoLidoDTO;
import comunicacao.exception.OperacaoInvalidaException;
import comunicacao.model.Aviso;
import comunicacao.security.AuthContext;
import comunicacao.security.CondominioUtils;
import comunicacao.security.JwtClaims;
import comunicacao.service.AvisoLeituraService;
import comunicacao.service.AvisoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    @GetMapping("/ativos")
    public List<AvisoLidoDTO> listarAtivos() {
        UUID userId = currentUserId();
        Map<UUID, LocalDateTime> quandoLeu = leituraService.quandoLeu(userId);
        return avisoService.listarAtivos(CondominioUtils.condominioIdEfetivo()).stream()
                .map(a -> AvisoLidoDTO.de(a, quandoLeu.get(a.getId())))
                .toList();
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

    @PostMapping("/{id}/lido")
    public ResponseEntity<Map<String, Object>> marcarLido(@PathVariable UUID id) {
        UUID userId = currentUserId();
        leituraService.marcarLido(id, userId);
        return ResponseEntity.ok(Map.of(
                "avisoId", id,
                "usuarioId", userId,
                "totalLeituras", leituraService.contarLeituras(id)
        ));
    }

    @GetMapping("/{id}/leituras")
    public Map<String, Object> estatisticasLeitura(@PathVariable UUID id) {
        return Map.of(
                "avisoId", id,
                "totalLeituras", leituraService.contarLeituras(id)
        );
    }

    private UUID currentUserId() {
        JwtClaims claims = AuthContext.get();
        if (claims == null || claims.authUserId() == null) {
            throw new OperacaoInvalidaException("Usuário não autenticado");
        }
        return UUID.fromString(claims.authUserId());
    }
}
