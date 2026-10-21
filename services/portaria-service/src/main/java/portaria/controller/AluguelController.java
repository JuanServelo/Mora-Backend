package portaria.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import portaria.dto.aluguel.*;
import portaria.exception.OperacaoInvalidaException;
import portaria.model.enums.StatusAluguel;
import portaria.security.AuthContext;
import portaria.security.CondominioUtils;
import portaria.security.JwtClaims;
import portaria.service.AluguelService;

import java.util.List;
import java.util.UUID;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/alugueis")
@RequiredArgsConstructor
@Tag(name = "Aluguel de vagas", description = "Morador disponibiliza a própria vaga e outro morador a aluga, com aprovação do dono.")
public class AluguelController {

    private final AluguelService aluguelService;

    @PostMapping("/disponibilizar")
    @Operation(summary = "Disponibilizar minha vaga", description = "Coloca a vaga da unidade para aluguel no período.")
    @ApiResponse(responseCode = "201", description = "Disponibilidade criada.")
    public ResponseEntity<DisponibilidadeVagaResponseDTO> disponibilizar(
            @Valid @RequestBody DisponibilidadeVagaRequestDTO dto) {
        var resultado = aluguelService.disponibilizar(dto, currentUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DisponibilidadeVagaResponseDTO.fromEntity(resultado));
    }

    @GetMapping("/disponiveis")
    @Operation(summary = "Vagas disponíveis para aluguel", description = "As vagas que outros moradores disponibilizaram.")
    @ApiResponse(responseCode = "200", description = "Disponibilidades.")
    public List<DisponibilidadeVagaResponseDTO> listarDisponiveis() {
        return aluguelService.listarDisponibilidadesAtivas(CondominioUtils.condominioIdEfetivo()).stream()
                .map(DisponibilidadeVagaResponseDTO::fromEntity)
                .toList();
    }

    @GetMapping("/minhas-disponibilidades")
    @Operation(summary = "Minhas vagas disponibilizadas", description = "O que quem chama colocou para aluguel.")
    @ApiResponse(responseCode = "200", description = "Disponibilidades.")
    public List<DisponibilidadeVagaResponseDTO> minhasDisponibilidades() {
        return aluguelService.listarMinhasDisponibilidades(currentUserId()).stream()
                .map(DisponibilidadeVagaResponseDTO::fromEntity)
                .toList();
    }

    @DeleteMapping("/disponibilidades/{id}")
    @Operation(summary = "Retirar vaga do aluguel", description = "Cancela a disponibilidade.")
    @ApiResponse(responseCode = "200", description = "Cancelada.")
    public ResponseEntity<Void> cancelarDisponibilidade(@Parameter(description = "Id do aluguel ou da disponibilidade.", example = "0b1c2d3e-4f5a-4b6c-7d8e-9f0a1b2c3d4e") @PathVariable String id) {
        aluguelService.cancelarDisponibilidade(id, currentUserId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/solicitar")
    @Operation(summary = "Solicitar aluguel", description = "Pede uma vaga disponível no período, por diária ou mensalidade.")
    @ApiResponse(responseCode = "201", description = "Solicitação criada.")
    public ResponseEntity<AluguelVagaResponseDTO> solicitar(
            @Valid @RequestBody AluguelVagaRequestDTO dto) {
        var aluguel = aluguelService.solicitar(dto, currentUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AluguelVagaResponseDTO.fromEntity(aluguel));
    }

    @PostMapping("/{id}/aprovar")
    @Operation(summary = "Aprovar aluguel", description = "O dono da vaga aprova a solicitação.")
    @ApiResponse(responseCode = "200", description = "Aluguel aprovado.")
    public AluguelVagaResponseDTO aprovar(@Parameter(description = "Id do aluguel ou da disponibilidade.", example = "0b1c2d3e-4f5a-4b6c-7d8e-9f0a1b2c3d4e") @PathVariable String id) {
        return AluguelVagaResponseDTO.fromEntity(aluguelService.aprovar(id, currentUserId()));
    }

    @PostMapping("/{id}/recusar")
    @Operation(summary = "Recusar aluguel", description = "O dono da vaga recusa, com justificativa.")
    @ApiResponse(responseCode = "200", description = "Aluguel recusado.")
    public AluguelVagaResponseDTO recusar(
            @Parameter(description = "Id do aluguel ou da disponibilidade.", example = "0b1c2d3e-4f5a-4b6c-7d8e-9f0a1b2c3d4e") @PathVariable String id,
            @Valid @RequestBody RecusaAluguelRequestDTO recusaDTO) {
        return AluguelVagaResponseDTO.fromEntity(
                aluguelService.recusar(id, currentUserId(), recusaDTO.justificativa()));
    }

    @PostMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar aluguel", description = "Cancela o aluguel; pode haver penalidade, conforme o prazo.")
    @ApiResponse(responseCode = "200", description = "Aluguel cancelado.")
    public AluguelVagaResponseDTO cancelar(@Parameter(description = "Id do aluguel ou da disponibilidade.", example = "0b1c2d3e-4f5a-4b6c-7d8e-9f0a1b2c3d4e") @PathVariable String id) {
        return AluguelVagaResponseDTO.fromEntity(aluguelService.cancelar(id, currentUserId()));
    }

    @GetMapping("/historico/solicitante")
    @Operation(summary = "Aluguéis que pedi", description = "Histórico como quem aluga, filtrável por status.")
    @ApiResponse(responseCode = "200", description = "Aluguéis.")
    public List<AluguelVagaResponseDTO> historicoComoSolicitante(
            @RequestParam(required = false) StatusAluguel status) {
        UUID id = currentUserId();
        var lista = (status != null)
                ? aluguelService.historicoPorSolicitanteEStatus(id, status)
                : aluguelService.historicoPorSolicitante(id);
        return lista.stream().map(AluguelVagaResponseDTO::fromEntity).toList();
    }

    @GetMapping("/historico/proprietario")
    @Operation(summary = "Aluguéis das minhas vagas", description = "Histórico como dono da vaga, filtrável por status.")
    @ApiResponse(responseCode = "200", description = "Aluguéis.")
    public List<AluguelVagaResponseDTO> historicoComoProprietario(
            @RequestParam(required = false) StatusAluguel status) {
        UUID id = currentUserId();
        var lista = (status != null)
                ? aluguelService.historicoPorProprietarioEStatus(id, status)
                : aluguelService.historicoPorProprietario(id);
        return lista.stream().map(AluguelVagaResponseDTO::fromEntity).toList();
    }

    private UUID currentUserId() {
        JwtClaims claims = AuthContext.get();
        if (claims == null || claims.authUserId() == null) {
            throw new OperacaoInvalidaException("Usuário não autenticado.");
        }
        try {
            return UUID.fromString(claims.authUserId());
        } catch (IllegalArgumentException e) {
            throw new OperacaoInvalidaException("ID de usuário inválido no token.");
        }
    }
}
