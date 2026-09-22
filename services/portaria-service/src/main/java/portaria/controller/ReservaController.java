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

@RestController
@RequestMapping("/reservas")
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaService reservaService;
    private final AuthApiClient authApiClient;

    @PostMapping
    public ResponseEntity<ReservaResponseDTO> solicitar(@RequestBody ReservaRequestDTO request) {
        var reserva = reservaService.solicitar(claims(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ReservaResponseDTO.fromEntity(reserva));
    }

    /** Agenda do espaço no período exibido — inclui reservas passadas (RN-03). */
    @GetMapping("/agenda")
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
    public List<PessoaVinculadaDTO> moradoresDaUnidade(@RequestParam String unidadeId) {
        return authApiClient.pessoasDaUnidade(unidadeId).stream()
                .map(p -> new PessoaVinculadaDTO(p.id(), p.nome()))
                .toList();
    }

    /** Funcionários elegíveis como responsáveis por evento do condomínio. */
    @GetMapping("/opcoes/funcionarios")
    public List<PessoaVinculadaDTO> funcionarios() {
        return authApiClient.funcionarios().stream()
                .map(p -> new PessoaVinculadaDTO(p.id(), p.nome()))
                .toList();
    }

    @GetMapping
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
    public List<ReservaResponseDTO> minhasReservas() {
        return reservaService.listarDoSolicitante(claims().authUserId())
                .stream().map(ReservaResponseDTO::fromEntity).toList();
    }

    @GetMapping("/area/{areaComumId}")
    public List<ReservaResponseDTO> listarPorArea(@PathVariable String areaComumId) {
        return reservaService.listarPorArea(areaComumId)
                .stream().map(ReservaResponseDTO::fromEntity).toList();
    }

    @GetMapping("/{id}")
    public ReservaResponseDTO buscarPorId(@PathVariable String id) {
        return ReservaResponseDTO.fromEntity(reservaService.buscarPorId(id));
    }

    /** Fila do síndico: pendentes ordenadas pela proximidade do prazo (RN-08). */
    @GetMapping("/pendentes")
    public List<ReservaResponseDTO> pendentes() {
        return reservaService.listarPendentes()
                .stream().map(ReservaResponseDTO::fromEntity).toList();
    }

    /** Aprova várias; as que conflitarem voltam com o motivo (RN-06). */
    @PostMapping("/aprovar-lote")
    public ReservaService.ResultadoLote aprovarLote(@RequestBody List<String> ids) {
        return reservaService.aprovarEmLote(ids, claims());
    }

    /** Trilha somente leitura das decisões sobre a reserva (RN-10). */
    @GetMapping("/{id}/auditoria")
    public List<portaria.model.ReservaAuditoria> auditoria(@PathVariable String id) {
        return reservaService.auditoriaDe(id);
    }

    @PatchMapping("/{id}/aprovar")
    public ReservaResponseDTO aprovar(@PathVariable String id) {
        return ReservaResponseDTO.fromEntity(reservaService.aprovar(id, claims()));
    }

    @PatchMapping("/{id}/recusar")
    public ReservaResponseDTO recusar(@PathVariable String id,
                                      @RequestBody RecusaReservaRequestDTO request) {
        return ReservaResponseDTO.fromEntity(
                reservaService.recusar(id, request.justificativa(), claims()));
    }

    @PatchMapping("/{id}/cancelar")
    public ReservaResponseDTO cancelar(@PathVariable String id) {
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
