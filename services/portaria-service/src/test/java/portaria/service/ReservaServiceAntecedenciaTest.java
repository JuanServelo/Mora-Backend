package portaria.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import portaria.client.AuthApiClient;
import portaria.dto.reserva.ReservaRequestDTO;
import portaria.exception.OperacaoInvalidaException;
import portaria.model.AreaComum;
import portaria.model.Reserva;
import portaria.model.enums.ModoFuncionamento;
import portaria.model.enums.StatusReserva;
import portaria.model.enums.TipoReserva;
import portaria.repository.ReservaAuditoriaRepository;
import portaria.repository.ReservaRepository;
import portaria.security.AuthContext;
import portaria.security.JwtClaims;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RF-10: antecedência mínima para reservar.
 *
 * A regra tem duas metades que só fazem sentido juntas — barrar o morador de
 * última hora e NÃO barrar quem administra. Testar só a primeira deixaria
 * passar a versão que impede o síndico de encaixar um evento urgente, que é o
 * erro mais provável de alguém cometer ao "endurecer" a validação depois.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReservaServiceAntecedenciaTest {

    @Mock private ReservaRepository reservaRepository;
    @Mock private AreaComunService areaComunService;
    @Mock private FuncionamentoAreaComumService funcionamentoService;
    @Mock private AuthApiClient authApiClient;
    @Mock private ReservaAuditoriaRepository auditoriaRepository;

    @InjectMocks private ReservaService reservaService;

    private AreaComum area;

    @BeforeEach
    void preparar() {
        area = new AreaComum();
        area.setId("area-1");
        area.setNome("Churrasqueira");
        area.setCondominioId("cond-a");
        area.setAtivo(true);
        area.setPodeReservar(true);
        area.setModoFuncionamento(ModoFuncionamento.SEM_RESTRICAO);
        area.setAntecedenciaMinimaHoras(48);

        when(areaComunService.buscarPorId("area-1")).thenReturn(area);
        when(reservaRepository.findConflitantes(any(), any(), any(), any())).thenReturn(List.of());
        when(reservaRepository.findPendentesVencidas(any(), any())).thenReturn(List.of());
        when(reservaRepository.save(any(Reserva.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // O responsável é validado contra o auth-api; aqui ele existe e está ativo.
        var eu = new AuthApiClient.PessoaUnidade("7", "Fulano", "p@m.com", "MORADOR");
        when(authApiClient.pessoasDaMinhaUnidade()).thenReturn(List.of(eu));
        when(authApiClient.pessoasDaUnidade(anyString())).thenReturn(List.of(eu));
        when(authApiClient.funcionarios()).thenReturn(List.of(eu));
    }

    @AfterEach
    void limpar() {
        AuthContext.clear();
    }

    private static JwtClaims claims(String perfil) {
        return new JwtClaims("7", "p@m.com", perfil, "cond-a", "unid-1", "Fulano");
    }

    private ReservaRequestDTO pedido(LocalDateTime inicio, LocalDateTime fim) {
        return new ReservaRequestDTO("area-1", inicio, fim, TipoReserva.MORADOR,
                "unid-1", "7", null, null);
    }

    @Test
    @DisplayName("morador pedindo com menos antecedência que a exigida é recusado")
    void moradorAbaixoDoMinimo() {
        AuthContext.set(claims("MORADOR"));
        LocalDateTime inicio = LocalDateTime.now().plusHours(10);

        var e = assertThrows(OperacaoInvalidaException.class,
                () -> reservaService.solicitar(claims("MORADOR"), pedido(inicio, inicio.plusHours(2))));

        assertTrue(e.getMessage().contains("Churrasqueira"),
                "a mensagem precisa nomear o espaço: " + e.getMessage());
        assertTrue(e.getMessage().contains("2 dias"),
                "48 horas deve ser lido como '2 dias': " + e.getMessage());
    }

    @Test
    @DisplayName("morador pedindo além do mínimo passa")
    void moradorAcimaDoMinimo() {
        AuthContext.set(claims("MORADOR"));
        LocalDateTime inicio = LocalDateTime.now().plusDays(4);

        Reserva r = reservaService.solicitar(claims("MORADOR"), pedido(inicio, inicio.plusHours(2)));
        assertNotNull(r);
        assertEquals(StatusReserva.APROVADA, r.getStatus());
    }

    @Test
    @DisplayName("síndico não é barrado: evento de última hora precisa caber")
    void sindicoNaoEBarrado() {
        AuthContext.set(claims("ADMIN_SINDICO"));
        LocalDateTime inicio = LocalDateTime.now().plusHours(10);

        assertDoesNotThrow(() ->
                reservaService.solicitar(claims("ADMIN_SINDICO"), pedido(inicio, inicio.plusHours(2))));
    }

    @Test
    @DisplayName("antecedência zero desliga a regra — é o estado de todo espaço antigo")
    void zeroDesliga() {
        area.setAntecedenciaMinimaHoras(0);
        AuthContext.set(claims("MORADOR"));
        LocalDateTime inicio = LocalDateTime.now().plusMinutes(30);

        assertDoesNotThrow(() ->
                reservaService.solicitar(claims("MORADOR"), pedido(inicio, inicio.plusHours(1))));
    }

    @Test
    @DisplayName("exigência em horas quebradas é lida em horas, não em dias")
    void horasQuebradas() {
        area.setAntecedenciaMinimaHoras(5);
        AuthContext.set(claims("MORADOR"));
        LocalDateTime inicio = LocalDateTime.now().plusHours(1);

        var e = assertThrows(OperacaoInvalidaException.class,
                () -> reservaService.solicitar(claims("MORADOR"), pedido(inicio, inicio.plusHours(2))));
        assertTrue(e.getMessage().contains("5 horas"), e.getMessage());
    }

    @Test
    @DisplayName("data no passado continua recusada, antes mesmo da antecedência")
    void passadoRecusado() {
        AuthContext.set(claims("MORADOR"));
        LocalDateTime inicio = LocalDateTime.now().minusHours(1);

        var e = assertThrows(OperacaoInvalidaException.class,
                () -> reservaService.solicitar(claims("MORADOR"), pedido(inicio, inicio.plusHours(2))));
        assertTrue(e.getMessage().toLowerCase().contains("passad"), e.getMessage());
    }

    @Test
    @DisplayName("prazoDecisao: o que vier primeiro entre 24h da criação e o início")
    void prazoDecisao() {
        Reserva r = new Reserva();
        LocalDateTime criacao = LocalDateTime.of(2026, 10, 1, 10, 0);
        r.setCriadoEm(criacao);

        // Início distante: vence o prazo de resposta.
        r.setInicio(criacao.plusDays(10));
        assertEquals(criacao.plus(ReservaService.PRAZO_APROVACAO), ReservaService.prazoDecisao(r));

        // Início próximo: vence o início, porque não se aprova o que já começou.
        r.setInicio(criacao.plusHours(3));
        assertEquals(criacao.plusHours(3), ReservaService.prazoDecisao(r));
    }

    @Test
    @DisplayName("podeAprovar: só síndico e admin geral")
    void podeAprovar() {
        assertTrue(ReservaService.podeAprovar("ADMIN_SINDICO"));
        assertTrue(ReservaService.podeAprovar("ADMIN_GERAL"));
        assertFalse(ReservaService.podeAprovar("PORTEIRO"),
                "porteiro registra reserva, mas não decide a dos outros");
        assertFalse(ReservaService.podeAprovar("MORADOR"));
        assertFalse(ReservaService.podeAprovar(null));
    }
}
