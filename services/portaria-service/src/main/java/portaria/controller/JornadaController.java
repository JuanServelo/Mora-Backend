package portaria.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import portaria.dto.jornada.AvaliacaoEntradaDTO;
import portaria.dto.jornada.JornadaDTO;
import portaria.model.FuncionarioAuditoria;
import portaria.model.LiberacaoExcepcional;
import portaria.service.JornadaService;
import portaria.service.LiberacaoExcepcionalService;
import portaria.util.JornadaUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Gestão de funcionários e turnos (RF-09).
 *
 * A identidade do funcionário continua no auth-api; aqui vive a vida funcional
 * — situação, jornada, liberações e a trilha de auditoria.
 */
@RestController
@RequestMapping("/jornadas")
@RequiredArgsConstructor
public class JornadaController {

    private final JornadaService jornadaService;
    private final LiberacaoExcepcionalService liberacaoService;

    @GetMapping
    public List<JornadaDTO> listar() {
        return jornadaService.listar();
    }

    @GetMapping("/{authUserId}")
    public JornadaDTO consultar(@PathVariable String authUserId) {
        return jornadaService.consultar(authUserId);
    }

    @PutMapping
    public JornadaDTO salvar(@RequestBody JornadaDTO dto) {
        return jornadaService.salvar(dto);
    }

    /** Próximos plantões — única forma prática de conferir a escala (RN-03). */
    @GetMapping("/{authUserId}/proximos-plantoes")
    public List<JornadaUtils.ProximoPlantao> proximosPlantoes(
            @PathVariable String authUserId,
            @RequestParam(defaultValue = "14") int dias) {
        return jornadaService.proximosPlantoes(authUserId, dias);
    }

    /** Prévia antes de confirmar mudança de âncora do ciclo. */
    @PostMapping("/previa")
    public List<JornadaUtils.ProximoPlantao> previa(@RequestBody JornadaDTO dto,
                                                    @RequestParam(defaultValue = "14") int dias) {
        return jornadaService.previa(dto, dias);
    }

    /** Consultada pela portaria antes de registrar a entrada do funcionário. */
    @GetMapping("/{authUserId}/avaliar-entrada")
    public AvaliacaoEntradaDTO avaliarEntrada(
            @PathVariable String authUserId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime instante) {
        return jornadaService.avaliarEntrada(authUserId,
                instante != null ? instante : LocalDateTime.now());
    }

    /** Somente leitura: a trilha não tem edição nem exclusão, de propósito. */
    @GetMapping("/{authUserId}/auditoria")
    public List<FuncionarioAuditoria> auditoria(@PathVariable String authUserId) {
        return jornadaService.auditoriaDe(authUserId);
    }

    // ─── Liberações excepcionais (RN-05) ─────────────────────────────────────

    @GetMapping("/liberacoes/todas")
    public List<LiberacaoExcepcional> listarLiberacoes() {
        return liberacaoService.listar();
    }

    @PostMapping("/liberacoes")
    public LiberacaoExcepcional criarLiberacao(@RequestBody LiberacaoExcepcional pedido) {
        return liberacaoService.criar(pedido);
    }

    @DeleteMapping("/liberacoes/{id}")
    public void cancelarLiberacao(@PathVariable String id) {
        liberacaoService.cancelar(id);
    }
}
