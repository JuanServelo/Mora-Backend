package comunicacao.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import comunicacao.model.enums.CategoriaArtigo;

import java.time.LocalDateTime;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Entity
@Table(name = "artigos_conhecimento")
public class ArtigoConhecimento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Schema(description = "Id do artigo.", example = "c4d2e8f1-5a7b-4c3d-9e1f-2b6a8d0c4e7f", accessMode = Schema.AccessMode.READ_ONLY)
    private UUID id;

    @NotBlank(message = "Título é obrigatório")
    @Schema(description = "Título do artigo.", example = "Horário de uso do salão de festas")
    private String titulo;

    @NotBlank(message = "Conteúdo é obrigatório")
    @Column(columnDefinition = "TEXT")
    @Schema(description = "Texto do artigo.", example = "O salão pode ser usado das 10h às 22h, com reserva feita pelo app com 48h de antecedência.")
    private String conteudo;

    @NotNull(message = "Categoria é obrigatória")
    @Enumerated(EnumType.STRING)
    @Schema(description = "REGRA, MANUAL, TUTORIAL, ORIENTACAO_CONVIVENCIA ou FAQ.", example = "REGRA")
    private CategoriaArtigo categoria;

    @Schema(description = "Quem escreveu.", example = "sindico.verde@mora.test")
    private String autor;

    @Column(name = "`condominioId`")
    @Schema(description = "Sai do token; o valor enviado é ignorado.", example = "cond-parque-verde", accessMode = Schema.AccessMode.READ_ONLY)
    private String condominioId;

    @Schema(description = "Rascunho (false) só a gestão vê.", example = "true")
    private boolean publicado = false;

    @Column(name = "criado_em")
    @Schema(description = "Quando foi criado.", example = "2026-10-02T14:00:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "atualizado_em")
    @Schema(description = "Última alteração.", example = "2026-10-02T14:00:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime atualizadoEm = LocalDateTime.now();
}
