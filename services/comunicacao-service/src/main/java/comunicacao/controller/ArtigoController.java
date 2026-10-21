package comunicacao.controller;

import comunicacao.model.ArtigoConhecimento;
import comunicacao.model.enums.CategoriaArtigo;
import comunicacao.security.CondominioUtils;
import comunicacao.service.ArtigoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/artigos")
@RequiredArgsConstructor
@Tag(name = "Base de conhecimento", description = "Artigos e FAQ do condomínio (RF-12): regras, manuais, tutoriais e orientações de convivência.")
public class ArtigoController {

    private final ArtigoService artigoService;

    @PostMapping
    @Operation(summary = "Criar artigo", description = """
            Cria um artigo no condomínio de quem chama. Com `publicado: false`, fica como rascunho. O condomínio sai do \
            token. Perfil: ADMIN_SINDICO.""")
    @ApiResponse(responseCode = "201", description = "Artigo criado.")
    public ResponseEntity<ArtigoConhecimento> criar(@RequestBody ArtigoConhecimento artigo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(artigoService.criar(artigo));
    }

    @GetMapping
    @Operation(summary = "Listar artigos", description = """
            Sem filtro, a gestão vê todos (inclusive rascunhos). Com `publicadosOnly=true` ou `categoria`, só os \
            publicados: é o que o morador usa. Qualquer perfil, menos CONVIDADO e TERCEIRO.""")
    @ApiResponse(responseCode = "200", description = "Artigos.")
    public List<ArtigoConhecimento> listar(
            @Parameter(description = "Só os publicados.", example = "true") @RequestParam(required = false) boolean publicadosOnly,
            @Parameter(description = "Filtra pela categoria (só publicados).", example = "REGRA") @RequestParam(required = false) CategoriaArtigo categoria) {
        String condominioId = CondominioUtils.condominioIdEfetivo();
        if (publicadosOnly || categoria != null) {
            return artigoService.listarPublicados(condominioId, categoria);
        }
        return artigoService.listarTodos(condominioId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe do artigo", description = "Um artigo do condomínio de quem chama; de outro condomínio, 404.")
    @ApiResponse(responseCode = "200", description = "Artigo.")
    public ArtigoConhecimento buscarPorId(@Parameter(description = "Id do artigo.", example = "c4d2e8f1-5a7b-4c3d-9e1f-2b6a8d0c4e7f") @PathVariable UUID id) {
        return artigoService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar artigo", description = "Substitui título, conteúdo e categoria. Perfil: ADMIN_SINDICO.")
    @ApiResponse(responseCode = "200", description = "Artigo atualizado.")
    public ArtigoConhecimento atualizar(@Parameter(description = "Id do artigo.", example = "c4d2e8f1-5a7b-4c3d-9e1f-2b6a8d0c4e7f") @PathVariable UUID id, @RequestBody ArtigoConhecimento dados) {
        return artigoService.atualizar(id, dados);
    }

    @PatchMapping("/{id}/publicar")
    @Operation(summary = "Publicar artigo", description = "Torna o rascunho visível aos moradores. Perfil: ADMIN_SINDICO.")
    @ApiResponse(responseCode = "200", description = "Artigo publicado.")
    public ArtigoConhecimento publicar(@Parameter(description = "Id do artigo.", example = "c4d2e8f1-5a7b-4c3d-9e1f-2b6a8d0c4e7f") @PathVariable UUID id) {
        return artigoService.publicar(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Excluir artigo", description = "Apaga o artigo. Perfil: ADMIN_SINDICO.")
    @ApiResponse(responseCode = "204", description = "Excluído.")
    public void excluir(@Parameter(description = "Id do artigo.", example = "c4d2e8f1-5a7b-4c3d-9e1f-2b6a8d0c4e7f") @PathVariable UUID id) {
        artigoService.excluir(id);
    }
}
