package comunicacao.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import comunicacao.model.enums.PublicoAlvo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Entity
@Table(name = "avisos")
public class Aviso {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Schema(description = "Id do aviso.", example = "766730a5-0658-475f-89fe-aad79bb287c1", accessMode = Schema.AccessMode.READ_ONLY)
    private UUID id;

    @NotBlank(message = "Título é obrigatório")
    @Schema(description = "Título do comunicado.", example = "Manutenção da caixa d'água")
    private String titulo;

    @NotBlank(message = "Mensagem é obrigatória")
    @Column(columnDefinition = "TEXT")
    @Schema(description = "Texto do comunicado.", example = "Na quinta-feira, das 8h às 12h, o abastecimento será interrompido para a limpeza semestral da caixa d'água.")
    private String mensagem;

    @NotNull(message = "Data inicial é obrigatória")
    @Column(name = "data_inicio")
    @Schema(description = "Primeiro dia em que o aviso aparece.", example = "2026-10-20")
    private LocalDate dataInicio;

    @NotNull(message = "Data final é obrigatória")
    @Column(name = "data_fim")
    @Schema(description = "Último dia em que o aviso aparece.", example = "2026-10-31")
    private LocalDate dataFim;

    @Enumerated(EnumType.STRING)
    @Column(name = "publico_alvo")
    @Schema(description = "Quem recebe: TODOS (moradores, donos e porteiros), MORADORES, FUNCIONARIOS ou SINDICO.", example = "MORADORES")
    private PublicoAlvo publicoAlvo = PublicoAlvo.TODOS;

    @Column(name = "`condominioId`")
    @Schema(description = "Sai do token de quem publica; o valor enviado é ignorado.", example = "cond-parque-verde", accessMode = Schema.AccessMode.READ_ONLY)
    private String condominioId;

    @Schema(description = "Quem publicou. Sem valor, vira o e-mail do token.", example = "sindico.verde@mora.test")
    private String autor;

    // Caminho da imagem do aviso, opcional. O arquivo e servido pelo
    // comunicacao-service, que ja tem a infraestrutura de upload; aqui guarda-se
    // so a referencia, junto do aviso a que ela pertence.
    @Column(name = "imagem_url")
    @Schema(description = "Caminho devolvido por POST /avisos/imagem.", example = "/uploads/avisos/3f2b9c1e-8a4d-4e7f-b6c2-1d9e0a5f7b3c.jpg")
    private String imagemUrl;

    @Schema(description = "Rascunho (false) só a gestão vê.", example = "true")
    private boolean publicado = false;

    @Column(name = "criado_em")
    @Schema(description = "Quando foi criado.", example = "2026-10-18T09:15:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "atualizado_em")
    @Schema(description = "Última alteração.", example = "2026-10-18T09:15:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime atualizadoEm = LocalDateTime.now();
}
