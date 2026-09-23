package comunicacao.model;

import comunicacao.model.enums.TipoConversa;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "conversas")
public class Conversa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "condominio_id", nullable = false, length = 50)
    private String condominioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoConversa tipo;

    @Column(nullable = false, length = 150)
    private String assunto;

    /** Id do autor, como texto — o auth-api numera usuários com `integer`. */
    @Column(name = "criada_por", nullable = false, length = 64)
    private String criadaPor;

    /**
     * Denormalizado de propósito.
     *
     * A lista de conversas ordena por este campo. Sem a coluna, toda listagem
     * viraria um JOIN com MAX() sobre `mensagens` — e a caixa da gestão é a
     * tela que mais abre.
     */
    @Column(name = "ultima_mensagem_em")
    private LocalDateTime ultimaMensagemEm;

    @Column(name = "encerrada_em")
    private LocalDateTime encerradaEm;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm = LocalDateTime.now();
}
