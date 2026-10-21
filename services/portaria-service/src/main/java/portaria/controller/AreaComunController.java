package portaria.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import portaria.exception.AcessoNegadoException;
import portaria.model.AreaComum;
import portaria.security.CondominioUtils;
import portaria.service.AreaComunService;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/areas-comuns")
@RequiredArgsConstructor
@Tag(name = "Áreas comuns", description = "Espaços do condomínio que podem ser reservados (RF-10).")
public class AreaComunController {

    private final AreaComunService areaComunService;

    @PostMapping("/cadastrar")
    @Operation(summary = "Cadastrar área comum", description = "Cria um espaço, com capacidade, taxa de locação, política de cancelamento e se exige aprovação. O `condominioId` do corpo precisa ser o de quem chama.")
    @ApiResponse(responseCode = "201", description = "Área criada.")
    public ResponseEntity<AreaComum> cadastrar(@Valid @RequestBody AreaComum areaComum) {
        String condominioIdEfetivo = CondominioUtils.condominioIdEfetivo();
        if (condominioIdEfetivo != null && !condominioIdEfetivo.equals(areaComum.getCondominioId())) {
            throw new AcessoNegadoException("Acesso negado ao condomínio especificado.");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(areaComunService.cadastrar(areaComum));
    }

    @GetMapping
    @Operation(summary = "Listar áreas ativas", description = "Espaços ativos do condomínio. O Admin Geral pode informar `condominioId`.")
    @ApiResponse(responseCode = "200", description = "Áreas comuns.")
    public List<AreaComum> listarAtivas(@RequestParam(required = false) String condominioId) {
        return areaComunService.listarAtivas(CondominioUtils.resolverEscopo(condominioId));
    }

    @GetMapping("/todas")
    @Operation(summary = "Listar todas as áreas", description = "Ativas e desativadas.")
    @ApiResponse(responseCode = "200", description = "Áreas comuns.")
    public List<AreaComum> listarTodas(@RequestParam(required = false) String condominioId) {
        return areaComunService.listarTodas(CondominioUtils.resolverEscopo(condominioId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe da área comum", description = "Um espaço do condomínio de quem chama.")
    @ApiResponse(responseCode = "200", description = "Área comum.")
    public AreaComum buscarPorId(@Parameter(description = "Id da área comum.", example = "cf9f4897-b196-4186-83af-2d67678651c2") @PathVariable String id) {
        return areaComunService.buscarPorId(id);
    }

    @GetMapping("/tipo/{tipo}")
    @Operation(summary = "Áreas por tipo", description = "Espaços de um tipo (SALAO, PISCINA, CHURRASQUEIRA...).")
    @ApiResponse(responseCode = "200", description = "Áreas comuns.")
    public List<AreaComum> listarPorTipo(@Parameter(description = "Tipo da área.", example = "SALAO") @PathVariable String tipo) {
        return areaComunService.listarPorTipo(tipo);
    }

    @GetMapping("/tipo/{tipo}/ativas")
    @Operation(summary = "Áreas ativas por tipo", description = "Só os espaços ativos do tipo.")
    @ApiResponse(responseCode = "200", description = "Áreas comuns.")
    public List<AreaComum> listarPorTipoAtivas(@Parameter(description = "Tipo da área.", example = "SALAO") @PathVariable String tipo) {
        return areaComunService.listarPorTipoAtivas(tipo);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar área comum", description = "Substitui os dados do espaço. Só no condomínio de quem chama.")
    @ApiResponse(responseCode = "200", description = "Área atualizada.")
    public AreaComum atualizar(@Parameter(description = "Id da área comum.", example = "cf9f4897-b196-4186-83af-2d67678651c2") @PathVariable String id, @Valid @RequestBody AreaComum dados) {
        String condominioIdEfetivo = CondominioUtils.condominioIdEfetivo();
        if (condominioIdEfetivo != null) {
            AreaComum existente = areaComunService.buscarPorId(id);
            if (!condominioIdEfetivo.equals(existente.getCondominioId())) {
                throw new AcessoNegadoException("Acesso negado ao condomínio especificado.");
            }
        }
        return areaComunService.atualizar(id, dados);
    }

    @PutMapping("/{id}/desativar")
    @Operation(summary = "Desativar área comum", description = "O espaço deixa de aceitar reservas, sem apagar.")
    @ApiResponse(responseCode = "200", description = "Desativada.")
    public ResponseEntity<Void> desativar(@Parameter(description = "Id da área comum.", example = "cf9f4897-b196-4186-83af-2d67678651c2") @PathVariable String id) {
        areaComunService.desativar(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/ativar")
    @Operation(summary = "Reativar área comum", description = "O espaço volta a aceitar reservas.")
    @ApiResponse(responseCode = "200", description = "Reativada.")
    public ResponseEntity<Void> ativar(@Parameter(description = "Id da área comum.", example = "cf9f4897-b196-4186-83af-2d67678651c2") @PathVariable String id) {
        areaComunService.ativar(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir área comum", description = "Apaga o espaço.")
    @ApiResponse(responseCode = "200", description = "Excluída.")
    public ResponseEntity<Void> deletar(@Parameter(description = "Id da área comum.", example = "cf9f4897-b196-4186-83af-2d67678651c2") @PathVariable String id) {
        areaComunService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
