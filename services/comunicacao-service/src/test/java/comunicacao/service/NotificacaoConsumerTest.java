package comunicacao.service;

import comunicacao.model.Notificacao;
import comunicacao.model.enums.TipoNotificacao;
import comunicacao.repository.NotificacaoRepository;
import comunicacao.service.NotificacaoService.Publicacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificacaoConsumerTest {

    private NotificacaoRepository repositorio;
    private NotificacaoConsumer consumidor;

    @BeforeEach
    void preparar() {
        repositorio = mock(NotificacaoRepository.class);
        when(repositorio.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        consumidor = new NotificacaoConsumer(new NotificacaoService(repositorio));
    }

    private static Publicacao respondida() {
        return new Publicacao("cond-a", "4", "ocorrencias", "OCORRENCIA_RESPONDIDA",
                "Ocorrência REC-1 respondida", "Vamos verificar amanhã.",
                Map.of("reclamacaoId", 12, "protocolo", "REC-1"), "ocorrencias:reclamacao:12:interacao:3");
    }

    @Test
    void gravaANotificacaoDaFilaComAReferenciaDaOcorrencia() {
        consumidor.receber(respondida());

        ArgumentCaptor<Notificacao> gravada = ArgumentCaptor.forClass(Notificacao.class);
        verify(repositorio).saveAndFlush(gravada.capture());
        assertEquals(TipoNotificacao.OCORRENCIA_RESPONDIDA, gravada.getValue().getTipo());
        assertEquals("ocorrencias", gravada.getValue().getOrigem());
        assertEquals("12", gravada.getValue().getReferenciaId());
    }

    @Test
    void entregaSimultaneaDaMesmaChaveNaoEErro() {
        when(repositorio.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("uq"));
        assertDoesNotThrow(() -> consumidor.receber(respondida()));
    }

    @Test
    void mensagemMalformadaEDescartadaSemVoltarParaAFila() {
        Publicacao semDestinatario = new Publicacao("cond-a", null, "ocorrencias", "OCORRENCIA_RESPONDIDA",
                "t", "m", null, null);
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> consumidor.receber(semDestinatario));
    }
}
