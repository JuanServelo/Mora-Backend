package portaria.dto;

import java.time.LocalDate;

public record PreAutorizacaoRequestDTO(
        String nomeVisitante,
        String cpfVisitante,
        String telefoneVisitante,
        /** SERVICO = terceiro (síndico); nulo ou VISITA = visitante (morador). */
        portaria.model.enums.TipoVisita tipoPessoa,
        String empresa,
        String placaVeiculo,
        String modeloVeiculo,
        String corVeiculo,
        LocalDate validadeInicio,
        LocalDate validadeFim,
        String observacoes
) {}
