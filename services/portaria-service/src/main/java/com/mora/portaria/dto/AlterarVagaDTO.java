package com.mora.portaria.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlterarVagaDTO {
    /** ID da nova vaga. Null para desvincular (apenas VEICULO_SERVICO pode ficar sem vaga). */
    private String vagaId;
}

