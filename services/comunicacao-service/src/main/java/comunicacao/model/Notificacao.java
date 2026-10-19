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

@Data
@Entity
@Table(name = "notificacoes")
public class Notificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
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
    private String destinatarioId;

    @Column(name = "`condominioId`")
    private String condominioId;

    @Enumerated(EnumType.STRING)
    private TipoNotificacao tipo;

    @NotBlank
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String mensagem;

    @Column(name = "referencia_id")
    private String referenciaId;

    private boolean lida = false;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "lida_em")
    private LocalDateTime lidaEm;

    /**
     * Quem publicou, quando veio de outro serviço (`financeiro`, ...).
     * Nulo para o que nasce aqui mesmo.
     */
    @Column(length = 30)
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
    private String chaveUnica;

    /** O que o publicador quis anexar (id da fatura, da multa...), como JSONB. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> dados;
}
