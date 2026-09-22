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

@RestController
@RequestMapping("/pre-autorizacoes")
@RequiredArgsConstructor
public class PreAutorizacaoController {

    private final PreAutorizacaoService preAutorizacaoService;

    /**
     * Morador pré-libera visitante (unidade obrigatória); síndico e porteiro
     * pré-liberam terceiro, e aí não há unidade — o responsável é quem criou.
     */
    @PostMapping
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
    public List<PreAutorizacaoResponseDTO> listarAtivasHoje() {
        String condominioId = CondominioUtils.condominioIdEfetivo();
        return preAutorizacaoService.listarAtivasHoje(condominioId)
                .stream().map(PreAutorizacaoResponseDTO::fromEntity).toList();
    }

    @GetMapping("/buscar")
    public List<PreAutorizacaoResponseDTO> buscar(@RequestParam String termo) {
        String condominioId = CondominioUtils.condominioIdEfetivo();
        return preAutorizacaoService.buscarPorNomeOuCpf(condominioId, termo)
                .stream().map(PreAutorizacaoResponseDTO::fromEntity).toList();
    }

    /** Atendimento: autorização ativa para o CPF identificado (RN-08). */
    @GetMapping("/por-cpf")
    public List<PreAutorizacaoResponseDTO> porCpf(@RequestParam String cpf) {
        String condominioId = CondominioUtils.condominioIdEfetivo();
        return preAutorizacaoService.buscarPorCpf(condominioId, cpf)
                .stream().map(PreAutorizacaoResponseDTO::fromEntity).toList();
    }

    /** Atendimento: autorização ativa para a placa lida no portão (RN-08). */
    @GetMapping("/por-placa")
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
    public AtendimentoResponseDTO registrarEntrada(
            @PathVariable String id,
            @RequestParam(defaultValue = "true") boolean incluirVeiculo,
            @RequestParam(required = false) String vagaId) {
        return preAutorizacaoService.registrarEntrada(id, incluirVeiculo, vagaId);
    }

    /** Consumida na entrada; não pode ser reaproveitada depois. */
    @PostMapping("/{id}/utilizar")
    public PreAutorizacaoResponseDTO utilizar(@PathVariable String id) {
        return PreAutorizacaoResponseDTO.fromEntity(preAutorizacaoService.marcarUtilizada(id));
    }

    /**
     * O morador enxerga o que a própria unidade pré-liberou; o síndico, que não
     * tem unidade, enxerga as que ele mesmo criou.
     */
    @GetMapping("/minhas")
    public List<PreAutorizacaoResponseDTO> minhas() {
        JwtClaims claims = AuthContext.get();
        String unidadeId = claims != null ? claims.unidadeId() : null;
        var lista = (unidadeId != null && !unidadeId.isBlank())
                ? preAutorizacaoService.listarDaUnidade(unidadeId)
                : preAutorizacaoService.listarDoMorador(currentUserId());
        return lista.stream().map(PreAutorizacaoResponseDTO::fromEntity).toList();
    }

    @GetMapping("/{id}")
    public PreAutorizacaoResponseDTO buscarPorId(@PathVariable String id) {
        return PreAutorizacaoResponseDTO.fromEntity(preAutorizacaoService.buscarPorId(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelar(@PathVariable String id) {
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
