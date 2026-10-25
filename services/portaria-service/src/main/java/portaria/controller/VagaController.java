package portaria.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import portaria.dto.VagaResponseDTO;
import portaria.model.Vaga;
import portaria.security.CondominioUtils;
import portaria.service.VagaService;

import java.util.List;
import java.util.UUID;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/vagas")
@RequiredArgsConstructor
@Tag(name = "Vagas", description = "Vagas de garagem, ligadas ou não a uma unidade.")
public class VagaController {

    private final VagaService vagaService;

    @PostMapping("/cadastrar")
    @Operation(summary = "Cadastrar vaga", description = "Cria a vaga e, com `apartamentoId`, já a liga à unidade.")
    @ApiResponse(responseCode = "201", description = "Vaga criada.")
    public ResponseEntity<VagaResponseDTO> cadastrar(
            @Valid @RequestBody Vaga vaga,
            @RequestParam(required = false) UUID apartamentoId) {
        Vaga salvo = vagaService.cadastrar(vaga, apartamentoId);
        return ResponseEntity.status(HttpStatus.CREATED).body(VagaResponseDTO.fromEntity(salvo));
    }

    @GetMapping
    @Operation(summary = "Listar vagas ativas", description = "Vagas ativas do condomínio. O Admin Geral pode informar `condominioId`.")
    @ApiResponse(responseCode = "200", description = "Vagas.")
    public List<VagaResponseDTO> listarApenasAtivas(@RequestParam(required = false) String condominioId) {
        return vagaService.listarApenasAtivas(CondominioUtils.resolverEscopo(condominioId)).stream()
                .map(VagaResponseDTO::fromEntity)
                .toList();
    }

    @GetMapping("/todas")
    @Operation(summary = "Listar todas as vagas", description = "Ativas e desativadas.")
    @ApiResponse(responseCode = "200", description = "Vagas.")
    public List<VagaResponseDTO> listarTodas(@RequestParam(required = false) String condominioId) {
        return vagaService.listarTodas(CondominioUtils.resolverEscopo(condominioId)).stream()
                .map(VagaResponseDTO::fromEntity)
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe da vaga", description = "Uma vaga do condomínio de quem chama.")
    @ApiResponse(responseCode = "200", description = "Vaga.")
    public VagaResponseDTO buscarPorId(@Parameter(description = "Id da vaga.", example = "7a1c3e5f-9b2d-4f6a-8c0e-1d3f5a7b9c2e") @PathVariable String id) {
        return VagaResponseDTO.fromEntity(vagaService.buscarPorId(id));
    }

    @GetMapping("/apartamento/{apartamentoId}")
    @Operation(summary = "Vagas de um apartamento", description = "As vagas ligadas à unidade.")
    @ApiResponse(responseCode = "200", description = "Vagas.")
    public List<VagaResponseDTO> buscarPorApartamento(@Parameter(description = "Id do apartamento.", example = "e064d8a3-2835-4bdf-8e9f-06fa3a108e6d") @PathVariable UUID apartamentoId) {
        return vagaService.buscarPorApartamento(apartamentoId).stream()
                .map(VagaResponseDTO::fromEntity)
                .toList();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar vaga", description = "Substitui número, localização e tipo; `apartamentoId` troca a unidade dona.")
    @ApiResponse(responseCode = "200", description = "Vaga atualizada.")
    public VagaResponseDTO atualizar(
            @Parameter(description = "Id da vaga.", example = "7a1c3e5f-9b2d-4f6a-8c0e-1d3f5a7b9c2e") @PathVariable String id,
            @Valid @RequestBody Vaga dados,
            @RequestParam(required = false) String apartamentoId) {
        return VagaResponseDTO.fromEntity(vagaService.atualizar(id, dados, apartamentoId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desativar vaga", description = "Tira a vaga das listas de ativas, sem apagar.")
    @ApiResponse(responseCode = "200", description = "Desativada.")
    public ResponseEntity<Void> desativar(@Parameter(description = "Id da vaga.", example = "7a1c3e5f-9b2d-4f6a-8c0e-1d3f5a7b9c2e") @PathVariable String id) {
        vagaService.desativar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/ativar")
    @Operation(summary = "Reativar vaga", description = "Volta a vaga às listas de ativas.")
    @ApiResponse(responseCode = "200", description = "Reativada.")
    public ResponseEntity<Void> ativar(@Parameter(description = "Id da vaga.", example = "7a1c3e5f-9b2d-4f6a-8c0e-1d3f5a7b9c2e") @PathVariable String id) {
        vagaService.ativar(id);
        return ResponseEntity.noContent().build();
    }
}
