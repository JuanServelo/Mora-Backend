package com.mora.plan.controller;

import com.mora.plan.dto.AssinaturaRequestDTO;
import com.mora.plan.dto.AssinaturaResponseDTO;
import com.mora.plan.dto.ErrorResponse;
import com.mora.plan.enums.StatusAssinatura;
import com.mora.plan.security.AuthContext;
import com.mora.plan.security.JwtClaims;
import com.mora.plan.service.AssinaturaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/assinaturas")
@RequiredArgsConstructor
@Tag(name = "Assinaturas", description = "Assinatura de cada condomínio, com vigência e status. É o modelo que o front e o gestao-geral leem. Exige token; o serviço ainda não restringe por perfil.")
public class AssinaturaController {

    private final AssinaturaService assinaturaService;

    @GetMapping
    @Operation(summary = "Listar assinaturas", description = "Todas as assinaturas, com o plano e se estão vigentes hoje. Usado pelo painel de receita do Admin Geral.")
    @ApiResponse(responseCode = "200", description = "Assinaturas.")
    public List<AssinaturaResponseDTO> listar() {
        return assinaturaService.listarTodas();
    }

    /**
     * Painel de receita da plataforma, calculado pela função
     * {@code receita_plataforma} do banco. Só o Admin Geral: é o faturamento da
     * empresa, não de um condomínio.
     */
    @GetMapping("/receita")
    @Operation(summary = "Receita da plataforma", description = "Receita recorrente, receita por plano, evolução mês a mês e o quanto cada cliente já rendeu, tudo agregado no PostgreSQL pela função `receita_plataforma` (GROUP BY, generate_series, daterange, DISTINCT ON e jsonb). Valores em centavos. Com `plano`, os números descrevem só aquele plano. É o que o gestao-geral consome em /api/gestao/receita. Perfil: ADMIN_GERAL.")
    @Parameter(name = "meses", description = "Janela da evolução, de 1 a 36 meses (padrão 12).", example = "6")
    @Parameter(name = "plano", description = "Nome do plano; vazio ou `todos` para todos.", example = "Completo")
    @ApiResponse(responseCode = "200", description = "Painel de receita.", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
            {
              "filtros": { "meses": 3, "plano": "todos" },
              "planosDisponiveis": ["Básico", "Completo"],
              "indicadores": { "mrrCentavos": 150000, "clientesAtivos": 2, "ticketMedioCentavos": 75000, "acumuladoCentavos": 1200000 },
              "porPlano": [
                { "plano": "Completo", "assinaturas": 1, "mrrCentavos": 100000 },
                { "plano": "Básico", "assinaturas": 1, "mrrCentavos": 50000 }
              ],
              "evolucao": [
                { "mes": "2026-08", "clientes": 1, "mrrCentavos": 100000 },
                { "mes": "2026-09", "clientes": 2, "mrrCentavos": 150000 },
                { "mes": "2026-10", "clientes": 2, "mrrCentavos": 150000 }
              ],
              "clientes": [
                { "condominioId": "cond-parque-verde", "plano": "Completo", "status": "ATIVA", "vigente": true, "desde": "2026-01-01", "mensalidadeCentavos": 100000, "acumuladoCentavos": 1000000 },
                { "condominioId": "cond-jardim-sul", "plano": "Básico", "status": "ATIVA", "vigente": true, "desde": "2026-07-01", "mensalidadeCentavos": 50000, "acumuladoCentavos": 200000 }
              ],
              "condominiosComAssinatura": ["cond-jardim-sul", "cond-parque-verde"]
            }""")))
    @ApiResponse(responseCode = "403", description = "Perfil diferente de ADMIN_GERAL.")
    public ResponseEntity<?> receita(@RequestParam(required = false) Integer meses,
                                     @RequestParam(required = false) String plano) {
        JwtClaims quem = AuthContext.get();
        if (quem == null || !"ADMIN_GERAL".equals(quem.perfil())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(
                    "A receita da plataforma é do Admin Geral.",
                    HttpStatus.FORBIDDEN.value(),
                    System.currentTimeMillis()));
        }
        return ResponseEntity.ok(assinaturaService.receita(meses, plano));
    }

    /** Assinatura vigente de um condomínio — é o que o gestao-geral consome. */
    @GetMapping("/condominio/{condominioId}")
    @Operation(summary = "Assinatura vigente do condomínio", description = "A assinatura ATIVA dentro da vigência; 404 se não houver. É o que o gestao-geral consome para o painel.")
    @Parameter(name = "condominioId", description = "Id do condomínio.", example = "cond-parque-verde")
    @ApiResponse(responseCode = "200", description = "Assinatura vigente.")
    public AssinaturaResponseDTO vigente(@PathVariable String condominioId) {
        return assinaturaService.buscarVigentePorCondominio(condominioId);
    }

    @GetMapping("/condominio/{condominioId}/historico")
    @Operation(summary = "Histórico do condomínio", description = "Todas as assinaturas que o condomínio já teve, da mais recente para a mais antiga.")
    @Parameter(name = "condominioId", description = "Id do condomínio.", example = "cond-parque-verde")
    @ApiResponse(responseCode = "200", description = "Assinaturas.")
    public List<AssinaturaResponseDTO> historico(@PathVariable String condominioId) {
        return assinaturaService.historicoDoCondominio(condominioId);
    }

    @PostMapping
    @Operation(summary = "Criar assinatura", description = "Vincula o condomínio a um plano, com vigência. Sem status, nasce ATIVA.")
    @ApiResponse(responseCode = "201", description = "Assinatura criada.")
    public ResponseEntity<AssinaturaResponseDTO> criar(@Valid @RequestBody AssinaturaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assinaturaService.criar(dto));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Mudar status", description = "ATIVA, SUSPENSA ou CANCELADA. Suspensa ou cancelada deixa de ser vigente na hora.")
    @Parameter(name = "id", description = "Id da assinatura.", example = "7")
    @ApiResponse(responseCode = "200", description = "Assinatura atualizada.")
    public AssinaturaResponseDTO alterarStatus(@PathVariable Long id,
                                               @Parameter(description = "Novo status.", example = "SUSPENSA") @RequestParam StatusAssinatura status) {
        return assinaturaService.alterarStatus(id, status);
    }
}
