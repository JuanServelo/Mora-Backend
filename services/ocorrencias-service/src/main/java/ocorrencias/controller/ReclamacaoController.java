package ocorrencias.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ocorrencias.dto.AtendimentoRequest;
import ocorrencias.dto.NovaReclamacaoRequest;
import ocorrencias.dto.ReclamacaoResponse;
import ocorrencias.model.enums.CategoriaReclamacao;
import ocorrencias.model.enums.StatusReclamacao;
import ocorrencias.security.Autorizacao;
import ocorrencias.service.EstatisticasReclamacoes;
import ocorrencias.service.EstatisticasService;
import ocorrencias.service.ReclamacaoService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@Tag(name = "Ocorrências", description = "Registro e atendimento das reclamações dos moradores, com histórico e estatísticas agregadas em SQL.")
@RestController
@RequestMapping("/reclamacoes")
@RequiredArgsConstructor
public class ReclamacaoController {

    /** Uma ocorrência como a gestão a vê, já respondida e com OS vinculada. */
    static final String EXEMPLO = """
            { "id": 3, "protocolo": "REC-MVHIJIIS-623Q", "categoria": "ILUMINACAO",
              "descricao": "Lâmpada do hall do bloco A queimada desde ontem.", "anexoUrl": null,
              "status": "EM_ANALISE", "condominioId": "cond-teste", "usuarioId": "26",
              "usuario": { "nome": "Morador Ocorrências", "email": "morador.ocorrencias@mora.com" },
              "criadoEm": "2026-10-20T23:48:51", "atualizadoEm": "2026-10-20T23:50:02", "resolvidoEm": null,
              "interacoes": [ { "id": 7, "mensagem": "Zelador vai trocar a lâmpada amanhã de manhã.",
                "statusAnterior": "PENDENTE", "statusNovo": "EM_ANALISE", "autorPerfil": "ADMIN_SINDICO",
                "autorEmail": "sindico.ocorrencias@mora.com", "criadoEm": "2026-10-20T23:50:02" } ],
              "ordensServico": [ { "id": 4, "numero": "OS-MVHIK2AB-X7Q2", "status": "ABERTA", "prioridade": "MEDIA", "prazo": "2026-10-23" } ] }""";

    private final ReclamacaoService reclamacaoService;
    private final EstatisticasService estatisticasService;

    @Operation(summary = "Registrar ocorrência",
            description = "Nasce PENDENTE, com protocolo, no condomínio de quem registra. Qualquer usuário do condomínio.")
    @ApiResponse(responseCode = "201", description = "Ocorrência registrada.", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
            { "id": 3, "protocolo": "REC-MVHIJIIS-623Q", "categoria": "ILUMINACAO",
              "descricao": "Lâmpada do hall do bloco A queimada desde ontem.", "anexoUrl": null, "status": "PENDENTE",
              "condominioId": "cond-teste", "usuarioId": "26", "usuario": null, "criadoEm": "2026-10-20T23:48:51",
              "atualizadoEm": "2026-10-20T23:48:51", "resolvidoEm": null, "interacoes": [], "ordensServico": [] }""")))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReclamacaoResponse registrar(@Valid @RequestBody NovaReclamacaoRequest req) {
        return reclamacaoService.registrar(req);
    }

    @Operation(summary = "Minhas ocorrências",
            description = "Só as de quem pede, mais novas primeiro, com o histórico de respostas e as OS vinculadas.")
    @ApiResponse(responseCode = "200", description = "Ocorrências de quem pede.", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "[" + EXEMPLO + "]")))
    @GetMapping("/minhas")
    public List<ReclamacaoResponse> minhas() {
        return reclamacaoService.minhas();
    }

    @Operation(summary = "Ocorrências do condomínio",
            description = "Visão da gestão, com nome de quem abriu. Síndico: as do próprio condomínio. Admin Geral: todas, ou as de `condominioId`.")
    @ApiResponse(responseCode = "200", description = "Ocorrências.", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "[" + EXEMPLO + "]")))
    @GetMapping
    public List<ReclamacaoResponse> listar(
            @Parameter(description = "Só para o Admin Geral.", example = "cond-teste") @RequestParam(required = false) String condominioId,
            @Parameter(description = "Filtra por status.", example = "PENDENTE") @RequestParam(required = false) StatusReclamacao status,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return reclamacaoService.listarDaGestao(condominioId, status, authorization);
    }

    @Operation(summary = "Categorias aceitas", description = "As categorias que o registro aceita, na ordem da tela.")
    @ApiResponse(responseCode = "200", description = "Categorias.", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
            ["BARULHO", "INFRAESTRUTURA", "LIMPEZA", "GARAGEM", "ANIMAIS", "ELEVADORES", "ILUMINACAO",
             "ELETRICA", "HIDRAULICA", "PINTURA", "JARDINAGEM", "SUGESTAO_MELHORIA", "OUTROS"]""")))
    @GetMapping("/categorias")
    public List<String> categorias() {
        return Arrays.stream(CategoriaReclamacao.values()).map(Enum::name).toList();
    }

    @Operation(summary = "Estatísticas para o painel do síndico",
            description = "Situação atual, abertas e resolvidas por mês, por categoria e tempo médio de resolução, agregados em SQL. "
                    + "`meses` de 1 a 24 (padrão 6). Perfis: ADMIN_SINDICO e ADMIN_GERAL.")
    @ApiResponse(responseCode = "200", description = "Estatísticas.", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
            { "meses": ["2026-09", "2026-10"],
              "atual": { "abertas": 2, "pendentes": 1, "emAnalise": 1, "maisAntigaDesde": "2026-09-28T10:15:00" },
              "porMes": [ { "mes": "2026-09", "abertas": 3, "resolvidas": 2 }, { "mes": "2026-10", "abertas": 1, "resolvidas": 1 } ],
              "porCategoria": [ { "categoria": "BARULHO", "total": 2 }, { "categoria": "ILUMINACAO", "total": 1 } ],
              "tempoMedioResolucaoDias": 1.5 }""")))
    @GetMapping("/estatisticas")
    public EstatisticasReclamacoes.Resultado estatisticas(
            @Parameter(description = "Janela das séries, de 1 a 24.", example = "6") @RequestParam(required = false) Integer meses,
            @Parameter(description = "Só para o Admin Geral.", example = "cond-teste") @RequestParam(required = false) String condominioId) {
        Autorizacao.exigirVisaoDeGestao("consultar estatísticas de ocorrências");
        return estatisticasService.reclamacoes(Autorizacao.condominioDeLeitura(condominioId), meses);
    }

    @Operation(summary = "Contagem por status",
            description = "Para os painéis. Admin Geral: `condominioIds` separados por vírgula; ausente = todos; vazio = nenhum. "
                    + "Síndico: sempre o próprio condomínio.")
    @ApiResponse(responseCode = "200", description = "Contagem.", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
            { "total": 12, "abertas": 5, "porStatus": { "PENDENTE": 3, "EM_ANALISE": 2, "RESOLVIDO": 7 } }""")))
    @GetMapping("/contagem")
    public EstatisticasReclamacoes.Contagem contagem(
            @Parameter(description = "Ids separados por vírgula (Admin Geral).", example = "cond-teste,residencial-mora") @RequestParam(required = false) String condominioIds) {
        Autorizacao.exigirVisaoDeGestao("consultar a contagem de ocorrências");
        if (!Autorizacao.ehAdminGeral()) {
            return estatisticasService.contagem(List.of(Autorizacao.exigirCondominio()));
        }
        List<String> ids = condominioIds == null
                ? null
                : Arrays.stream(condominioIds.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        return estatisticasService.contagem(ids);
    }

    @Operation(summary = "Detalhe de uma ocorrência",
            description = "Para quem a abriu ou para a gestão do condomínio dela. De outro condomínio responde 404.")
    @ApiResponse(responseCode = "200", description = "Ocorrência.", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = EXEMPLO)))
    @GetMapping("/{id}")
    public ReclamacaoResponse detalhar(@Parameter(description = "Id da ocorrência.", example = "3") @PathVariable Long id,
                                       @RequestHeader(value = "Authorization", required = false) String authorization) {
        return reclamacaoService.detalhar(id, authorization);
    }

    @Operation(summary = "Responder ou mudar status",
            description = "A resposta entra no histórico com autor e data; com `status`, ela é a justificativa. "
                    + "O morador é notificado pela fila. Perfil: ADMIN_SINDICO.")
    @ApiResponse(responseCode = "200", description = "Ocorrência atualizada.", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = EXEMPLO)))
    @PatchMapping("/{id}")
    public ReclamacaoResponse atender(@Parameter(description = "Id da ocorrência.", example = "3") @PathVariable Long id,
                                      @Valid @RequestBody AtendimentoRequest req,
                                      @RequestHeader(value = "Authorization", required = false) String authorization) {
        return reclamacaoService.atender(id, req, authorization);
    }
}
