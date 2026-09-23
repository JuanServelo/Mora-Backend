package comunicacao.controller;

import comunicacao.dto.AvisoResponse;
import comunicacao.dto.LeituraAvisoResponse;
import comunicacao.model.Aviso;
import comunicacao.security.CondominioUtils;
import comunicacao.security.PerfilUtils;
import comunicacao.service.AvisoLeituraService;
import comunicacao.service.AvisoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<AvisoResponse> criar(@Valid @RequestBody Aviso aviso) {
        return ResponseEntity.status(HttpStatus.CREATED).body(avisoService.criar(aviso));
    }

    @GetMapping
    public List<AvisoResponse> listar() {
        return avisoService.listarTodos(CondominioUtils.condominioIdEfetivo(), PerfilUtils.usuarioAtual());
    }

    @GetMapping("/ativos")
    public List<AvisoResponse> listarAtivos() {
        return avisoService.listarAtivos(CondominioUtils.condominioIdEfetivo(), PerfilUtils.usuarioAtual());
    }

    @GetMapping("/{id}")
    public AvisoResponse buscarPorId(@PathVariable UUID id) {
        return avisoService.buscarPorId(id, PerfilUtils.usuarioAtual());
    }

    @PutMapping("/{id}")
    public AvisoResponse atualizar(@PathVariable UUID id, @Valid @RequestBody Aviso dados) {
        return avisoService.atualizar(id, dados);
    }

    @PatchMapping("/{id}/publicar")
    public AvisoResponse publicar(@PathVariable UUID id) {
        return avisoService.publicar(id);
    }

    @PatchMapping("/{id}/despublicar")
    public AvisoResponse despublicar(@PathVariable UUID id) {
        return avisoService.despublicar(id);
    }

    @PatchMapping("/{id}/encerrar")
    public AvisoResponse encerrar(@PathVariable UUID id) {
        return avisoService.encerrar(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable UUID id) {
        avisoService.excluir(id);
    }

    /**
     * Registra que o usuario leu o aviso.
     *
     * E o que permite ao sindico comprovar ciencia de uma regra. Idempotente:
     * reabrir o aviso nao gera uma segunda leitura.
     */
    @PostMapping("/{id}/lido")
    public Map<String, Object> marcarLido(@PathVariable UUID id) {
        String usuarioId = PerfilUtils.usuarioAtual();
        var leitura = leituraService.marcarLido(id, usuarioId);
        return Map.of(
                "avisoId", id.toString(),
                "usuarioId", usuarioId,
                "lidoEm", leitura.getLidoEm(),
                "totalLeituras", leituraService.contarLeituras(id));
    }

    /** Percentual de leitura, quem ja leu e quem falta. So para a administracao. */
    @GetMapping("/{id}/leituras")
    public LeituraAvisoResponse estatisticasLeitura(@PathVariable UUID id) {
        PerfilUtils.exigirGestor();
        return leituraService.estatisticas(id);
    }
}
