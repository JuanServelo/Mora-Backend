package portaria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import portaria.model.enums.TipoVaga;

import java.util.UUID;

/**
 * DTO de entrada para criação e atualização de vagas.
 * Impede que campos gerenciados pelo sistema (id, ativa, criadoEm, atualizadoEm)
 * sejam enviados pelo cliente.
 */
public record VagaRequestDTO(

        @NotBlank(message = "Número da vaga é obrigatório")
        String numero,

        String localizacao,

        @NotNull(message = "Tipo da vaga é obrigatório (COBERTA, DESCOBERTA, MOTO, DEFICIENTE)")
        TipoVaga tipo,

        @NotNull(message = "Vaga deve estar associada a um apartamento")
        UUID apartamentoId
) {}
