package comunicacao.service;

import comunicacao.exception.DadosInvalidosException;
import comunicacao.model.Notificacao;
import comunicacao.model.enums.TipoNotificacao;
import comunicacao.repository.NotificacaoRepository;
import comunicacao.service.NotificacaoService.Publicacao;
import comunicacao.service.NotificacaoService.Resultado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificacaoServiceTest {

    private NotificacaoRepository repositorio;
    private NotificacaoService servico;

    @BeforeEach
    void preparar() {
        repositorio = mock(NotificacaoRepository.class);
        when(repositorio.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        servico = new NotificacaoService(repositorio);
    }

    private static Publicacao fatura(String chave) {
        return new Publicacao("cond-a", "4", "financeiro", "NOVA_FATURA",
                "Nova cobrança", "Sua fatura está disponível", Map.of("faturaId", "f-1"), chave);
    }

    @Test
    void gravaAPublicacaoComOrigemDadosEReferencia() {
        Resultado r = servico.publicarDeServico(fatura("financeiro:NOVA_FATURA:f-1"));

        assertFalse(r.repetida());
        ArgumentCaptor<Notificacao> gravada = ArgumentCaptor.forClass(Notificacao.class);
        verify(repositorio).saveAndFlush(gravada.capture());
        Notificacao n = gravada.getValue();
        assertEquals(TipoNotificacao.NOVA_FATURA, n.getTipo());
        assertEquals("financeiro", n.getOrigem());
        assertEquals("f-1", n.getReferenciaId());
        assertEquals("4", n.getDestinatarioId());
    }

    @Test
    void mesmaChaveDevolveARepetidaSemGravarDeNovo() {
        Notificacao existente = new Notificacao();
        when(repositorio.findByChaveUnica("financeiro:NOVA_FATURA:f-1")).thenReturn(Optional.of(existente));

        Resultado r = servico.publicarDeServico(fatura("financeiro:NOVA_FATURA:f-1"));

        assertTrue(r.repetida());
        assertSame(existente, r.notificacao());
        verify(repositorio, never()).saveAndFlush(any());
    }

    @Test
    void semChaveSempreGrava() {
        servico.publicarDeServico(fatura(null));
        servico.publicarDeServico(fatura("  "));
        verify(repositorio, never()).findByChaveUnica(any());
        verify(repositorio, times(2)).saveAndFlush(any());
    }

    @Test
    void recusaTipoDesconhecidoEFaltaDeDestinatario() {
        Publicacao tipoErrado = new Publicacao("cond-a", "4", "financeiro", "QUALQUER",
                "t", "m", null, null);
        assertThrows(DadosInvalidosException.class, () -> servico.publicarDeServico(tipoErrado));

        Publicacao semUsuario = new Publicacao("cond-a", null, "financeiro", "NOVA_FATURA",
                "t", "m", null, null);
        assertThrows(DadosInvalidosException.class, () -> servico.publicarDeServico(semUsuario));
    }

    @Test
    void semMensagemUsaOTitulo() {
        servico.publicarDeServico(new Publicacao("cond-a", "4", null, "MULTA_APLICADA",
                "Multa aplicada", null, null, null));
        ArgumentCaptor<Notificacao> gravada = ArgumentCaptor.forClass(Notificacao.class);
        verify(repositorio).saveAndFlush(gravada.capture());
        assertEquals("Multa aplicada", gravada.getValue().getMensagem());
    }
}
