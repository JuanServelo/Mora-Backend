package com.mora.plan.dto;

import com.mora.plan.enums.StatusAssinatura;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Devolve a assinatura com os limites do plano já resolvidos, para que o
 * consumidor não precise de uma segunda chamada só para saber o que pode fazer.
 */
@Data
@Builder
public class AssinaturaResponseDTO {
    @Schema(description = "Id da assinatura.", example = "7")
    private Long id;
    @Schema(description = "Id do condomínio (auth-api).", example = "cond-parque-verde")
    private String condominioId;
    @Schema(description = "ATIVA, SUSPENSA ou CANCELADA (padrão ATIVA).", example = "ATIVA")
    private StatusAssinatura status;
    @Schema(description = "Início da vigência.", example = "2026-01-01")
    private LocalDate vigenciaInicio;
    @Schema(description = "Fim da vigência; vazio para sem prazo.", example = "2026-12-31")
    private LocalDate vigenciaFim;
    @Schema(description = "Se está ATIVA e dentro da vigência hoje.", example = "true")
    private boolean vigente;

    @Schema(description = "Id do plano.", example = "1")
    private Long planId;
    @Schema(description = "Nome do plano.", example = "Completo")
    private String planNome;
    @Schema(description = "Mensalidade em reais.", example = "1000.00")
    private BigDecimal mensalidade;
    @Schema(description = "Limite de condomínios do plano.", example = "10")
    private Integer maxCondominios;
    @Schema(description = "Limite de usuários por condomínio.", example = "200")
    private Integer maxUsuariosPorCondominio;
    @Schema(description = "Módulos que o condomínio pode usar.", example = "[\"portaria\", \"reunioes\", \"financeiro\"]")
    private List<String> modulosAtivos;

    /**
     * Anotacao livre do contrato — numero, condicoes negociadas.
     *
     * Era gravada e nunca devolvida: quem cadastrava nunca mais via o que
     * escreveu, e a tela nao tinha como exibir o historico completo.
     */
    @Schema(description = "Anotação livre.", example = "Contrato anual assinado em dezembro.")
    private String observacao;
}
