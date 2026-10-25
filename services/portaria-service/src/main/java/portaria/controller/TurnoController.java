package portaria.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import portaria.model.Turno;
import portaria.security.CondominioUtils;
import portaria.service.TurnoService;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/turnos")
@RequiredArgsConstructor
@Tag(name = "Turnos", description = "Registro do turno de trabalho na portaria: início, pausas e fim.")
public class TurnoController {

    private final TurnoService turnoService;

    @PostMapping("/iniciar")
    @Operation(summary = "Iniciar turno", description = "Abre o turno do funcionário, registrando a primeira entrada.")
    @ApiResponse(responseCode = "201", description = "Turno iniciado.")
    public ResponseEntity<Turno> iniciar(@Valid @RequestBody Turno turno) {
        return ResponseEntity.status(HttpStatus.CREATED).body(turnoService.iniciar(turno));
    }

    @PostMapping("/{id}/retomar")
    @Operation(summary = "Retomar turno", description = "Registra nova entrada no turno, depois de uma pausa.")
    @ApiResponse(responseCode = "200", description = "Turno retomado.")
    public Turno retomar(@Parameter(description = "Id do turno.", example = "9a8b7c6d-5e4f-4a3b-2c1d-0e9f8a7b6c5d") @PathVariable String id) {
        return turnoService.retomar(id);
    }

    @PostMapping("/{id}/finalizar")
    @Operation(summary = "Finalizar turno", description = "Registra a saída e encerra o turno.")
    @ApiResponse(responseCode = "200", description = "Turno finalizado.")
    public Turno finalizar(@Parameter(description = "Id do turno.", example = "9a8b7c6d-5e4f-4a3b-2c1d-0e9f8a7b6c5d") @PathVariable String id) {
        return turnoService.finalizar(id);
    }

    @GetMapping
    @Operation(summary = "Listar turnos", description = "Turnos do condomínio, com as entradas e saídas.")
    @ApiResponse(responseCode = "200", description = "Turnos.")
    public List<Turno> listarTodos() {
        return turnoService.listarTodos(CondominioUtils.condominioIdEfetivo());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe do turno", description = "Um turno do condomínio de quem chama.")
    @ApiResponse(responseCode = "200", description = "Turno.")
    public Turno buscarPorId(@Parameter(description = "Id do turno.", example = "9a8b7c6d-5e4f-4a3b-2c1d-0e9f8a7b6c5d") @PathVariable String id) {
        return turnoService.buscarPorId(id);
    }
}
