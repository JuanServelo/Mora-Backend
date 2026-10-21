package portaria.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import portaria.dto.ApartamentoRequestDTO;
import portaria.dto.ApartamentoResponseDTO;
import portaria.model.Apartamento;
import portaria.security.CondominioUtils;
import portaria.service.ApartamentoService;

import java.util.List;
import java.util.UUID;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/apartamentos")
@RequiredArgsConstructor
@Tag(name = "Apartamentos", description = "Unidades do condomínio, ligadas a um bloco (RF-06).")
public class ApartamentoController {

    private final ApartamentoService apartamentoService;

    @PostMapping("/cadastrar")
    @Operation(summary = "Cadastrar apartamento", description = "Cria a unidade num bloco do condomínio de quem chama. Número repetido no mesmo bloco é recusado.")
    @ApiResponse(responseCode = "201", description = "Apartamento criado.")
    public ResponseEntity<ApartamentoResponseDTO> cadastrar(@Valid @RequestBody ApartamentoRequestDTO request) {
        Apartamento salvo = apartamentoService.cadastrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApartamentoResponseDTO.fromEntity(salvo));
    }

    @GetMapping
    @Operation(summary = "Listar apartamentos ativos", description = "Unidades ativas do condomínio. O Admin Geral pode informar `condominioId`.")
    @ApiResponse(responseCode = "200", description = "Apartamentos.")
    public List<ApartamentoResponseDTO> listarAtivos(@RequestParam(required = false) String condominioId) {
        return apartamentoService.listarAtivos(CondominioUtils.resolverEscopo(condominioId)).stream()
                .map(ApartamentoResponseDTO::fromEntity)
                .toList();
    }

    @GetMapping("/todos")
    @Operation(summary = "Listar todos os apartamentos", description = "Ativos e desativados.")
    @ApiResponse(responseCode = "200", description = "Apartamentos.")
    public List<ApartamentoResponseDTO> listarTodos(@RequestParam(required = false) String condominioId) {
        return apartamentoService.listarTodos(CondominioUtils.resolverEscopo(condominioId)).stream()
                .map(ApartamentoResponseDTO::fromEntity)
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe do apartamento", description = "Uma unidade do condomínio de quem chama.")
    @ApiResponse(responseCode = "200", description = "Apartamento.")
    public ApartamentoResponseDTO buscarPorId(@Parameter(description = "Id do apartamento.", example = "e064d8a3-2835-4bdf-8e9f-06fa3a108e6d") @PathVariable UUID id) {
        return ApartamentoResponseDTO.fromEntity(apartamentoService.buscarPorId(id));
    }

    @GetMapping("/bloco/{blocoId}")
    @Operation(summary = "Apartamentos de um bloco", description = "Todas as unidades do bloco.")
    @ApiResponse(responseCode = "200", description = "Apartamentos.")
    public List<ApartamentoResponseDTO> listarPorBloco(@Parameter(description = "Id do bloco.", example = "5c27d9b1-3cf7-4cef-8128-ec4011d93434") @PathVariable UUID blocoId) {
        return apartamentoService.listarPorBloco(blocoId).stream()
                .map(ApartamentoResponseDTO::fromEntity)
                .toList();
    }

    @GetMapping("/bloco/{blocoId}/ativos")
    @Operation(summary = "Apartamentos ativos de um bloco", description = "Só as unidades ativas do bloco.")
    @ApiResponse(responseCode = "200", description = "Apartamentos.")
    public List<ApartamentoResponseDTO> listarPorBlocoAtivos(@Parameter(description = "Id do bloco.", example = "5c27d9b1-3cf7-4cef-8128-ec4011d93434") @PathVariable UUID blocoId) {
        return apartamentoService.listarPorBlocoAtivos(blocoId).stream()
                .map(ApartamentoResponseDTO::fromEntity)
                .toList();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar apartamento", description = "Substitui número, andar, bloco, quartos, área e observações.")
    @ApiResponse(responseCode = "200", description = "Apartamento atualizado.")
    public ApartamentoResponseDTO atualizar(
            @Parameter(description = "Id do apartamento.", example = "e064d8a3-2835-4bdf-8e9f-06fa3a108e6d") @PathVariable UUID id,
            @Valid @RequestBody ApartamentoRequestDTO request
    ) {
        return ApartamentoResponseDTO.fromEntity(apartamentoService.atualizar(id, request));
    }

    @PutMapping("/{id}/desativar")
    @Operation(summary = "Desativar apartamento", description = "Tira a unidade das listas de ativas, sem apagar.")
    @ApiResponse(responseCode = "200", description = "Desativado.")
    public ResponseEntity<Void> desativar(@Parameter(description = "Id do apartamento.", example = "e064d8a3-2835-4bdf-8e9f-06fa3a108e6d") @PathVariable UUID id) {
        apartamentoService.desativar(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/ativar")
    @Operation(summary = "Reativar apartamento", description = "Volta a unidade às listas de ativas.")
    @ApiResponse(responseCode = "200", description = "Reativado.")
    public ResponseEntity<Void> ativar(@Parameter(description = "Id do apartamento.", example = "e064d8a3-2835-4bdf-8e9f-06fa3a108e6d") @PathVariable UUID id) {
        apartamentoService.ativar(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir apartamento", description = "Apaga a unidade.")
    @ApiResponse(responseCode = "200", description = "Excluído.")
    public ResponseEntity<Void> deletar(@Parameter(description = "Id do apartamento.", example = "e064d8a3-2835-4bdf-8e9f-06fa3a108e6d") @PathVariable UUID id) {
        apartamentoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
