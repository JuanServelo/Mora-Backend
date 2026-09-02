package com.mora.plan.dto;

import com.mora.plan.enums.StatusAssinatura;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Devolve a assinatura com os limites do plano já resolvidos, para que o
 * consumidor não precise de uma segunda chamada só para saber o que pode fazer.
 */
@Data
@Builder
public class AssinaturaResponseDTO {
    private Long id;
    private String condominioId;
    private StatusAssinatura status;
    private LocalDate vigenciaInicio;
    private LocalDate vigenciaFim;
    private boolean vigente;

    private Long planId;
    private String planNome;
    private BigDecimal mensalidade;
    private Integer maxCondominios;
    private Integer maxUsuariosPorCondominio;
    private List<String> modulosAtivos;
}
