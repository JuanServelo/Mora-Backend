package portaria.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import portaria.model.Funcionario;
import portaria.security.CondominioUtils;
import portaria.service.FuncionarioService;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/funcionarios")
@RequiredArgsConstructor
@Tag(name = "Funcionários", description = "Cadastro de funcionários na portaria, com cargo e horário.")
public class FuncionarioController {

    private final FuncionarioService funcionarioService;

    @PostMapping("/cadastrar")
    @Operation(summary = "Cadastrar funcionário", description = "Cria o funcionário com cargo, matrícula e horário de entrada e saída.")
    @ApiResponse(responseCode = "201", description = "Funcionário criado.")
    public ResponseEntity<Funcionario> cadastrar(@Valid @RequestBody Funcionario funcionario) {
        return ResponseEntity.status(HttpStatus.CREATED).body(funcionarioService.cadastrar(funcionario));
    }

    @GetMapping
    @Operation(summary = "Listar funcionários ativos", description = "Funcionários ativos do condomínio.")
    @ApiResponse(responseCode = "200", description = "Funcionários.")
    public List<Funcionario> listarAtivos() {
        return funcionarioService.listarAtivos(CondominioUtils.condominioIdEfetivo());
    }

    @GetMapping("/todos")
    @Operation(summary = "Listar todos os funcionários", description = "Ativos e desativados.")
    @ApiResponse(responseCode = "200", description = "Funcionários.")
    public List<Funcionario> listarTodos() {
        return funcionarioService.listarTodos(CondominioUtils.condominioIdEfetivo());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe do funcionário", description = "Um funcionário do condomínio de quem chama.")
    @ApiResponse(responseCode = "200", description = "Funcionário.")
    public Funcionario buscarPorId(@Parameter(description = "Id do funcionário.", example = "3") @PathVariable String id) {
        return funcionarioService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar funcionário", description = "Substitui os dados, o cargo e o horário.")
    @ApiResponse(responseCode = "200", description = "Funcionário atualizado.")
    public Funcionario atualizar(@Parameter(description = "Id do funcionário.", example = "3") @PathVariable String id, @Valid @RequestBody Funcionario dados) {
        return funcionarioService.atualizar(id, dados);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desativar funcionário", description = "Desativa o cadastro, sem apagar o histórico.")
    @ApiResponse(responseCode = "200", description = "Desativado.")
    public ResponseEntity<Void> desativar(@Parameter(description = "Id do funcionário.", example = "3") @PathVariable String id) {
        funcionarioService.desativar(id);
        return ResponseEntity.noContent().build();
    }
}
