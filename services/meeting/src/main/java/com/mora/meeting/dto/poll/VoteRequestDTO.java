package com.mora.meeting.dto.poll;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoteRequestDTO {

    @NotNull(message = "O ID da opção escolhida é obrigatório")
    private Long pollOptionId;

    @NotNull(message = "O ID do usuário é obrigatório")
    private Long usuarioId;

    /* * TODO (Integração Auth):
     * Quando a autenticação estiver pronta, remover este campo "usuarioId" daqui.
     * O Front-end não deve mandar quem ele é no corpo do JSON, pois isso pode ser fraudado.
     * O sistema deve extrair o usuário logado diretamente do Token JWT.
     */
}