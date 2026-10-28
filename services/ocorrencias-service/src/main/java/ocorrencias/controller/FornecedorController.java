package ocorrencias.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ocorrencias.dto.FornecedorRequest;
import ocorrencias.model.Fornecedor;
import ocorrencias.service.FornecedorService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Fornecedores", description = "Prestadores acionados nas ordens de serviço. Não se excluem: desativam.")
@RestController
@RequestMapping("/fornecedores")
@RequiredArgsConstructor
public class FornecedorController {

    static final String EXEMPLO = """
            { "id": 1, "condominioId": "cond-teste", "nome": "Hidráulica Silva", "servico": "Hidráulica",
              "telefone": "(41) 99999-0000", "email": "contato@hidraulicasilva.com", "documento": "12.345.678/0001-90",
              "ativo": true, "criadoEm": "2026-10-21T08:55:00" }""";

    private final FornecedorService fornecedorService;

    @Operation(summary = "Listar fornecedores", description = "Os do condomínio, por nome, ativos e inativos. Perfis: ADMIN_SINDICO e ADMIN_GERAL.")
    @ApiResponse(responseCode = "200", description = "Fornecedores.", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "[" + EXEMPLO + "]")))
    @GetMapping
    public List<Fornecedor> listar(
            @Parameter(description = "Só para o Admin Geral.", example = "cond-teste") @RequestParam(required = false) String condominioId) {
        return fornecedorService.listar(condominioId);
    }

    @Operation(summary = "Cadastrar fornecedor", description = "No condomínio de quem chama; nasce ativo. Perfil: ADMIN_SINDICO.")
    @ApiResponse(responseCode = "201", description = "Fornecedor cadastrado.", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = EXEMPLO)))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Fornecedor criar(@Valid @RequestBody FornecedorRequest req) {
        return fornecedorService.criar(req);
    }

    @Operation(summary = "Editar, desativar ou reativar fornecedor",
            description = "Fornecedor não se exclui: uma OS concluída continua apontando para quem a executou. Perfil: ADMIN_SINDICO.")
    @ApiResponse(responseCode = "200", description = "Fornecedor atualizado.", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = EXEMPLO)))
    @PutMapping("/{id}")
    public Fornecedor atualizar(@Parameter(description = "Id do fornecedor.", example = "1") @PathVariable Long id,
                                @Valid @RequestBody FornecedorRequest req) {
        return fornecedorService.atualizar(id, req);
    }
}
