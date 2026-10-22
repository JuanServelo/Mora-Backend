package portaria.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import portaria.dto.EntregaRequestDTO;
import portaria.dto.EntregaResponseDTO;
import portaria.security.CondominioUtils;
import portaria.service.EntregaService;

import java.util.List;
import java.util.UUID;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/entregas")
@RequiredArgsConstructor
@Tag(name = "Entregas", description = "Encomendas recebidas pela portaria e retiradas pelo morador (RF-07).")
public class EntregaController {

    private final EntregaService entregaService;

    @PostMapping("/cadastrar")
    @Operation(summary = "Registrar entrega", description = "Registra a encomenda recebida para um morador.")
    @ApiResponse(responseCode = "201", description = "Entrega registrada.")
    public ResponseEntity<EntregaResponseDTO> cadastrar(@RequestBody EntregaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(EntregaResponseDTO.fromEntity(entregaService.cadastrar(dto)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar entrega", description = "Corrige os dados da encomenda.")
    @ApiResponse(responseCode = "200", description = "Entrega atualizada.")
    public EntregaResponseDTO atualizar(@Parameter(description = "Id da entrega.", example = "3e5a7c9b-1d3f-4a6c-8e0b-2d4f6a8c0e1b") @PathVariable String id, @RequestBody EntregaRequestDTO dto) {
        return EntregaResponseDTO.fromEntity(entregaService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir entrega", description = "Apaga o registro da encomenda.")
    @ApiResponse(responseCode = "200", description = "Excluída.")
    public ResponseEntity<Void> excluir(@Parameter(description = "Id da entrega.", example = "3e5a7c9b-1d3f-4a6c-8e0b-2d4f6a8c0e1b") @PathVariable String id) {
        entregaService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "Listar entregas", description = "Todas as encomendas do condomínio.")
    @ApiResponse(responseCode = "200", description = "Entregas.")
    public List<EntregaResponseDTO> listarTodas() {
        return entregaService.listarTodas(CondominioUtils.condominioIdEfetivo()).stream()
                .map(EntregaResponseDTO::fromEntity)
                .toList();
    }

    @GetMapping("/pendentes")
    @Operation(summary = "Entregas pendentes", description = "Encomendas esperando retirada.")
    @ApiResponse(responseCode = "200", description = "Entregas.")
    public List<EntregaResponseDTO> listarPendentes() {
        return entregaService.listarPendentes(CondominioUtils.condominioIdEfetivo()).stream()
                .map(EntregaResponseDTO::fromEntity)
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe da entrega", description = "Uma encomenda do condomínio de quem chama.")
    @ApiResponse(responseCode = "200", description = "Entrega.")
    public EntregaResponseDTO buscarPorId(@Parameter(description = "Id da entrega.", example = "3e5a7c9b-1d3f-4a6c-8e0b-2d4f6a8c0e1b") @PathVariable String id) {
        return EntregaResponseDTO.fromEntity(entregaService.buscarPorId(id));
    }

    @GetMapping("/morador/{moradorId}")
    @Operation(summary = "Entregas de um morador", description = "As encomendas do morador; `pendentesOnly` mostra só as não retiradas.")
    @ApiResponse(responseCode = "200", description = "Entregas.")
    public List<EntregaResponseDTO> listarPorMorador(@Parameter(description = "Id do morador.", example = "2c4e6a8b-0d1f-4e3a-8b5c-7d9e1f3a5b7c") @PathVariable UUID moradorId,
                                                     @RequestParam(required = false) boolean pendentesOnly) {
        if (pendentesOnly) {
            return entregaService.listarPendentesPorMorador(moradorId).stream()
                    .map(EntregaResponseDTO::fromEntity).toList();
        }
        return entregaService.listarPorMorador(moradorId).stream()
                .map(EntregaResponseDTO::fromEntity).toList();
    }

    @PostMapping("/{id}/retirar")
    @Operation(summary = "Registrar retirada", description = "Marca a encomenda como retirada. O corpo, opcional, é o nome de quem retirou, como texto.")
    @ApiResponse(responseCode = "200", description = "Retirada registrada.")
    public EntregaResponseDTO retirar(@Parameter(description = "Id da entrega.", example = "3e5a7c9b-1d3f-4a6c-8e0b-2d4f6a8c0e1b") @PathVariable String id, @RequestBody(required = false) String recebedor,
            @RequestParam(required = false)
            @org.springframework.format.annotation.DateTimeFormat(
                    iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
            java.time.LocalDate dataRetirada) {
        return EntregaResponseDTO.fromEntity(entregaService.retirar(id, recebedor, dataRetirada));
    }
}
