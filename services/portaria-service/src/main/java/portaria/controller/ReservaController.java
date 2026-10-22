package portaria.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import portaria.client.AuthApiClient;
import portaria.dto.meuveiculo.PessoaVinculadaDTO;
import portaria.dto.reserva.RecusaReservaRequestDTO;
import portaria.dto.reserva.ReservaRequestDTO;
import portaria.dto.reserva.ReservaResponseDTO;
import portaria.exception.OperacaoInvalidaException;
import portaria.model.enums.StatusReserva;
import portaria.security.AuthContext;
import portaria.security.CondominioUtils;
import portaria.security.JwtClaims;
import portaria.service.ReservaService;

import java.time.LocalDateTime;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/reservas")
@RequiredArgsConstructor
@Tag(name = "Reservas", description = "Reserva de áreas comuns: solicitação, agenda, aprovação pelo síndico e auditoria (RF-10).")
public class ReservaController {

    private final ReservaService reservaService;
    private final AuthApiClient authApiClient;

    @PostMapping
    @Operation(summary = "Solicitar reserva", description = "Pede o espaço num horário. Se o espaço exige aprovação, nasce PENDENTE; conflito de horário é recusado.")
    @ApiResponse(responseCode = "201", description = "Reserva criada.")
    public ResponseEntity<ReservaResponseDTO> solicitar(@RequestBody ReservaRequestDTO request) {
        var reserva = reservaService.solicitar(claims(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ReservaResponseDTO.fromEntity(reserva));
    }

    /** Agenda do espaço no período exibido — inclui reservas passadas (RN-03). */
    @GetMapping("/agenda")
    @Operation(summary = "Agenda do espaço", description = "Reservas do espaço no período, inclusive as passadas (RN-03).")
    @ApiResponse(responseCode = "200", description = "Reservas.")
    public List<ReservaResponseDTO> agenda(
            @RequestParam String areaComumId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim
    ) {
        return reservaService.agenda(areaComumId, inicio, fim)
                .stream().map(ReservaResponseDTO::fromEntity).toList();
    }

    /** Moradores ativos de uma unidade — popula o seletor do porteiro (RN-04). */
    @GetMapping("/opcoes/moradores")
    @Operation(summary = "Moradores da unidade", description = "Moradores ativos da unidade, para o porteiro escolher o responsável (RN-04).")
    @ApiResponse(responseCode = "200", description = "Pessoas.")
    public List<PessoaVinculadaDTO> moradoresDaUnidade(@RequestParam String unidadeId) {
        return authApiClient.pessoasDaUnidade(unidadeId).stream()
                .map(p -> new PessoaVinculadaDTO(p.id(), p.nome()))
                .toList();
    }

    /** Funcionários elegíveis como responsáveis por evento do condomínio. */
    @GetMapping("/opcoes/funcionarios")
    @Operation(summary = "Funcionários elegíveis", description = "Funcionários que podem ser responsáveis por um evento do condomínio.")
    @ApiResponse(responseCode = "200", description = "Pessoas.")
    public List<PessoaVinculadaDTO> funcionarios() {
        return authApiClient.funcionarios().stream()
                .map(p -> new PessoaVinculadaDTO(p.id(), p.nome()))
                .toList();
    }

    @GetMapping
    @Operation(summary = "Listar reservas", description = "Reservas do condomínio, filtráveis por status.")
    @ApiResponse(responseCode = "200", description = "Reservas.")
    public List<ReservaResponseDTO> listar(@RequestParam(required = false) StatusReserva status) {
        String condominioId = CondominioUtils.condominioIdEfetivo();
        if (status != null) {
            return reservaService.listarPorCondominioEStatus(condominioId, status)
                    .stream().map(ReservaResponseDTO::fromEntity).toList();
        }
        return reservaService.listarPorCondominio(condominioId)
                .stream().map(ReservaResponseDTO::fromEntity).toList();
    }

    @GetMapping("/minhas")
    @Operation(summary = "Minhas reservas", description = "As reservas que quem chama solicitou.")
    @ApiResponse(responseCode = "200", description = "Reservas.")
    public List<ReservaResponseDTO> minhasReservas() {
        return reservaService.listarDoSolicitante(claims().authUserId())
                .stream().map(ReservaResponseDTO::fromEntity).toList();
    }

    @GetMapping("/area/{areaComumId}")
    @Operation(summary = "Reservas de um espaço", description = "Todas as reservas do espaço.")
    @ApiResponse(responseCode = "200", description = "Reservas.")
    public List<ReservaResponseDTO> listarPorArea(@Parameter(description = "Id da área comum.", example = "cf9f4897-b196-4186-83af-2d67678651c2") @PathVariable String areaComumId) {
        return reservaService.listarPorArea(areaComumId)
                .stream().map(ReservaResponseDTO::fromEntity).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe da reserva", description = "Uma reserva do condomínio de quem chama.")
    @ApiResponse(responseCode = "200", description = "Reserva.")
    public ReservaResponseDTO buscarPorId(@Parameter(description = "Id da reserva.", example = "51898a22-fb54-49d4-be61-55df90480a30") @PathVariable String id) {
        return ReservaResponseDTO.fromEntity(reservaService.buscarPorId(id));
    }

    /** Fila do síndico: pendentes ordenadas pela proximidade do prazo (RN-08). */
    @GetMapping("/pendentes")
    @Operation(summary = "Fila de aprovação", description = "Reservas pendentes, da que vence primeiro para a última (RN-08). Para o síndico.")
    @ApiResponse(responseCode = "200", description = "Reservas pendentes.")
    public List<ReservaResponseDTO> pendentes() {
        return reservaService.listarPendentes()
                .stream().map(ReservaResponseDTO::fromEntity).toList();
    }

    /** Aprova várias; as que conflitarem voltam com o motivo (RN-06). */
    @PostMapping("/aprovar-lote")
    @Operation(summary = "Aprovar várias reservas", description = "Aprova a lista de ids; as que conflitarem voltam em `falhas`, com o motivo (RN-06).")
    @ApiResponse(responseCode = "200", description = "Resultado do lote.")
    public ReservaService.ResultadoLote aprovarLote(@RequestBody List<String> ids) {
        return reservaService.aprovarEmLote(ids, claims());
    }

    /** Trilha somente leitura das decisões sobre a reserva (RN-10). */
    @GetMapping("/{id}/auditoria")
    @Operation(summary = "Auditoria da reserva", description = "Trilha somente leitura das decisões sobre a reserva (RN-10).")
    @ApiResponse(responseCode = "200", description = "Registros.")
    public List<portaria.model.ReservaAuditoria> auditoria(@Parameter(description = "Id da reserva.", example = "51898a22-fb54-49d4-be61-55df90480a30") @PathVariable String id) {
        return reservaService.auditoriaDe(id);
    }

    @PatchMapping("/{id}/aprovar")
    @Operation(summary = "Aprovar reserva", description = "Aprova uma reserva pendente.")
    @ApiResponse(responseCode = "200", description = "Reserva aprovada.")
    public ReservaResponseDTO aprovar(@Parameter(description = "Id da reserva.", example = "51898a22-fb54-49d4-be61-55df90480a30") @PathVariable String id) {
        return ReservaResponseDTO.fromEntity(reservaService.aprovar(id, claims()));
    }

    @PatchMapping("/{id}/recusar")
    @Operation(summary = "Recusar reserva", description = "Recusa uma reserva pendente, com justificativa.")
    @ApiResponse(responseCode = "200", description = "Reserva recusada.")
    public ReservaResponseDTO recusar(@Parameter(description = "Id da reserva.", example = "51898a22-fb54-49d4-be61-55df90480a30") @PathVariable String id,
                                      @RequestBody RecusaReservaRequestDTO request) {
        return ReservaResponseDTO.fromEntity(
                reservaService.recusar(id, request.justificativa(), claims()));
    }

    @PatchMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar reserva", description = "Cancela a reserva, pela regra de cancelamento do espaço.")
    @ApiResponse(responseCode = "200", description = "Reserva cancelada.")
    public ReservaResponseDTO cancelar(@Parameter(description = "Id da reserva.", example = "51898a22-fb54-49d4-be61-55df90480a30") @PathVariable String id) {
        return ReservaResponseDTO.fromEntity(reservaService.cancelar(id, claims()));
    }

    private JwtClaims claims() {
        JwtClaims c = AuthContext.get();
        if (c == null || c.authUserId() == null) {
            throw new OperacaoInvalidaException("Usuário não autenticado");
        }
        return c;
    }
}
