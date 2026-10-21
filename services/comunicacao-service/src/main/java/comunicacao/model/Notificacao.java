package comunicacao.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import comunicacao.model.enums.TipoNotificacao;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Entity
@Table(name = "notificacoes")
public class Notificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Schema(description = "Id da notificação.", example = "5b9e2f1c-7d4a-4c8e-a3b6-9f0d1e2c7a45")
    private String id;

    @NotNull

    /**
     * O id de quem, como texto.
     *
     * Não é UUID: o `auth-api` numera usuários com `integer` autoincremental, e
     * era isso que o token trazia — `UUID.fromString("32")` derrubava com 500
     * toda rota que precisa saber quem está pedindo. `VARCHAR` aceita o formato
     * de hoje e sobrevive a uma eventual migração para UUID sem nova alteração
     * de esquema; o preço é não haver tipo forte no banco.
     */
    @Column(name = "destinatario_id")
    @Schema(description = "Id de quem recebe (auth-api).", example = "4")
    private String destinatarioId;

    @Column(name = "`condominioId`")
    @Schema(description = "Condomínio do destinatário.", example = "cond-parque-verde")
    private String condominioId;

    @Enumerated(EnumType.STRING)
    @Schema(description = "Tipo: AVISO, ENTREGA, RESERVA, CHAT, SISTEMA, NOVA_FATURA, FATURA_VENCIDA, PAGAMENTO_CONFIRMADO, MULTA_APLICADA, MULTA_JULGADA...", example = "MULTA_APLICADA")
    private TipoNotificacao tipo;

    @NotBlank
    @Schema(description = "Título mostrado no sino.", example = "Multa aplicada à sua unidade")
    private String titulo;

    @Column(columnDefinition = "TEXT")
    @Schema(description = "Texto da notificação.", example = "Barulho após as 22h em dia de semana — R$ 150,00. Você pode recorrer até 28/10.")
    private String mensagem;

    @Column(name = "referencia_id")
    @Schema(description = "Id do que originou (multa, fatura, conversa), para o link da tela.", example = "9c1f4e2a-6b7d-4f3a-a2c8-51e0d7b3f6a9")
    private String referenciaId;

    @Schema(description = "Se já foi lida.", example = "false")
    private boolean lida = false;

    @Column(name = "criado_em")
    @Schema(description = "Quando foi criada.", example = "2026-10-13T14:20:05")
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "lida_em")
    @Schema(description = "Quando foi lida.", example = "2026-10-13T18:02:41")
    private LocalDateTime lidaEm;

    /**
     * Quem publicou, quando veio de outro serviço (`financeiro`, ...).
     * Nulo para o que nasce aqui mesmo.
     */
    @Column(length = 30)
    @Schema(description = "Serviço que publicou; nulo para o que nasce no comunicacao.", example = "financeiro")
    private String origem;

    /**
     * Identifica o evento de origem, para não notificar duas vezes.
     *
     * O fechamento do mês pode rodar de novo, e a mesma fatura vencida pode ser
     * avisada pelo webhook e pelo job diário. O índice único é **parcial**
     * (`WHERE chave_unica IS NOT NULL`, na `fase8`): quem publica sem chave não
     * colide com todos os outros que também não têm.
     */
    @Column(name = "chave_unica", length = 200)
    @Schema(description = "Identifica o evento de origem, para não notificar duas vezes.", example = "financeiro:MULTA_APLICADA:9c1f4e2a-6b7d-4f3a-a2c8-51e0d7b3f6a9")
    private String chaveUnica;

    /** O que o publicador quis anexar (id da fatura, da multa...), como JSONB. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Schema(description = "O que o publicador anexou.", example = "{\"multaId\": \"9c1f4e2a-6b7d-4f3a-a2c8-51e0d7b3f6a9\", \"valorCentavos\": 15000}")
    private Map<String, Object> dados;
}
