package comunicacao.service;

import comunicacao.clients.AuthClient;
import comunicacao.clients.AuthClient.Usuario;
import comunicacao.exception.AcessoNegadoException;
import comunicacao.model.Aviso;
import comunicacao.model.enums.PublicoAlvo;
import comunicacao.security.AuthContext;
import comunicacao.security.JwtClaims;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RelatorioLeituraServiceTest {

    private final UUID avisoId = UUID.randomUUID();
    private AvisoService avisoService;
    private AvisoLeituraService leituraService;
    private AuthClient authClient;
    private RelatorioLeituraService servico;

    @BeforeEach
    void preparar() {
        avisoService = mock(AvisoService.class);
        leituraService = mock(AvisoLeituraService.class);
        authClient = mock(AuthClient.class);
        servico = new RelatorioLeituraService(avisoService, leituraService, authClient);

        Aviso aviso = new Aviso();
        aviso.setId(avisoId);
        aviso.setCondominioId("cond-a");
        aviso.setPublicoAlvo(PublicoAlvo.MORADORES);
        when(avisoService.buscarPorId(avisoId)).thenReturn(aviso);
        when(leituraService.leiturasDoAviso(avisoId)).thenReturn(Map.of("1", LocalDateTime.now()));

        AuthContext.set(new JwtClaims("99", "sindico@x", "ADMIN_SINDICO", "cond-a", "Síndico"));
    }

    @AfterEach
    void limpar() {
        AuthContext.clear();
    }

    private static Usuario usuario(String id, String perfil, boolean alcancavel) {
        return new Usuario(id, "Pessoa " + id, id + "@x", perfil, null, "u", alcancavel);
    }

    @Test
    void denominadorSegueOPublicoDoAviso() {
        when(authClient.listarUsuariosDoCondominio(eq("cond-a"), any())).thenReturn(List.of(
                usuario("1", "MORADOR", true),
                usuario("2", "DONO_ALUGUEL", true),
                usuario("3", "PORTEIRO", true),       // fora: aviso é para moradores
                usuario("4", "ADMIN_SINDICO", true),  // fora: a gestão publica
                usuario("5", "MORADOR", false)));     // fora: sem acesso ao sistema

        Map<String, Object> r = servico.relatorio(avisoId, "Bearer t");

        assertEquals(2, r.get("total"));
        assertEquals(1L, r.get("confirmados"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> destinatarios = (List<Map<String, Object>>) r.get("destinatarios");
        assertEquals(false, destinatarios.get(0).get("lido"), "quem falta ler vem primeiro");
    }

    @Test
    void semAuthApiOTotalSaiNuloEmVezDeInventado() {
        when(authClient.listarUsuariosDoCondominio(any(), any())).thenReturn(null);

        Map<String, Object> r = servico.relatorio(avisoId, "Bearer t");

        assertNull(r.get("total"));
        assertEquals(1, r.get("confirmados"));
        assertNotNull(r.get("observacao"));
    }

    @Test
    void moradorNaoVeORelatorio() {
        AuthContext.set(new JwtClaims("1", "m@x", "MORADOR", "cond-a", "Morador"));
        assertThrows(AcessoNegadoException.class, () -> servico.relatorio(avisoId, "Bearer t"));
        verifyNoInteractions(authClient);
    }
}
