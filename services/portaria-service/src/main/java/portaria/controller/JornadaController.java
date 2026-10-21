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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Gestão de funcionários e turnos (RF-09).
 *
 * A identidade do funcionário continua no auth-api; aqui vive a vida funcional
 * — situação, jornada, liberações e a trilha de auditoria.
 */
@RestController
@RequestMapping("/jornadas")
@RequiredArgsConstructor
@Tag(name = "Jornadas", description = "Vida funcional e escala dos funcionários (RF-09). A identidade fica no auth-api; aqui ficam situação, jornada e liberações.")
public class JornadaController {

    private final JornadaService jornadaService;
    private final LiberacaoExcepcionalService liberacaoService;

    @GetMapping
    @Operation(summary = "Listar jornadas", description = "A jornada de cada funcionário do condomínio.")
    @ApiResponse(responseCode = "200", description = "Jornadas.")
    public List<JornadaDTO> listar() {
        return jornadaService.listar();
    }

    @GetMapping("/{authUserId}")
    @Operation(summary = "Jornada de um funcionário", description = "Situação, tipo de jornada, ciclo e os dias de trabalho, pelo id do auth-api.")
    @ApiResponse(responseCode = "200", description = "Jornada.")
    public JornadaDTO consultar(@Parameter(description = "Id do funcionário no auth-api.", example = "3") @PathVariable String authUserId) {
        return jornadaService.consultar(authUserId);
    }

    @PutMapping
    @Operation(summary = "Gravar jornada", description = "Cria ou substitui a jornada do funcionário. Cada mudança entra na trilha de auditoria.")
    @ApiResponse(responseCode = "200", description = "Jornada gravada.")
    public JornadaDTO salvar(@RequestBody JornadaDTO dto) {
        return jornadaService.salvar(dto);
    }

    /** Próximos plantões — única forma prática de conferir a escala (RN-03). */
    @GetMapping("/{authUserId}/proximos-plantoes")
    @Operation(summary = "Próximos plantões", description = "Os plantões dos próximos `dias` (padrão 14), calculados da escala: a forma prática de conferi-la (RN-03).")
    @ApiResponse(responseCode = "200", description = "Plantões.")
    public List<JornadaUtils.ProximoPlantao> proximosPlantoes(
            @Parameter(description = "Id do funcionário no auth-api.", example = "3") @PathVariable String authUserId,
            @RequestParam(defaultValue = "14") int dias) {
        return jornadaService.proximosPlantoes(authUserId, dias);
    }

    /** Prévia antes de confirmar mudança de âncora do ciclo. */
    @PostMapping("/previa")
    @Operation(summary = "Prévia da escala", description = "Os plantões que a jornada enviada produziria, sem gravar. Usada antes de confirmar mudança de ciclo.")
    @ApiResponse(responseCode = "200", description = "Plantões previstos.")
    public List<JornadaUtils.ProximoPlantao> previa(@RequestBody JornadaDTO dto,
                                                    @RequestParam(defaultValue = "14") int dias) {
        return jornadaService.previa(dto, dias);
    }

    /** Consultada pela portaria antes de registrar a entrada do funcionário. */
    @GetMapping("/{authUserId}/avaliar-entrada")
    @Operation(summary = "Pode entrar agora?", description = "Consultada pela portaria (auth-api) antes de registrar a entrada: funcionário ativo, dentro do turno ou com liberação excepcional.")
    @ApiResponse(responseCode = "200", description = "Avaliação.")
    public AvaliacaoEntradaDTO avaliarEntrada(
            @Parameter(description = "Id do funcionário no auth-api.", example = "3") @PathVariable String authUserId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime instante) {
        return jornadaService.avaliarEntrada(authUserId,
                instante != null ? instante : LocalDateTime.now());
    }

    /** Somente leitura: a trilha não tem edição nem exclusão, de propósito. */
    @GetMapping("/{authUserId}/auditoria")
    @Operation(summary = "Auditoria da jornada", description = "Trilha somente leitura das mudanças na jornada do funcionário.")
    @ApiResponse(responseCode = "200", description = "Registros.")
    public List<FuncionarioAuditoria> auditoria(@Parameter(description = "Id do funcionário no auth-api.", example = "3") @PathVariable String authUserId) {
        return jornadaService.auditoriaDe(authUserId);
    }

    // ─── Liberações excepcionais (RN-05) ─────────────────────────────────────

    @GetMapping("/liberacoes/todas")
    @Operation(summary = "Liberações excepcionais", description = "Autorizações para o funcionário entrar fora do turno.")
    @ApiResponse(responseCode = "200", description = "Liberações.")
    public List<LiberacaoExcepcional> listarLiberacoes() {
        return liberacaoService.listar();
    }

    @PostMapping("/liberacoes")
    @Operation(summary = "Criar liberação excepcional", description = "Autoriza o funcionário a entrar num dia e horário fora da escala, com motivo.")
    @ApiResponse(responseCode = "200", description = "Liberação criada.")
    public LiberacaoExcepcional criarLiberacao(@RequestBody LiberacaoExcepcional pedido) {
        return liberacaoService.criar(pedido);
    }

    @DeleteMapping("/liberacoes/{id}")
    @Operation(summary = "Cancelar liberação", description = "Cancela uma liberação ainda não utilizada.")
    @ApiResponse(responseCode = "200", description = "Cancelada.")
    public void cancelarLiberacao(@Parameter(description = "Id da liberação.", example = "d4e5f6a7-b8c9-4d0e-9f1a-2b3c4d5e6f7a") @PathVariable String id) {
        liberacaoService.cancelar(id);
    }
}
