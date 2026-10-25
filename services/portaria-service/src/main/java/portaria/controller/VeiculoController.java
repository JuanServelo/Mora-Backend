package portaria.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import portaria.dto.AlterarVagaDTO;
import portaria.dto.CriarVeiculoDTO;
import portaria.dto.MovimentacaoVeiculoResponseDTO;
import portaria.dto.VeiculoResponseDTO;
import portaria.service.VeiculoService;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/veiculos")
@RequiredArgsConstructor
@Tag(name = "Veículos", description = "Veículos cadastrados e o controle de entrada e saída na garagem.")
public class VeiculoController {

    private final VeiculoService veiculoService;

    @PostMapping("/cadastrar")
    @Operation(summary = "Cadastrar veículo", description = "Cadastra o veículo de um morador, visitante ou funcionário, opcionalmente com a vaga.")
    @ApiResponse(responseCode = "201", description = "Veículo criado.")
    public ResponseEntity<VeiculoResponseDTO> cadastrar(@Valid @RequestBody CriarVeiculoDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(veiculoService.cadastrar(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar veículo", description = "Substitui os dados do veículo.")
    @ApiResponse(responseCode = "200", description = "Veículo atualizado.")
    public ResponseEntity<VeiculoResponseDTO> atualizar(
            @Parameter(description = "Id do veículo.", example = "1f3e5a7c-9b2d-4e6f-a8c0-2d4f6b8a0c1e") @PathVariable String id,
            @Valid @RequestBody CriarVeiculoDTO dto) {
        return ResponseEntity.ok(veiculoService.atualizar(id, dto));
    }

    @PatchMapping("/{id}/vaga")
    @Operation(summary = "Trocar vaga do veículo", description = "Liga o veículo a outra vaga.")
    @ApiResponse(responseCode = "200", description = "Vaga trocada.")
    public ResponseEntity<VeiculoResponseDTO> alterarVaga(
            @Parameter(description = "Id do veículo.", example = "1f3e5a7c-9b2d-4e6f-a8c0-2d4f6b8a0c1e") @PathVariable String id,
            @RequestBody AlterarVagaDTO dto) {
        return ResponseEntity.ok(veiculoService.alterarVaga(id, dto));
    }

    @PostMapping("/{id}/entrada")
    @Operation(summary = "Registrar entrada do veículo", description = "Marca o veículo como dentro, opcionalmente numa vaga.")
    @ApiResponse(responseCode = "200", description = "Entrada registrada.")
    public ResponseEntity<VeiculoResponseDTO> registrarEntrada(
            @Parameter(description = "Id do veículo.", example = "1f3e5a7c-9b2d-4e6f-a8c0-2d4f6b8a0c1e") @PathVariable String id,
            @RequestParam(required = false) String vagaId) {
        return ResponseEntity.ok(veiculoService.registrarEntrada(id, vagaId));
    }

    @PostMapping("/entrada/placa/{placa}")
    @Operation(summary = "Entrada pela placa", description = "Registra a entrada de um veículo cadastrado a partir da placa lida no portão.")
    @ApiResponse(responseCode = "200", description = "Entrada registrada.")
    public ResponseEntity<VeiculoResponseDTO> registrarEntradaPorPlaca(@Parameter(description = "Placa do veículo.", example = "BRA2E19") @PathVariable String placa) {
        return ResponseEntity.ok(veiculoService.registrarEntradaPorPlaca(placa));
    }

    @PostMapping("/{id}/saida")
    @Operation(summary = "Registrar saída do veículo", description = "Marca o veículo como fora.")
    @ApiResponse(responseCode = "200", description = "Saída registrada.")
    public ResponseEntity<VeiculoResponseDTO> registrarSaida(@Parameter(description = "Id do veículo.", example = "1f3e5a7c-9b2d-4e6f-a8c0-2d4f6b8a0c1e") @PathVariable String id) {
        return ResponseEntity.ok(veiculoService.registrarSaida(id));
    }

    @GetMapping
    @Operation(summary = "Listar veículos", description = "Veículos cadastrados no condomínio.")
    @ApiResponse(responseCode = "200", description = "Veículos.")
    public ResponseEntity<List<VeiculoResponseDTO>> listarTodos() {
        return ResponseEntity.ok(veiculoService.listarTodos());
    }

    @GetMapping("/dentro")
    @Operation(summary = "Veículos dentro", description = "Veículos cadastrados que estão na garagem agora.")
    @ApiResponse(responseCode = "200", description = "Veículos.")
    public ResponseEntity<List<VeiculoResponseDTO>> listarDentro() {
        return ResponseEntity.ok(veiculoService.listarDentro());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe do veículo", description = "Um veículo do condomínio de quem chama.")
    @ApiResponse(responseCode = "200", description = "Veículo.")
    public ResponseEntity<VeiculoResponseDTO> buscarPorId(@Parameter(description = "Id do veículo.", example = "1f3e5a7c-9b2d-4e6f-a8c0-2d4f6b8a0c1e") @PathVariable String id) {
        return ResponseEntity.ok(veiculoService.buscarPorId(id));
    }

    @GetMapping("/proprietario/{proprietarioId}")
    @Operation(summary = "Veículos de uma pessoa", description = "Os veículos cadastrados no nome da pessoa.")
    @ApiResponse(responseCode = "200", description = "Veículos.")
    public ResponseEntity<List<VeiculoResponseDTO>> listarPorProprietario(@Parameter(description = "Id do dono do veículo.", example = "4") @PathVariable String proprietarioId) {
        return ResponseEntity.ok(veiculoService.listarPorProprietario(proprietarioId));
    }

    // ─── Portaria: entrada avulsa (veículo não cadastrado) ───────────────────

    @PostMapping("/entrada/avulsa/{placa}")
    @Operation(summary = "Entrada avulsa pela placa", description = "Registra a entrada de um veículo sem cadastro, só pela placa.")
    @ApiResponse(responseCode = "200", description = "Entrada registrada.")
    public ResponseEntity<MovimentacaoVeiculoResponseDTO> registrarEntradaAvulsa(@Parameter(description = "Placa do veículo.", example = "BRA2E19") @PathVariable String placa) {
        return ResponseEntity.ok(veiculoService.registrarEntradaAvulsa(placa));
    }

    @PostMapping("/saida/placa/{placa}")
    @Operation(summary = "Saída pela placa", description = "Fecha a movimentação em aberto da placa.")
    @ApiResponse(responseCode = "200", description = "Saída registrada.")
    public ResponseEntity<MovimentacaoVeiculoResponseDTO> registrarSaidaPorPlaca(@Parameter(description = "Placa do veículo.", example = "BRA2E19") @PathVariable String placa) {
        return ResponseEntity.ok(veiculoService.registrarSaidaPorPlaca(placa));
    }

    @GetMapping("/dentro-portaria")
    @Operation(summary = "Movimentações em aberto", description = "Veículos, cadastrados ou avulsos, com entrada sem saída.")
    @ApiResponse(responseCode = "200", description = "Movimentações.")
    public ResponseEntity<List<MovimentacaoVeiculoResponseDTO>> listarDentroPortaria() {
        return ResponseEntity.ok(veiculoService.listarDentroPortaria());
    }

    @GetMapping("/historico-acesso")
    @Operation(summary = "Histórico de acesso de veículos", description = "Entradas e saídas, filtráveis por placa, período e status.")
    @ApiResponse(responseCode = "200", description = "Movimentações.")
    public ResponseEntity<List<MovimentacaoVeiculoResponseDTO>> listarHistoricoAcesso(
            @RequestParam(required = false) String placa,
            @RequestParam(required = false) String dataInicio,
            @RequestParam(required = false) String dataFim,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(veiculoService.listarHistoricoAcesso(placa, dataInicio, dataFim, status));
    }
}
