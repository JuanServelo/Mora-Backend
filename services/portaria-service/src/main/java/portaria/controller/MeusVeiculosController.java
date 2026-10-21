package portaria.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import portaria.dto.meuveiculo.*;
import portaria.exception.OperacaoInvalidaException;
import portaria.security.AuthContext;
import portaria.security.CondominioUtils;
import portaria.security.JwtClaims;
import portaria.service.MeusVeiculosService;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Tela do morador: ele gerencia apenas os veículos e vagas da própria unidade.
 *
 * A unidade vem sempre do JWT (unidadeIdObrigatoria), nunca de parâmetro —
 * não há como pedir os dados de outra unidade mudando a requisição.
 */
@RestController
@RequestMapping("/meus-veiculos")
@RequiredArgsConstructor
@Tag(name = "Meus veículos", description = "A tela do morador: os veículos e vagas da própria unidade. A unidade sai sempre do token.")
public class MeusVeiculosController {

    private final MeusVeiculosService service;

    @GetMapping
    @Operation(summary = "Meus veículos e vagas", description = "Os veículos da unidade de quem chama, com as pessoas vinculadas, e as vagas com a ocupação de agora.")
    @ApiResponse(responseCode = "200", description = "Veículos e vagas.")
    public MeusVeiculosResponseDTO listar() {
        return service.listar(CondominioUtils.unidadeIdObrigatoria());
    }

    /** Opções para o seletor de pessoas vinculadas. */
    @GetMapping("/pessoas-unidade")
    @Operation(summary = "Pessoas da unidade", description = "Quem pode ser vinculado a um veículo da unidade.")
    @ApiResponse(responseCode = "200", description = "Pessoas.")
    public List<PessoaVinculadaDTO> pessoasDaUnidade() {
        CondominioUtils.unidadeIdObrigatoria();
        return service.pessoasDaUnidade();
    }

    @PostMapping
    @Operation(summary = "Cadastrar meu veículo", description = "Cadastra um veículo na unidade de quem chama, com as pessoas vinculadas e a vaga.")
    @ApiResponse(responseCode = "201", description = "Veículo criado.")
    public ResponseEntity<MeuVeiculoDTO> cadastrar(@RequestBody CadastrarMeuVeiculoDTO dto) {
        var criado = service.cadastrar(CondominioUtils.unidadeIdObrigatoria(), currentUserId(), dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar meu veículo", description = "Troca modelo, cor, observação e vaga.")
    @ApiResponse(responseCode = "200", description = "Veículo atualizado.")
    public MeuVeiculoDTO atualizar(@Parameter(description = "Id do veículo.", example = "1f3e5a7c-9b2d-4e6f-a8c0-2d4f6b8a0c1e") @PathVariable String id, @RequestBody AtualizarMeuVeiculoDTO dto) {
        return service.atualizar(id, CondominioUtils.unidadeIdObrigatoria(), dto);
    }

    /** Remove o vínculo do próprio morador — ou o veículo, se for o último. */
    @DeleteMapping("/{id}/vinculo")
    @Operation(summary = "Sair de um veículo", description = "Remove o vínculo de quem chama; se era o último, o veículo é removido.")
    @ApiResponse(responseCode = "200", description = "Resultado.")
    public DesvincularResultadoDTO desvincular(@Parameter(description = "Id do veículo.", example = "1f3e5a7c-9b2d-4e6f-a8c0-2d4f6b8a0c1e") @PathVariable String id) {
        return service.desvincular(id, CondominioUtils.unidadeIdObrigatoria(), currentUserId());
    }

    @PostMapping("/{id}/pessoas/{pessoaId}")
    @Operation(summary = "Vincular pessoa ao veículo", description = "Acrescenta uma pessoa da unidade ao veículo.")
    @ApiResponse(responseCode = "200", description = "Veículo atualizado.")
    public MeuVeiculoDTO vincularPessoa(@Parameter(description = "Id do veículo.", example = "1f3e5a7c-9b2d-4e6f-a8c0-2d4f6b8a0c1e") @PathVariable String id, @Parameter(description = "Id da pessoa (auth-api).", example = "5") @PathVariable String pessoaId) {
        return service.vincularPessoa(id, CondominioUtils.unidadeIdObrigatoria(), pessoaId);
    }

    @DeleteMapping("/{id}/pessoas/{pessoaId}")
    @Operation(summary = "Desvincular pessoa do veículo", description = "Tira uma pessoa da unidade do veículo.")
    @ApiResponse(responseCode = "200", description = "Veículo atualizado.")
    public MeuVeiculoDTO desvincularPessoa(@Parameter(description = "Id do veículo.", example = "1f3e5a7c-9b2d-4e6f-a8c0-2d4f6b8a0c1e") @PathVariable String id, @Parameter(description = "Id da pessoa (auth-api).", example = "5") @PathVariable String pessoaId) {
        return service.desvincularPessoa(id, CondominioUtils.unidadeIdObrigatoria(), pessoaId);
    }

    private String currentUserId() {
        JwtClaims claims = AuthContext.get();
        if (claims == null || claims.authUserId() == null) {
            throw new OperacaoInvalidaException("Usuário não autenticado");
        }
        return claims.authUserId();
    }
}
