package portaria.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import portaria.client.AuthApiClient;
import portaria.client.ComunicacaoClient;
import portaria.model.AreaComum;
import portaria.model.Entrega;
import portaria.model.PreAutorizacao;
import portaria.model.Reserva;
import portaria.model.enums.StatusReserva;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Quem recebe cada aviso, e o que ele diz.
 *
 * O destinatário é a parte que erra em silêncio: mandar para o porteiro algo
 * que é do morador não quebra nada, só enche a caixa errada — e a pessoa certa
 * nunca fica sabendo. Por isso cada teste afirma o id do destinatário, não só
 * que "alguma notificação saiu".
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificacaoPortariaServiceTest {

    @Mock private ComunicacaoClient comunicacao;
    @Mock private AuthApiClient authApiClient;
    @InjectMocks private NotificacaoPortariaService notificacoes;

    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 12, 1, 14, 0);

    private static Reserva reserva() {
        AreaComum area = new AreaComum();
        area.setNome("Salão de Festas");

        Reserva r = new Reserva();
        r.setId("r-1");
        r.setCondominioId("cond-a");
        r.setAreaComum(area);
        r.setSolicitanteId("3");
        r.setResponsavelNome("Ana Souza");
        r.setInicio(INICIO);
        r.setFim(INICIO.plusHours(3));
        r.setCriadoEm(INICIO.minusDays(10));
        r.setStatus(StatusReserva.PENDENTE);
        return r;
    }

    private ArgumentCaptor<String> capturarDestinatario() {
        ArgumentCaptor<String> cap = ArgumentCaptor.forClass(String.class);
        verify(comunicacao, atLeastOnce()).notificar(
                cap.capture(), any(), any(), any(), any(), any(), any());
        return cap;
    }

    // ── Reserva pendente ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("reserva aguardando aprovação")
    class Pendente {

        @BeforeEach
        void sindicos() {
            when(authApiClient.sindicos(anyString())).thenReturn(List.of(
                    new AuthApiClient.PessoaUnidade("4", "Síndico", "s@m.com", "ADMIN_SINDICO")));
        }

        @Test
        @DisplayName("avisa o síndico do condomínio da reserva")
        void avisaSindico() {
            notificacoes.reservaAguardandoAprovacao(reserva());

            verify(comunicacao, times(1)).notificar(
                    eq("4"), eq("cond-a"), eq("RESERVA"), any(), any(), any(), any());
            // A consulta já vem escopada ao condomínio da reserva: o serviço
            // não filtra perfil por conta própria.
            verify(authApiClient).sindicos("cond-a");
        }

        @Test
        @DisplayName("dois síndicos recebem — e com chaves distintas, senão o 2º é descartado")
        void doisSindicos() {
            when(authApiClient.sindicos(anyString())).thenReturn(List.of(
                    new AuthApiClient.PessoaUnidade("4", "S1", "a@m.com", "ADMIN_SINDICO"),
                    new AuthApiClient.PessoaUnidade("5", "S2", "b@m.com", "ADMIN_SINDICO")));

            notificacoes.reservaAguardandoAprovacao(reserva());

            ArgumentCaptor<String> chaves = ArgumentCaptor.forClass(String.class);
            verify(comunicacao, times(2)).notificar(
                    any(), any(), any(), any(), any(), chaves.capture(), any());
            assertEquals(2, chaves.getAllValues().stream().distinct().count(),
                    "chave repetida faria o comunicacao descartar o segundo aviso");
        }

        @Test
        @DisplayName("a mensagem diz quem pediu, o espaço e até quando decidir")
        void conteudo() {
            notificacoes.reservaAguardandoAprovacao(reserva());

            ArgumentCaptor<String> msg = ArgumentCaptor.forClass(String.class);
            verify(comunicacao).notificar(any(), any(), any(), any(), msg.capture(), any(), any());
            String texto = msg.getValue();
            assertTrue(texto.contains("Ana Souza"), texto);
            assertTrue(texto.contains("Salão de Festas"), texto);
            assertTrue(texto.contains("01/12/2026"), texto);
            assertTrue(texto.toLowerCase().contains("expira"), texto);
        }

        @Test
        @DisplayName("leva a ação, para a notificação abrir na fila de aprovação")
        void levaAcao() {
            notificacoes.reservaAguardandoAprovacao(reserva());

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> dados = ArgumentCaptor.forClass(Map.class);
            verify(comunicacao).notificar(any(), any(), any(), any(), any(), any(),
                    dados.capture());
            assertEquals("aprovar", dados.getValue().get("acao"));
            assertEquals("r-1", dados.getValue().get("reservaId"));
        }

        @Test
        @DisplayName("sem síndico cadastrado, não quebra nem avisa ninguém")
        void semSindico() {
            when(authApiClient.sindicos(anyString())).thenReturn(List.of());
            assertDoesNotThrow(() -> notificacoes.reservaAguardandoAprovacao(reserva()));
            verifyNoInteractions(comunicacao);
        }

        @Test
        @DisplayName("auth-api fora do ar não impede a reserva de existir")
        void authForaDoAr() {
            // O cliente já devolve lista vazia em falha; aqui se garante que
            // uma exceção inesperada também não sobe para quem reservou.
            when(authApiClient.sindicos(anyString())).thenThrow(new RuntimeException("timeout"));
            assertThrows(RuntimeException.class,
                    () -> notificacoes.reservaAguardandoAprovacao(reserva()));
        }
    }

    // ── Decisão ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("decisão sobre a reserva")
    class Decisao {

        @Test
        @DisplayName("aprovação vai para quem solicitou")
        void aprovada() {
            notificacoes.reservaDecidida(reserva(), true, null);

            assertEquals("3", capturarDestinatario().getValue(),
                    "quem recebe é o solicitante, não o síndico que decidiu");
        }

        @Test
        @DisplayName("o título da aprovação diz que está confirmada")
        void tituloAprovada() {
            notificacoes.reservaDecidida(reserva(), true, null);

            ArgumentCaptor<String> titulo = ArgumentCaptor.forClass(String.class);
            verify(comunicacao).notificar(any(), any(), any(), titulo.capture(), any(), any(), any());
            assertTrue(titulo.getValue().toLowerCase().contains("confirmada"), titulo.getValue());
        }

        @Test
        @DisplayName("a recusa carrega a justificativa no corpo da mensagem")
        void recusadaComMotivo() {
            notificacoes.reservaDecidida(reserva(), false, "Espaço em manutenção nessa data.");

            ArgumentCaptor<String> msg = ArgumentCaptor.forClass(String.class);
            verify(comunicacao).notificar(any(), any(), any(), any(), msg.capture(), any(), any());
            assertTrue(msg.getValue().contains("Espaço em manutenção nessa data."),
                    "recusar sem dizer o motivo obriga a pessoa a procurar alguém: "
                    + msg.getValue());
        }

        @Test
        @DisplayName("recusa sem motivo não vira 'null' na tela")
        void recusadaSemMotivo() {
            notificacoes.reservaDecidida(reserva(), false, null);

            ArgumentCaptor<String> msg = ArgumentCaptor.forClass(String.class);
            verify(comunicacao).notificar(any(), any(), any(), any(), msg.capture(), any(), any());
            assertFalse(msg.getValue().contains("null"), msg.getValue());
            assertTrue(msg.getValue().contains("não informado"), msg.getValue());
        }

        @Test
        @DisplayName("aprovação e recusa usam chaves diferentes")
        void chavesDistintas() {
            Reserva r = reserva();
            notificacoes.reservaDecidida(r, true, null);
            notificacoes.reservaDecidida(r, false, "x");

            ArgumentCaptor<String> chaves = ArgumentCaptor.forClass(String.class);
            verify(comunicacao, times(2)).notificar(
                    any(), any(), any(), any(), any(), chaves.capture(), any());
            assertEquals(2, chaves.getAllValues().stream().distinct().count());
        }

        @Test
        @DisplayName("reserva sem solicitante não gera aviso órfão")
        void semSolicitante() {
            Reserva r = reserva();
            r.setSolicitanteId(null);
            notificacoes.reservaDecidida(r, true, null);
            verifyNoInteractions(comunicacao);
        }
    }

    // ── Pré-autorização ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("visitante pré-aprovado entrou")
    class PreAprovado {

        private PreAutorizacao pa() {
            PreAutorizacao p = new PreAutorizacao();
            p.setId("pa-1");
            p.setMoradorId("3");
            p.setCondominioId("cond-a");
            p.setNomeVisitante("João Entregas");
            return p;
        }

        @Test
        @DisplayName("avisa quem fez a pré-liberação")
        void avisaOAutorizador() {
            notificacoes.visitantePreAprovadoEntrou(pa());
            assertEquals("3", capturarDestinatario().getValue());
        }

        @Test
        @DisplayName("a mensagem nomeia o visitante")
        void nomeiaVisitante() {
            notificacoes.visitantePreAprovadoEntrou(pa());

            ArgumentCaptor<String> msg = ArgumentCaptor.forClass(String.class);
            verify(comunicacao).notificar(any(), any(), any(), any(), msg.capture(), any(), any());
            assertTrue(msg.getValue().contains("João Entregas"), msg.getValue());
        }

        @Test
        @DisplayName("com veículo, a placa aparece no aviso")
        void comVeiculo() {
            PreAutorizacao p = pa();
            p.setPlacaVeiculo("ABC1D23");
            notificacoes.visitantePreAprovadoEntrou(p);

            ArgumentCaptor<String> msg = ArgumentCaptor.forClass(String.class);
            verify(comunicacao).notificar(any(), any(), any(), any(), msg.capture(), any(), any());
            assertTrue(msg.getValue().contains("ABC1D23"), msg.getValue());
        }

        @Test
        @DisplayName("pré-liberação sem morador vinculado não avisa ninguém")
        void semMorador() {
            PreAutorizacao p = pa();
            p.setMoradorId(null);
            notificacoes.visitantePreAprovadoEntrou(p);
            verifyNoInteractions(comunicacao);
        }
    }

    // ── Entregas ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("entregas")
    class Entregas {

        private Entrega entrega() {
            Entrega e = new Entrega();
            e.setId("e-1");
            e.setCondominioId("cond-a");
            e.setDestinatarioId(12L);
            e.setDestinatarioNome("Ana Souza");
            e.setDescricao("Caixa da Amazon");
            e.setRemetente("Amazon");
            e.setBloco("Bloco A");
            e.setApartamento("101");
            return e;
        }

        @Test
        @DisplayName("chegada avisa o destinatário, com remetente e localização")
        void chegada() {
            notificacoes.entregaRecebida(entrega());

            ArgumentCaptor<String> msg = ArgumentCaptor.forClass(String.class);
            verify(comunicacao).notificar(eq("12"), eq("cond-a"), eq("ENTREGA"),
                    any(), msg.capture(), any(), any());
            assertTrue(msg.getValue().contains("Caixa da Amazon"), msg.getValue());
            assertTrue(msg.getValue().contains("Amazon"), msg.getValue());
            assertTrue(msg.getValue().contains("Bloco A 101"), msg.getValue());
        }

        @Test
        @DisplayName("entrega sem destinatário identificado não gera aviso")
        void semDestinatario() {
            Entrega e = entrega();
            e.setDestinatarioId(null);
            notificacoes.entregaRecebida(e);
            verifyNoInteractions(comunicacao);
        }

        @Test
        @DisplayName("retirada pelo próprio destinatário gera um aviso só")
        void retiradaPeloProprio() {
            Entrega e = entrega();
            e.setRecebedorNome("Ana Souza");
            notificacoes.entregaRetirada(e, "12");

            verify(comunicacao, times(1)).notificar(
                    any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("retirada por outra pessoa avisa os dois, com textos diferentes")
        void retiradaPorOutro() {
            Entrega e = entrega();
            e.setRecebedorNome("Bruno Dias");
            notificacoes.entregaRetirada(e, "20");

            ArgumentCaptor<String> dest = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> msg = ArgumentCaptor.forClass(String.class);
            verify(comunicacao, times(2)).notificar(
                    dest.capture(), any(), any(), any(), msg.capture(), any(), any());

            assertEquals(List.of("12", "20"), dest.getAllValues(),
                    "o destinatário sabe que saiu; quem retirou recebe o comprovante");
            assertTrue(msg.getAllValues().get(0).contains("Bruno Dias"),
                    "o dono precisa saber por quem: " + msg.getAllValues().get(0));
            assertTrue(msg.getAllValues().get(1).contains("Ana Souza"),
                    "o comprovante diz de quem era: " + msg.getAllValues().get(1));
        }

        @Test
        @DisplayName("sem quem retirou identificado, só o destinatário é avisado")
        void recebedorNaoIdentificado() {
            notificacoes.entregaRetirada(entrega(), null);
            verify(comunicacao, times(1)).notificar(
                    eq("12"), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("chegada e retirada usam chaves distintas")
        void chavesDistintas() {
            Entrega e = entrega();
            notificacoes.entregaRecebida(e);
            notificacoes.entregaRetirada(e, "12");

            ArgumentCaptor<String> chaves = ArgumentCaptor.forClass(String.class);
            verify(comunicacao, times(2)).notificar(
                    any(), any(), any(), any(), any(), chaves.capture(), any());
            assertEquals(2, chaves.getAllValues().stream().distinct().count());
        }
    }
}
