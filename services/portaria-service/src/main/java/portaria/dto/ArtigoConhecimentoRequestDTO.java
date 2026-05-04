package portaria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import portaria.model.enums.CategoriaArtigo;

@Data
public class ArtigoConhecimentoRequestDTO {

    @NotBlank(message = "Título é obrigatório")
    private String titulo;

    @NotBlank(message = "Conteúdo é obrigatório")
    private String conteudo;

    @NotNull(message = "Categoria é obrigatória")
    private CategoriaArtigo categoria;

    private String autor;

    private boolean publicado;
}
