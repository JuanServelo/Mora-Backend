package portaria.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import portaria.model.Bloco;
import portaria.security.CondominioUtils;
import portaria.service.BlocoService;

import java.util.List;
import java.util.UUID;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/blocos")
@RequiredArgsConstructor
@Tag(name = "Blocos", description = "Blocos do condomínio (RF-06). O condomínio sai do token.")
public class BlocoController {

    private final BlocoService blocoService;

    @PostMapping("/cadastrar")
    @Operation(summary = "Cadastrar bloco", description = "Cria um bloco no condomínio de quem chama, com número de andares e apartamentos por andar.")
    @ApiResponse(responseCode = "201", description = "Bloco criado.")
    public ResponseEntity<Bloco> cadastrar(@Valid @RequestBody Bloco bloco) {
        return ResponseEntity.status(HttpStatus.CREATED).body(blocoService.cadastrar(bloco));
    }

    @GetMapping
    @Operation(summary = "Listar blocos ativos", description = "Blocos ativos do condomínio. O Admin Geral pode informar `condominioId`.")
    @ApiResponse(responseCode = "200", description = "Blocos.")
    public List<Bloco> listarAtivos(@RequestParam(required = false) String condominioId) {
        return blocoService.listarAtivos(CondominioUtils.resolverEscopo(condominioId));
    }

    @GetMapping("/todos")
    @Operation(summary = "Listar todos os blocos", description = "Ativos e desativados. O Admin Geral pode informar `condominioId`.")
    @ApiResponse(responseCode = "200", description = "Blocos.")
    public List<Bloco> listarTodos(@RequestParam(required = false) String condominioId) {
        return blocoService.listarTodos(CondominioUtils.resolverEscopo(condominioId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe do bloco", description = "Um bloco do condomínio de quem chama.")
    @ApiResponse(responseCode = "200", description = "Bloco.")
    public Bloco buscarPorId(@Parameter(description = "Id do bloco.", example = "5c27d9b1-3cf7-4cef-8128-ec4011d93434") @PathVariable UUID id) {
        return blocoService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar bloco", description = "Substitui nome, sigla, descrição, andares e apartamentos por andar.")
    @ApiResponse(responseCode = "200", description = "Bloco atualizado.")
    public Bloco atualizar(@Parameter(description = "Id do bloco.", example = "5c27d9b1-3cf7-4cef-8128-ec4011d93434") @PathVariable UUID id, @Valid @RequestBody Bloco dados) {
        return blocoService.atualizar(id, dados);
    }

    @PutMapping("/{id}/desativar")
    @Operation(summary = "Desativar bloco", description = "Tira o bloco das listas de ativos, sem apagar.")
    @ApiResponse(responseCode = "200", description = "Desativado.")
    public ResponseEntity<Void> desativar(@Parameter(description = "Id do bloco.", example = "5c27d9b1-3cf7-4cef-8128-ec4011d93434") @PathVariable UUID id) {
        blocoService.desativar(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/ativar")
    @Operation(summary = "Reativar bloco", description = "Volta o bloco às listas de ativos.")
    @ApiResponse(responseCode = "200", description = "Reativado.")
    public ResponseEntity<Void> ativar(@Parameter(description = "Id do bloco.", example = "5c27d9b1-3cf7-4cef-8128-ec4011d93434") @PathVariable UUID id) {
        blocoService.ativar(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir bloco", description = "Apaga o bloco. Recusado se ainda houver apartamentos nele.")
    @ApiResponse(responseCode = "200", description = "Excluído.")
    public ResponseEntity<Void> deletar(@Parameter(description = "Id do bloco.", example = "5c27d9b1-3cf7-4cef-8128-ec4011d93434") @PathVariable UUID id) {
        blocoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
