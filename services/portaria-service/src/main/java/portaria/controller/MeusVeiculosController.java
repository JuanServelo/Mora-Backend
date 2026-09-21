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

/**
 * Tela do morador: ele gerencia apenas os veículos e vagas da própria unidade.
 *
 * A unidade vem sempre do JWT (unidadeIdObrigatoria), nunca de parâmetro —
 * não há como pedir os dados de outra unidade mudando a requisição.
 */
@RestController
@RequestMapping("/meus-veiculos")
@RequiredArgsConstructor
public class MeusVeiculosController {

    private final MeusVeiculosService service;

    @GetMapping
    public MeusVeiculosResponseDTO listar() {
        return service.listar(CondominioUtils.unidadeIdObrigatoria());
    }

    /** Opções para o seletor de pessoas vinculadas. */
    @GetMapping("/pessoas-unidade")
    public List<PessoaVinculadaDTO> pessoasDaUnidade() {
        CondominioUtils.unidadeIdObrigatoria();
        return service.pessoasDaUnidade();
    }

    @PostMapping
    public ResponseEntity<MeuVeiculoDTO> cadastrar(@RequestBody CadastrarMeuVeiculoDTO dto) {
        var criado = service.cadastrar(CondominioUtils.unidadeIdObrigatoria(), currentUserId(), dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    @PutMapping("/{id}")
    public MeuVeiculoDTO atualizar(@PathVariable String id, @RequestBody AtualizarMeuVeiculoDTO dto) {
        return service.atualizar(id, CondominioUtils.unidadeIdObrigatoria(), dto);
    }

    /** Remove o vínculo do próprio morador — ou o veículo, se for o último. */
    @DeleteMapping("/{id}/vinculo")
    public DesvincularResultadoDTO desvincular(@PathVariable String id) {
        return service.desvincular(id, CondominioUtils.unidadeIdObrigatoria(), currentUserId());
    }

    @PostMapping("/{id}/pessoas/{pessoaId}")
    public MeuVeiculoDTO vincularPessoa(@PathVariable String id, @PathVariable String pessoaId) {
        return service.vincularPessoa(id, CondominioUtils.unidadeIdObrigatoria(), pessoaId);
    }

    @DeleteMapping("/{id}/pessoas/{pessoaId}")
    public MeuVeiculoDTO desvincularPessoa(@PathVariable String id, @PathVariable String pessoaId) {
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
