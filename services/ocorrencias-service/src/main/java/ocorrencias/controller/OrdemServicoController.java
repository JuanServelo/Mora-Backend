package ocorrencias.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ocorrencias.dto.AndamentoRequest;
import ocorrencias.dto.ConclusaoRequest;
import ocorrencias.dto.NovaOrdemServicoRequest;
import ocorrencias.dto.OrdemServicoResponse;
import ocorrencias.model.enums.StatusOrdemServico;
import ocorrencias.security.Autorizacao;
import ocorrencias.service.EstatisticasService;
import ocorrencias.service.OrdemServicoService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Ordens de serviço", description = "Manutenção corretiva (vinda de ocorrência) e preventiva, com prazo, responsável, andamento e conclusão.")
@RestController
@RequestMapping("/ordens-servico")
@RequiredArgsConstructor
public class OrdemServicoController {

    /** Uma OS com o histórico de andamento, como o detalhe a devolve. */
    static final String EXEMPLO = """
            { "id": 4, "numero": "OS-MVHIK2AB-X7Q2", "descricao": "Trocar o registro do 3º andar.", "prioridade": "ALTA",
              "prazo": "2026-11-05", "responsavel": "Hidráulica Silva",
              "fornecedor": { "id": 1, "nome": "Hidráulica Silva", "servico": "Hidráulica", "telefone": "(41) 99999-0000" },
              "reclamacao": { "id": 3, "protocolo": "REC-MVHIJIIS-623Q", "categoria": "HIDRAULICA" },
              "preventiva": false, "status": "EM_EXECUCAO", "condominioId": "cond-teste",
              "abertaPorEmail": "sindico.ocorrencias@mora.com", "custo": null, "observacoes": null,
              "concluidaEm": null, "concluidaNoPrazo": false, "criadoEm": "2026-10-21T09:10:00", "atualizadoEm": "2026-10-21T15:02:00",
              "andamentos": [
                { "id": 9, "mensagem": "OS aberta a partir da ocorrência REC-MVHIJIIS-623Q.", "statusAnterior": null, "statusNovo": "ABERTA",
                  "autorEmail": "sindico.ocorrencias@mora.com", "sistema": false, "criadoEm": "2026-10-21T09:10:00" },
                { "id": 10, "mensagem": "Peça comprada; instalação amanhã.", "statusAnterior": "ABERTA", "statusNovo": "EM_EXECUCAO",
                  "autorEmail": "sindico.ocorrencias@mora.com", "sistema": false, "criadoEm": "2026-10-21T15:02:00" } ] }""";

    private final OrdemServicoService ordemServicoService;
    private final EstatisticasService estatisticasService;

    @Operation(summary = "Abrir OS",
            description = "Com `reclamacaoId`, fica vinculada à ocorrência, entra no histórico dela e o morador é notificado; "
                    + "sem, é manutenção preventiva. Sem `responsavel`, o responsável é o fornecedor. Nasce ABERTA. Perfil: ADMIN_SINDICO.")
    @ApiResponse(responseCode = "201", description = "OS aberta.", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = EXEMPLO)))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrdemServicoResponse abrir(@Valid @RequestBody NovaOrdemServicoRequest req) {
        return ordemServicoService.abrir(req);
    }

    @Operation(summary = "Listar OS", description = "As do condomínio, mais novas primeiro, sem o histórico. Perfis: ADMIN_SINDICO e ADMIN_GERAL.")
    @ApiResponse(responseCode = "200", description = "Ordens de serviço.", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
            [ { "id": 4, "numero": "OS-MVHIK2AB-X7Q2", "descricao": "Trocar o registro do 3º andar.", "prioridade": "ALTA",
                "prazo": "2026-11-05", "responsavel": "Hidráulica Silva",
                "fornecedor": { "id": 1, "nome": "Hidráulica Silva", "servico": "Hidráulica", "telefone": "(41) 99999-0000" },
                "reclamacao": { "id": 3, "protocolo": "REC-MVHIJIIS-623Q", "categoria": "HIDRAULICA" },
                "preventiva": false, "status": "ATRASADA", "condominioId": "cond-teste", "abertaPorEmail": "sindico.ocorrencias@mora.com",
                "custo": null, "observacoes": null, "concluidaEm": null, "concluidaNoPrazo": false,
                "criadoEm": "2026-10-21T09:10:00", "atualizadoEm": "2026-11-06T06:00:00", "andamentos": null } ]""")))
    @GetMapping
    public List<OrdemServicoResponse> listar(
            @Parameter(description = "Só para o Admin Geral.", example = "cond-teste") @RequestParam(required = false) String condominioId,
            @Parameter(description = "Filtra por status.", example = "ATRASADA") @RequestParam(required = false) StatusOrdemServico status) {
        return ordemServicoService.listar(condominioId, status);
    }

    @Operation(summary = "Indicadores",
            description = "Abertas, em execução, atrasadas, críticas atrasadas e percentual concluído no prazo, agregados em SQL. "
                    + "Perfis: ADMIN_SINDICO e ADMIN_GERAL.")
    @ApiResponse(responseCode = "200", description = "Indicadores.", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
            { "abertas": 2, "emExecucao": 1, "atrasadas": 1, "criticasAtrasadas": 0,
              "concluidas": 4, "concluidasNoPrazo": 3, "percentualNoPrazo": 75 }""")))
    @GetMapping("/estatisticas")
    public EstatisticasService.IndicadoresOrdemServico estatisticas(
            @Parameter(description = "Só para o Admin Geral.", example = "cond-teste") @RequestParam(required = false) String condominioId) {
        Autorizacao.exigirVisaoDeGestao("consultar indicadores de ordens de serviço");
        return estatisticasService.ordensServico(Autorizacao.condominioDeLeitura(condominioId));
    }

    @Operation(summary = "Detalhe da OS", description = "A OS com o histórico de andamento. De outro condomínio responde 404.")
    @ApiResponse(responseCode = "200", description = "Ordem de serviço.", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = EXEMPLO)))
    @GetMapping("/{id}")
    public OrdemServicoResponse detalhar(@Parameter(description = "Id da OS.", example = "4") @PathVariable Long id) {
        return ordemServicoService.detalhar(id);
    }

    @Operation(summary = "Registrar andamento",
            description = "Entra no histórico com data e autor. O primeiro põe a OS EM_EXECUCAO; uma atrasada continua atrasada. "
                    + "OS concluída responde 409. Perfil: ADMIN_SINDICO.")
    @ApiResponse(responseCode = "200", description = "OS atualizada.", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = EXEMPLO)))
    @PostMapping("/{id}/andamentos")
    public OrdemServicoResponse registrarAndamento(@Parameter(description = "Id da OS.", example = "4") @PathVariable Long id,
                                                   @Valid @RequestBody AndamentoRequest req) {
        return ordemServicoService.registrarAndamento(id, req);
    }

    @Operation(summary = "Concluir", description = "Registra custo e observações e passa a CONCLUIDA; conta no indicador de OS no prazo. Perfil: ADMIN_SINDICO.")
    @ApiResponse(responseCode = "200", description = "OS concluída.", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
            { "id": 4, "numero": "OS-MVHIK2AB-X7Q2", "descricao": "Trocar o registro do 3º andar.", "prioridade": "ALTA",
              "prazo": "2026-11-05", "responsavel": "Hidráulica Silva", "fornecedor": null, "reclamacao": null, "preventiva": true,
              "status": "CONCLUIDA", "condominioId": "cond-teste", "abertaPorEmail": "sindico.ocorrencias@mora.com",
              "custo": 250.50, "observacoes": "Registro trocado e testado.", "concluidaEm": "2026-11-03T16:40:00", "concluidaNoPrazo": true,
              "criadoEm": "2026-10-21T09:10:00", "atualizadoEm": "2026-11-03T16:40:00",
              "andamentos": [ { "id": 11, "mensagem": "OS concluída. Registro trocado e testado.", "statusAnterior": "EM_EXECUCAO",
                "statusNovo": "CONCLUIDA", "autorEmail": "sindico.ocorrencias@mora.com", "sistema": false, "criadoEm": "2026-11-03T16:40:00" } ] }""")))
    @PostMapping("/{id}/conclusao")
    public OrdemServicoResponse concluir(@Parameter(description = "Id da OS.", example = "4") @PathVariable Long id,
                                         @Valid @RequestBody ConclusaoRequest req) {
        return ordemServicoService.concluir(id, req);
    }
}
