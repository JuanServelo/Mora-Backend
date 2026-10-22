package portaria.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import portaria.model.Visitante;
import portaria.security.CondominioUtils;
import portaria.service.VisitanteService;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/visitantes")
@RequiredArgsConstructor
@Tag(name = "Visitantes", description = "Entrada e saída de visitantes cadastrados na portaria.")
public class VisitanteController {

    private final VisitanteService visitanteService;

    @PostMapping("/entrada")
    @Operation(summary = "Registrar entrada de visitante", description = "Cadastra o visitante e registra a entrada, com a unidade de destino.")
    @ApiResponse(responseCode = "201", description = "Entrada registrada.")
    public ResponseEntity<Visitante> registrarEntrada(@Valid @RequestBody Visitante visitante) {
        return ResponseEntity.status(HttpStatus.CREATED).body(visitanteService.registrarEntrada(visitante));
    }

    @PostMapping("/{id}/saida")
    @Operation(summary = "Registrar saída de visitante", description = "Registra a saída do visitante.")
    @ApiResponse(responseCode = "200", description = "Saída registrada.")
    public Visitante registrarSaida(@Parameter(description = "Id do visitante.", example = "b1c2d3e4-f5a6-4b7c-8d9e-0f1a2b3c4d5e") @PathVariable String id) {
        return visitanteService.registrarSaida(id);
    }

    @GetMapping
    @Operation(summary = "Listar visitantes", description = "Visitantes do condomínio.")
    @ApiResponse(responseCode = "200", description = "Visitantes.")
    public List<Visitante> listarTodos() {
        return visitanteService.listarTodos(CondominioUtils.condominioIdEfetivo());
    }

    @GetMapping("/dentro")
    @Operation(summary = "Visitantes dentro", description = "Visitantes com entrada sem saída.")
    @ApiResponse(responseCode = "200", description = "Visitantes.")
    public List<Visitante> listarDentro() {
        return visitanteService.listarDentro(CondominioUtils.condominioIdEfetivo());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe do visitante", description = "Um visitante do condomínio de quem chama.")
    @ApiResponse(responseCode = "200", description = "Visitante.")
    public Visitante buscarPorId(@Parameter(description = "Id do visitante.", example = "b1c2d3e4-f5a6-4b7c-8d9e-0f1a2b3c4d5e") @PathVariable String id) {
        return visitanteService.buscarPorId(id);
    }
}
