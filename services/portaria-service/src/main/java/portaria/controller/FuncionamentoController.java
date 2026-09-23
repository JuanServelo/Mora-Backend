package portaria.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import portaria.dto.reserva.FuncionamentoDTO;
import portaria.service.FuncionamentoAreaComumService;

/**
 * Funcionamento da área comum (RN-09).
 *
 * Caminho próprio, fora de /areas-comuns, para não colidir com o CRUD
 * existente do cadastro de estruturas.
 */
@RestController
@RequestMapping("/funcionamento")
@RequiredArgsConstructor
public class FuncionamentoController {

    private final FuncionamentoAreaComumService service;

    @GetMapping("/{areaComumId}")
    public FuncionamentoDTO consultar(@PathVariable String areaComumId) {
        return service.consultar(areaComumId);
    }

    @PutMapping("/{areaComumId}")
    public FuncionamentoDTO salvar(@PathVariable String areaComumId,
                                   @RequestBody FuncionamentoDTO dto) {
        return service.salvar(areaComumId, dto);
    }
}
