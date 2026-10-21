package portaria.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import portaria.dto.reserva.FuncionamentoDTO;
import portaria.service.FuncionamentoAreaComumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Funcionamento da área comum (RN-09).
 *
 * Caminho próprio, fora de /areas-comuns, para não colidir com o CRUD
 * existente do cadastro de estruturas.
 */
@RestController
@RequestMapping("/funcionamento")
@RequiredArgsConstructor
@Tag(name = "Áreas comuns", description = "Espaços do condomínio que podem ser reservados (RF-10).")
public class FuncionamentoController {

    private final FuncionamentoAreaComumService service;

    @GetMapping("/{areaComumId}")
    @Operation(summary = "Horário de funcionamento", description = "Modo (sem restrição ou horários definidos) e o horário de cada dia da semana do espaço (RN-09).")
    @ApiResponse(responseCode = "200", description = "Funcionamento.")
    public FuncionamentoDTO consultar(@PathVariable String areaComumId) {
        return service.consultar(areaComumId);
    }

    @PutMapping("/{areaComumId}")
    @Operation(summary = "Definir horário de funcionamento", description = "Grava o modo e os horários do espaço. Reservas fora do horário passam a ser recusadas.")
    @ApiResponse(responseCode = "200", description = "Funcionamento gravado.")
    public FuncionamentoDTO salvar(@PathVariable String areaComumId,
                                   @RequestBody FuncionamentoDTO dto) {
        return service.salvar(areaComumId, dto);
    }
}
