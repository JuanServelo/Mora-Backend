package portaria.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import portaria.dto.atendimento.AtendimentoRegistroDTO;
import portaria.dto.atendimento.AtendimentoResponseDTO;
import portaria.dto.atendimento.VagaStatusDTO;
import portaria.dto.atendimento.VisitanteResumoDTO;
import portaria.model.enums.TipoVisita;
import portaria.service.AtendimentoService;

import java.util.List;
import java.util.UUID;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/atendimento")
@RequiredArgsConstructor
@Tag(name = "Atendimento", description = "O fluxo da guarita: identificar a pessoa, registrar a entrada com o veículo e a saída.")
public class AtendimentoController {

    private final AtendimentoService atendimentoService;

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe do atendimento", description = "A pessoa atendida, com a situação de acesso e o veículo dentro.")
    @ApiResponse(responseCode = "200", description = "Atendimento.")
    public ResponseEntity<VisitanteResumoDTO> buscarPorId(@Parameter(description = "Id da pessoa atendida.", example = "b1c2d3e4-f5a6-4b7c-8d9e-0f1a2b3c4d5e") @PathVariable String id) {
        return ResponseEntity.ok(atendimentoService.buscarPorId(id));
    }

    /**
     * Busca visitantes/terceiros por nome ou documento.
     * @param q    termo de busca (parcial, case-insensitive)
     * @param tipo VISITA ou SERVICO
     */
    @GetMapping("/buscar")
    @Operation(summary = "Buscar visitante ou prestador", description = "Por nome ou documento (`q`), no tipo informado (VISITA ou SERVICO).")
    @ApiResponse(responseCode = "200", description = "Pessoas encontradas.")
    public ResponseEntity<List<VisitanteResumoDTO>> buscar(
            @RequestParam String q,
            @RequestParam TipoVisita tipo) {
        return ResponseEntity.ok(atendimentoService.buscar(q, tipo));
    }

    /**
     * Retorna as vagas de um apartamento com status de ocupação.
     * Usado para popular o seletor de vaga no fluxo de Visitante.
     */
    @GetMapping("/vagas-unidade/{apartamentoId}")
    @Operation(summary = "Vagas da unidade", description = "As vagas do apartamento com a ocupação de agora, para escolher onde o visitante estaciona.")
    @ApiResponse(responseCode = "200", description = "Vagas.")
    public ResponseEntity<List<VagaStatusDTO>> vagasUnidade(
            @Parameter(description = "Id do apartamento.", example = "e064d8a3-2835-4bdf-8e9f-06fa3a108e6d") @PathVariable UUID apartamentoId) {
        return ResponseEntity.ok(atendimentoService.vagasUnidade(apartamentoId));
    }

    /**
     * Registra o atendimento completo: pessoa + veículo (opcional) + entrada.
     * Operação atômica: falha em qualquer etapa reverte tudo.
     */
    @PostMapping("/registrar")
    @Operation(summary = "Registrar atendimento", description = "Pessoa, veículo (opcional) e entrada numa operação só: se uma etapa falha, nada é gravado.")
    @ApiResponse(responseCode = "201", description = "Atendimento registrado.")
    public ResponseEntity<AtendimentoResponseDTO> registrar(
            @Valid @RequestBody AtendimentoRegistroDTO dto) {
        return ResponseEntity.ok(atendimentoService.registrar(dto));
    }

    /**
     * Retorna visitantes/terceiros atualmente dentro do condomínio (status=DENTRO).
     */
    @GetMapping("/dentro")
    @Operation(summary = "Quem está dentro", description = "Visitantes e prestadores com entrada sem saída.")
    @ApiResponse(responseCode = "200", description = "Pessoas dentro.")
    public ResponseEntity<List<VisitanteResumoDTO>> dentro() {
        return ResponseEntity.ok(atendimentoService.dentro());
    }

    /**
     * Histórico de visitantes/terceiros com filtros opcionais.
     */
    @GetMapping("/historico")
    @Operation(summary = "Histórico de atendimentos", description = "Filtrável por nome, tipo e status.")
    @ApiResponse(responseCode = "200", description = "Atendimentos.")
    public ResponseEntity<List<VisitanteResumoDTO>> historico(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String tipoVisita,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(atendimentoService.historico(nome, tipoVisita, status));
    }

    /**
     * Registra a saída de um visitante/terceiro.
     */
    @PostMapping("/{id}/saida")
    @Operation(summary = "Registrar saída", description = "Registra a saída da pessoa e do veículo dela.")
    @ApiResponse(responseCode = "200", description = "Saída registrada.")
    public ResponseEntity<VisitanteResumoDTO> registrarSaida(@Parameter(description = "Id da pessoa atendida.", example = "b1c2d3e4-f5a6-4b7c-8d9e-0f1a2b3c4d5e") @PathVariable String id) {
        return ResponseEntity.ok(atendimentoService.registrarSaida(id));
    }
}
