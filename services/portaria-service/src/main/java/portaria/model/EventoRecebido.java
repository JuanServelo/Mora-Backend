package portaria.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Inbox de eventos de mensageria consumidos pelo Banco MORA.
 *
 * Serve a dois propósitos:
 *  - Idempotência: o {@code eventId} é a chave primária, então um evento
 *    reentregue pelo broker não é processado duas vezes.
 *  - Auditoria: guarda o payload bruto e o momento de recebimento.
 */
@Data
@Entity
@Table(name = "mensageria_evento_recebido")
public class EventoRecebido {

    @Id
    @Column(name = "event_id")
    private String eventId;

    @Column(nullable = false)
    private String tipo;

    @Column(columnDefinition = "text")
    private String payload;

    @Column(name = "publicado_em")
    private String publicadoEm;

    @Column(name = "recebido_em", nullable = false)
    private LocalDateTime recebidoEm = LocalDateTime.now();

    @Column(nullable = false)
    private boolean processado = false;
}
