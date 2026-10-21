package portaria.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import portaria.model.Morador;
import portaria.security.CondominioUtils;
import portaria.service.MoradorService;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/moradores")
@RequiredArgsConstructor
@Tag(name = "Moradores", description = "Cadastro de moradores na portaria, com a unidade.")
public class MoradorController {

    private final MoradorService moradorService;

    @PostMapping("/cadastrar")
    @Operation(summary = "Cadastrar morador", description = "Cria o morador ligado a uma unidade do condomínio de quem chama.")
    @ApiResponse(responseCode = "201", description = "Morador criado.")
    public ResponseEntity<Morador> cadastrar(@Valid @RequestBody Morador morador) {
        return ResponseEntity.status(HttpStatus.CREATED).body(moradorService.cadastrar(morador));
    }

    @GetMapping
    @Operation(summary = "Listar moradores ativos", description = "Moradores ativos do condomínio.")
    @ApiResponse(responseCode = "200", description = "Moradores.")
    public List<Morador> listarAtivos() {
        return moradorService.listarAtivos(CondominioUtils.condominioIdEfetivo());
    }

    @GetMapping("/paginado")
    @Operation(summary = "Moradores ativos, paginado", description = "Moradores ativos em páginas de 20, ordenados por nome.")
    @ApiResponse(responseCode = "200", description = "Página de moradores.")
    public Page<Morador> listarAtivosPaginado(@PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        return moradorService.listarAtivosPaginado(CondominioUtils.condominioIdEfetivo(), pageable);
    }

    @GetMapping("/todos")
    @Operation(summary = "Listar todos os moradores", description = "Ativos e desativados.")
    @ApiResponse(responseCode = "200", description = "Moradores.")
    public List<Morador> listarTodos() {
        return moradorService.listarTodos(CondominioUtils.condominioIdEfetivo());
    }

    @GetMapping("/todos/paginado")
    @Operation(summary = "Todos os moradores, paginado", description = "Ativos e desativados, em páginas de 20.")
    @ApiResponse(responseCode = "200", description = "Página de moradores.")
    public Page<Morador> listarTodosPaginado(@PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        return moradorService.listarTodosPaginado(CondominioUtils.condominioIdEfetivo(), pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe do morador", description = "Um morador do condomínio de quem chama.")
    @ApiResponse(responseCode = "200", description = "Morador.")
    public Morador buscarPorId(@Parameter(description = "Id do morador.", example = "4") @PathVariable String id) {
        return moradorService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar morador", description = "Substitui os dados de contato e a unidade.")
    @ApiResponse(responseCode = "200", description = "Morador atualizado.")
    public Morador atualizar(@Parameter(description = "Id do morador.", example = "4") @PathVariable String id, @Valid @RequestBody Morador dados) {
        return moradorService.atualizar(id, dados);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desativar morador", description = "Desativa o cadastro, sem apagar o histórico.")
    @ApiResponse(responseCode = "200", description = "Desativado.")
    public ResponseEntity<Void> desativar(@Parameter(description = "Id do morador.", example = "4") @PathVariable String id) {
        moradorService.desativar(id);
        return ResponseEntity.noContent().build();
    }
}
