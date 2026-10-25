package portaria.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import portaria.dto.chave.*;
import portaria.security.CondominioUtils;
import portaria.service.ChaveService;

import java.time.LocalDate;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/chaves")
@RequiredArgsConstructor
@Tag(name = "Chaves", description = "Chaves dos espaços sob guarda da portaria: retirada, devolução e histórico.")
public class ChaveController {

    private final ChaveService chaveService;

    @GetMapping("/locais")
    @Operation(summary = "Locais com chave", description = "Blocos e áreas comuns a que uma chave pode pertencer.")
    @ApiResponse(responseCode = "200", description = "Locais.")
    public List<LocalChaveDTO> listarLocais() {
        return chaveService.listarLocaisElegiveis(CondominioUtils.condominioIdEfetivo());
    }

    @GetMapping
    @Operation(summary = "Listar chaves", description = "As chaves do condomínio, com quem está cada uma.")
    @ApiResponse(responseCode = "200", description = "Chaves.")
    public List<ChaveResponseDTO> listarTodas() {
        return chaveService.listarTodas(CondominioUtils.condominioIdEfetivo());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe da chave", description = "Uma chave do condomínio de quem chama.")
    @ApiResponse(responseCode = "200", description = "Chave.")
    public ChaveResponseDTO buscarPorId(@Parameter(description = "Id da chave.", example = "e8f9a0b1-c2d3-4e4f-a5b6-c7d8e9f0a1b2") @PathVariable String id) {
        return chaveService.buscarPorId(id);
    }

    @PostMapping("/cadastrar")
    @Operation(summary = "Cadastrar chave", description = "Cria uma chave de um bloco ou área comum.")
    @ApiResponse(responseCode = "201", description = "Chave criada.")
    public ResponseEntity<ChaveResponseDTO> cadastrar(@Valid @RequestBody CadastrarChaveDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chaveService.cadastrar(dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir chave", description = "Apaga a chave.")
    @ApiResponse(responseCode = "200", description = "Excluída.")
    public ResponseEntity<Void> deletar(@Parameter(description = "Id da chave.", example = "e8f9a0b1-c2d3-4e4f-a5b6-c7d8e9f0a1b2") @PathVariable String id) {
        chaveService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/retirar")
    @Operation(summary = "Registrar retirada", description = "Entrega a chave a um morador, funcionário ou terceiro. Chave já retirada é recusada.")
    @ApiResponse(responseCode = "200", description = "Retirada registrada.")
    public ChaveResponseDTO retirar(
        @Parameter(description = "Id da chave.", example = "e8f9a0b1-c2d3-4e4f-a5b6-c7d8e9f0a1b2") @PathVariable String id,
        @Valid @RequestBody RetirarChaveRequest request
    ) {
        return chaveService.retirar(id, request);
    }

    @PostMapping("/{id}/devolver")
    @Operation(summary = "Registrar devolução", description = "A chave volta à portaria.")
    @ApiResponse(responseCode = "200", description = "Devolução registrada.")
    public ChaveResponseDTO devolver(@Parameter(description = "Id da chave.", example = "e8f9a0b1-c2d3-4e4f-a5b6-c7d8e9f0a1b2") @PathVariable String id) {
        return chaveService.devolver(id);
    }

    @GetMapping("/{id}/historico")
    @Operation(summary = "Histórico da chave", description = "Retiradas e devoluções, filtráveis por período, quem retirou, perfil e status.")
    @ApiResponse(responseCode = "200", description = "Movimentações.")
    public List<MovimentacaoChaveResponseDTO> historico(
        @Parameter(description = "Id da chave.", example = "e8f9a0b1-c2d3-4e4f-a5b6-c7d8e9f0a1b2") @PathVariable String id,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
        @RequestParam(required = false) String quemRetirou,
        @RequestParam(required = false) String perfil,
        @RequestParam(required = false) String status
    ) {
        return chaveService.buscarHistorico(id, dataInicio, dataFim, quemRetirou, perfil, status);
    }
}
