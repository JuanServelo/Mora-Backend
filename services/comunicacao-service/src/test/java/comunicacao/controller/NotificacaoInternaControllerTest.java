package comunicacao.controller;

import comunicacao.model.Notificacao;
import comunicacao.service.NotificacaoService;
import comunicacao.service.NotificacaoService.Publicacao;
import comunicacao.service.NotificacaoService.Resultado;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificacaoInternaControllerTest {

    private static final Publicacao PUBLICACAO = new Publicacao("cond-a", "4", "financeiro",
            "NOVA_FATURA", "Nova cobrança", null, null, "financeiro:NOVA_FATURA:f-1");

    @Test
    void semTokenConfiguradoARotaFicaFechada() {
        NotificacaoService servico = mock(NotificacaoService.class);
        var controller = new NotificacaoInternaController(servico, "");

        var r = controller.publicar("qualquer", PUBLICACAO);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, r.getStatusCode());
        verifyNoInteractions(servico);
    }

    @Test
    void tokenErradoOuAusenteE401() {
        NotificacaoService servico = mock(NotificacaoService.class);
        var controller = new NotificacaoInternaController(servico, "segredo-dos-servicos");

        assertEquals(HttpStatus.UNAUTHORIZED, controller.publicar("errado", PUBLICACAO).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, controller.publicar(null, PUBLICACAO).getStatusCode());
        verifyNoInteractions(servico);
    }

    @Test
    void novaE201ERepetidaE200() {
        NotificacaoService servico = mock(NotificacaoService.class);
        var controller = new NotificacaoInternaController(servico, "segredo-dos-servicos");

        when(servico.publicarDeServico(any())).thenReturn(new Resultado(new Notificacao(), false));
        assertEquals(HttpStatus.CREATED, controller.publicar("segredo-dos-servicos", PUBLICACAO).getStatusCode());

        when(servico.publicarDeServico(any())).thenReturn(new Resultado(new Notificacao(), true));
        var repetida = controller.publicar("segredo-dos-servicos", PUBLICACAO);
        assertEquals(HttpStatus.OK, repetida.getStatusCode());
        assertEquals(true, repetida.getBody().get("repetida"));
    }

    @Test
    void publicacaoSimultaneaQueEsbarraNoIndiceViraRepetida() {
        NotificacaoService servico = mock(NotificacaoService.class);
        var controller = new NotificacaoInternaController(servico, "segredo-dos-servicos");
        when(servico.publicarDeServico(any())).thenThrow(new DataIntegrityViolationException("uq"));
        when(servico.buscarPorChave("financeiro:NOVA_FATURA:f-1")).thenReturn(new Notificacao());

        var r = controller.publicar("segredo-dos-servicos", PUBLICACAO);

        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertEquals(true, r.getBody().get("repetida"));
    }
}
