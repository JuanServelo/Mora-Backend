package portaria.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import portaria.dto.PreAutorizacaoRequestDTO;
import portaria.dto.PreAutorizacaoResponseDTO;
import portaria.dto.atendimento.AtendimentoResponseDTO;
import portaria.exception.OperacaoInvalidaException;
import portaria.security.AuthContext;
import portaria.security.CondominioUtils;
import portaria.security.JwtClaims;
import portaria.service.PreAutorizacaoService;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/pre-autorizacoes")
@RequiredArgsConstructor
@Tag(name = "Pré-autorizações", description = "Visitas e serviços liberados com antecedência pelo morador, para a portaria (RN-08).")
public class PreAutorizacaoController {

    private final PreAutorizacaoService preAutorizacaoService;

    /**
     * Morador pré-libera visitante (unidade obrigatória); síndico e porteiro
     * pré-liberam terceiro, e aí não há unidade — o responsável é quem criou.
     */
    @PostMapping
    @Operation(summary = "Pré-autorizar visita", description = "O morador pré-libera um visitante ou prestador para a própria unidade, com validade; síndico e porteiro também podem.")
    @ApiResponse(responseCode = "201", description = "Pré-autorização criada.")
    public ResponseEntity<PreAutorizacaoResponseDTO> cadastrar(@RequestBody PreAutorizacaoRequestDTO request) {
        JwtClaims claims = AuthContext.get();
        boolean operador = claims != null && List.of("PORTEIRO", "ADMIN_SINDICO", "ADMIN_GERAL")
                .contains(claims.perfil());
        String unidadeId = operador
                ? (claims.unidadeId() != null && !claims.unidadeId().isBlank() ? claims.unidadeId() : null)
                : CondominioUtils.unidadeIdObrigatoria();

        var pa = preAutorizacaoService.cadastrar(currentUserId(), unidadeId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(PreAutorizacaoResponseDTO.fromEntity(pa));
    }

    @GetMapping("/hoje")
    @Operation(summary = "Pré-autorizações de hoje", description = "As válidas hoje, ainda não utilizadas: a lista da guarita.")
    @ApiResponse(responseCode = "200", description = "Pré-autorizações.")
    public List<PreAutorizacaoResponseDTO> listarAtivasHoje() {
        String condominioId = CondominioUtils.condominioIdEfetivo();
        return preAutorizacaoService.listarAtivasHoje(condominioId)
                .stream().map(PreAutorizacaoResponseDTO::fromEntity).toList();
    }

    @GetMapping("/buscar")
    @Operation(summary = "Buscar pré-autorização", description = "Por nome, CPF ou placa (`termo`).")
    @ApiResponse(responseCode = "200", description = "Pré-autorizações.")
    public List<PreAutorizacaoResponseDTO> buscar(@RequestParam String termo) {
        String condominioId = CondominioUtils.condominioIdEfetivo();
        return preAutorizacaoService.buscarPorNomeOuCpf(condominioId, termo)
                .stream().map(PreAutorizacaoResponseDTO::fromEntity).toList();
    }

    /** Atendimento: autorização ativa para o CPF identificado (RN-08). */
    @GetMapping("/por-cpf")
    @Operation(summary = "Pré-autorização pelo CPF", description = "Autorização ativa para o CPF identificado na guarita.")
    @ApiResponse(responseCode = "200", description = "Pré-autorizações.")
    public List<PreAutorizacaoResponseDTO> porCpf(@RequestParam String cpf) {
        String condominioId = CondominioUtils.condominioIdEfetivo();
        return preAutorizacaoService.buscarPorCpf(condominioId, cpf)
                .stream().map(PreAutorizacaoResponseDTO::fromEntity).toList();
    }

    /** Atendimento: autorização ativa para a placa lida no portão (RN-08). */
    @GetMapping("/por-placa")
    @Operation(summary = "Pré-autorização pela placa", description = "Autorização ativa para a placa lida no portão.")
    @ApiResponse(responseCode = "200", description = "Pré-autorizações.")
    public List<PreAutorizacaoResponseDTO> porPlaca(@RequestParam String placa) {
        String condominioId = CondominioUtils.condominioIdEfetivo();
        return preAutorizacaoService.buscarPorPlaca(condominioId, placa)
                .stream().map(PreAutorizacaoResponseDTO::fromEntity).toList();
    }

    /**
     * Registra a entrada do visitante pré-liberado — com o veículo dele, quando
     * houver. Tudo em uma transação: a pré-liberação só vira UTILIZADA se a
     * entrada de fato foi registrada.
     */
    @PostMapping("/{id}/registrar-entrada")
    @Operation(summary = "Entrada do pré-autorizado", description = "Registra a entrada do visitante pré-liberado, com o veículo dele quando `incluirVeiculo`.")
    @ApiResponse(responseCode = "200", description = "Entrada registrada.")
    public AtendimentoResponseDTO registrarEntrada(
            @Parameter(description = "Id da pré-autorização.", example = "a7b8c9d0-e1f2-4a3b-8c4d-5e6f7a8b9c0d") @PathVariable String id,
            @RequestParam(defaultValue = "true") boolean incluirVeiculo,
            @RequestParam(required = false) String vagaId) {
        return preAutorizacaoService.registrarEntrada(id, incluirVeiculo, vagaId);
    }

    /** Consumida na entrada; não pode ser reaproveitada depois. */
    @PostMapping("/{id}/utilizar")
    @Operation(summary = "Marcar como utilizada", description = "Consome a pré-autorização: ela não pode ser reaproveitada.")
    @ApiResponse(responseCode = "200", description = "Utilizada.")
    public PreAutorizacaoResponseDTO utilizar(@Parameter(description = "Id da pré-autorização.", example = "a7b8c9d0-e1f2-4a3b-8c4d-5e6f7a8b9c0d") @PathVariable String id) {
        return PreAutorizacaoResponseDTO.fromEntity(preAutorizacaoService.marcarUtilizada(id));
    }

    /**
     * O morador enxerga o que a própria unidade pré-liberou; o síndico, que não
     * tem unidade, enxerga as que ele mesmo criou.
     */
    @GetMapping("/minhas")
    @Operation(summary = "Minhas pré-autorizações", description = "O que a unidade de quem chama pré-liberou.")
    @ApiResponse(responseCode = "200", description = "Pré-autorizações.")
    public List<PreAutorizacaoResponseDTO> minhas() {
        JwtClaims claims = AuthContext.get();
        String unidadeId = claims != null ? claims.unidadeId() : null;
        var lista = (unidadeId != null && !unidadeId.isBlank())
                ? preAutorizacaoService.listarDaUnidade(unidadeId)
                : preAutorizacaoService.listarDoMorador(currentUserId());
        return lista.stream().map(PreAutorizacaoResponseDTO::fromEntity).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe da pré-autorização", description = "Uma pré-autorização do condomínio de quem chama.")
    @ApiResponse(responseCode = "200", description = "Pré-autorização.")
    public PreAutorizacaoResponseDTO buscarPorId(@Parameter(description = "Id da pré-autorização.", example = "a7b8c9d0-e1f2-4a3b-8c4d-5e6f7a8b9c0d") @PathVariable String id) {
        return PreAutorizacaoResponseDTO.fromEntity(preAutorizacaoService.buscarPorId(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cancelar pré-autorização", description = "Cancela uma pré-autorização ainda não utilizada.")
    @ApiResponse(responseCode = "200", description = "Cancelada.")
    public void cancelar(@Parameter(description = "Id da pré-autorização.", example = "a7b8c9d0-e1f2-4a3b-8c4d-5e6f7a8b9c0d") @PathVariable String id) {
        JwtClaims claims = AuthContext.get();
        String unidadeId = claims != null ? claims.unidadeId() : null;
        if (unidadeId != null && !unidadeId.isBlank()) {
            preAutorizacaoService.cancelar(id, unidadeId);
        } else {
            // Sem unidade (síndico): cancela o que criou.
            preAutorizacaoService.cancelarComoAutor(id, currentUserId());
        }
    }

    /** Id do usuário no auth-api: inteiro em texto, nunca UUID. */
    private String currentUserId() {
        JwtClaims claims = AuthContext.get();
        if (claims == null || claims.authUserId() == null) {
            throw new OperacaoInvalidaException("Usuário não autenticado");
        }
        return claims.authUserId();
    }
}
