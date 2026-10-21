package portaria.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import portaria.exception.AcessoNegadoException;
import portaria.exception.GlobalExceptionHandler;
import portaria.exception.OperacaoInvalidaException;
import portaria.exception.RecursoNaoEncontradoException;
import portaria.model.Turno;
import portaria.model.Visitante;
import portaria.service.EntregaService;
import portaria.service.TurnoService;
import portaria.service.VisitanteService;
import portaria.security.AuthContext;
import portaria.security.JwtClaims;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controllers da operação diária da portaria.
 *
 * O foco é o contrato HTTP: verbo, status e o que chega ao serviço. Entrada e
 * saída de pessoa são as ações mais repetidas do sistema, e um status errado
 * aqui faz o frontend tratar sucesso como falha — ou pior, o contrário.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PortariaControllersTest {

    private final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());

    private static MockMvc mvc(Object controller) {
        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @BeforeEach
    void autenticar() {
        AuthContext.set(new JwtClaims("2", "p@m.com", "PORTEIRO", "cond-a", null, "Porteiro"));
    }

    @AfterEach
    void limpar() {
        AuthContext.clear();
    }

    // ── Entregas ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("EntregaController")
    class Entregas {

        @Mock private EntregaService service;

        private MockMvc mock() {
            return mvc(new EntregaController(service));
        }

        private static final String CORPO = """
                {"destinatarioId":12,"destinatarioNome":"Ana","descricao":"Caixa",
                 "remetente":"Loja","bloco":"A","apartamento":"101"}""";

        @Test
        @DisplayName("registrar encomenda devolve 201")
        void cadastrar() throws Exception {
            when(service.cadastrar(any())).thenReturn(new portaria.model.Entrega());
            mock().perform(post("/entregas/cadastrar")
                            .contentType("application/json").content(CORPO))
                    .andExpect(status().isCreated());
            verify(service).cadastrar(any());
        }

        @Test
        @DisplayName("morador tentando registrar recebe 403")
        void moradorBarrado() throws Exception {
            when(service.cadastrar(any()))
                    .thenThrow(new AcessoNegadoException("Seu perfil não pode registrar entregas."));
            mock().perform(post("/entregas/cadastrar")
                            .contentType("application/json").content(CORPO))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("listar pendentes é endpoint próprio, separado do listar tudo")
        void pendentes() throws Exception {
            when(service.listarPendentes(anyString())).thenReturn(List.of());
            mock().perform(get("/entregas/pendentes")).andExpect(status().isOk());
            verify(service).listarPendentes(anyString());
            verify(service, never()).listarTodas(anyString());
        }

        @Test
        @DisplayName("entrega inexistente vira 404")
        void naoEncontrada() throws Exception {
            when(service.buscarPorId("x"))
                    .thenThrow(new RecursoNaoEncontradoException("Entrega não encontrada"));
            mock().perform(get("/entregas/x")).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("excluir devolve 204")
        void excluir() throws Exception {
            mock().perform(delete("/entregas/abc")).andExpect(status().isNoContent());
            verify(service).excluir("abc");
        }

        @Test
        @DisplayName("encomenda já retirada não é retirada de novo")
        void retirarDuasVezes() throws Exception {
            when(service.retirar(anyString(), any()))
                    .thenThrow(new OperacaoInvalidaException("Esta entrega já foi retirada."));
            mock().perform(post("/entregas/abc/retirar")
                            .contentType("application/json").content("Ana"))
                    .andExpect(status().isConflict());
        }
    }

    // ── Visitantes ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("VisitanteController")
    class Visitantes {

        @Mock private VisitanteService service;

        private MockMvc mock() {
            return mvc(new VisitanteController(service));
        }

        private Visitante visitante() {
            Visitante v = new Visitante();
            v.setId("vis-1");
            v.setNome("João");
            v.setCpf("52998224725");
            return v;
        }

        @Test
        @DisplayName("entrada devolve 201")
        void entrada() throws Exception {
            when(service.registrarEntrada(any())).thenReturn(visitante());
            mock().perform(post("/visitantes/entrada")
                            .contentType("application/json")
                            .content(json.writeValueAsString(visitante())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.nome").value("João"));
        }

        @Test
        @DisplayName("saída de quem não está dentro é recusada com 409")
        void saidaInvalida() throws Exception {
            when(service.registrarSaida("vis-1"))
                    .thenThrow(new OperacaoInvalidaException(
                            "Visitante não está registrado como DENTRO do condomínio."));
            mock().perform(post("/visitantes/vis-1/saida"))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("listar quem está dentro é endpoint próprio")
        void dentro() throws Exception {
            when(service.listarDentro(anyString())).thenReturn(List.of(visitante()));
            mock().perform(get("/visitantes/dentro"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
            verify(service, never()).listarTodos(anyString());
        }

        @Test
        @DisplayName("morador não registra entrada de visitante")
        void moradorBarrado() throws Exception {
            when(service.registrarEntrada(any()))
                    .thenThrow(new AcessoNegadoException(
                            "Seu perfil não pode registrar entrada de visitantes."));
            mock().perform(post("/visitantes/entrada")
                            .contentType("application/json")
                            .content(json.writeValueAsString(visitante())))
                    .andExpect(status().isForbidden());
        }
    }

    // ── Turnos ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("TurnoController")
    class Turnos {

        @Mock private TurnoService service;

        private MockMvc mock() {
            return mvc(new TurnoController(service));
        }

        private Turno turno() {
            Turno t = new Turno();
            t.setId("t-1");
            t.setCondominioId("cond-a");
            // @NotBlank nos dois: sem eles a validação responde 400 antes do serviço.
            t.setFuncionario("Carlos");
            t.setCargo("Porteiro");
            return t;
        }

        @Test
        @DisplayName("iniciar turno devolve 201")
        void iniciar() throws Exception {
            when(service.iniciar(any())).thenReturn(turno());
            mock().perform(post("/turnos/iniciar")
                            .contentType("application/json")
                            .content(json.writeValueAsString(turno())))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("retomar e finalizar respondem 200")
        void retomarFinalizar() throws Exception {
            when(service.retomar("t-1")).thenReturn(turno());
            when(service.finalizar("t-1")).thenReturn(turno());
            mock().perform(post("/turnos/t-1/retomar")).andExpect(status().isOk());
            mock().perform(post("/turnos/t-1/finalizar")).andExpect(status().isOk());
        }

        @Test
        @DisplayName("turno sem funcionário é 400, antes de chegar ao serviço")
        void semFuncionario() throws Exception {
            mock().perform(post("/turnos/iniciar")
                            .contentType("application/json").content("{\"cargo\":\"Porteiro\"}"))
                    .andExpect(status().isBadRequest());
            verify(service, never()).iniciar(any());
        }

        @Test
        @DisplayName("morador não inicia turno")
        void moradorBarrado() throws Exception {
            when(service.iniciar(any()))
                    .thenThrow(new AcessoNegadoException("Seu perfil não pode iniciar turnos."));
            mock().perform(post("/turnos/iniciar")
                            .contentType("application/json")
                            .content(json.writeValueAsString(turno())))
                    .andExpect(status().isForbidden());
        }
    }
}
