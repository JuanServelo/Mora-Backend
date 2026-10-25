package portaria.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import portaria.dto.ApartamentoRequestDTO;
import portaria.exception.AcessoNegadoException;
import portaria.exception.GlobalExceptionHandler;
import portaria.exception.RecursoNaoEncontradoException;
import portaria.model.AreaComum;
import portaria.model.Bloco;
import portaria.model.Vaga;
import portaria.security.AuthContext;
import portaria.security.JwtClaims;
import portaria.service.ApartamentoService;
import portaria.service.AreaComunService;
import portaria.service.BlocoService;
import portaria.service.VagaService;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controllers de estrutura — as telas do síndico.
 *
 * São passagens finas para o serviço, e é justamente por isso que erram em
 * silêncio: um verbo trocado, um código de status errado ou um parâmetro que
 * não chega ao serviço não quebram compilação. Os testes montam o MockMvc em
 * modo standalone, com o GlobalExceptionHandler registrado, para que o
 * mapeamento de exceção para status também fique coberto.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EstruturaControllersTest {

    // As entidades têm LocalDateTime; sem o módulo o Jackson recusa serializar.
    private final ObjectMapper json = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    private static MockMvc mvc(Object controller) {
        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @BeforeEach
    void autenticar() {
        AuthContext.set(new JwtClaims("1", "s@m.com", "ADMIN_SINDICO", "cond-a", null, "Síndico"));
    }

    @AfterEach
    void limpar() {
        AuthContext.clear();
    }

    // ── Blocos ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("BlocoController")
    class Blocos {

        @Mock private BlocoService service;

        private MockMvc mock() {
            return mvc(new BlocoController(service));
        }

        private Bloco bloco() {
            Bloco b = new Bloco();
            b.setId(UUID.randomUUID());
            b.setNome("Bloco A");
            b.setCondominioId("cond-a");
            b.setAndares(4);
            b.setApartamentosPorAndar(2);
            return b;
        }

        @Test
        @DisplayName("cadastrar devolve 201, não 200")
        void cadastrar() throws Exception {
            when(service.cadastrar(any())).thenReturn(bloco());
            mock().perform(post("/blocos/cadastrar")
                            .contentType("application/json")
                            .content(json.writeValueAsString(bloco())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.nome").value("Bloco A"));
        }

        @Test
        @DisplayName("listar aplica o escopo do condomínio do autenticado")
        void listarEscopado() throws Exception {
            when(service.listarAtivos("cond-a")).thenReturn(List.of(bloco()));
            mock().perform(get("/blocos"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
            verify(service).listarAtivos("cond-a");
        }

        @Test
        @DisplayName("parâmetro de outro condomínio é recusado com 403")
        void escopoDeOutroCondominio() throws Exception {
            mock().perform(get("/blocos").param("condominioId", "cond-b"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("/todos também passa pelo escopo")
        void listarTodos() throws Exception {
            when(service.listarTodos("cond-a")).thenReturn(List.of());
            mock().perform(get("/blocos/todos")).andExpect(status().isOk());
            verify(service).listarTodos("cond-a");
        }

        @Test
        @DisplayName("buscar por id inexistente vira 404")
        void naoEncontrado() throws Exception {
            when(service.buscarPorId(any(UUID.class)))
                    .thenThrow(new RecursoNaoEncontradoException("Bloco não encontrado"));
            mock().perform(get("/blocos/" + UUID.randomUUID()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("perfil sem permissão vira 403, e não 500")
        void semPermissao() throws Exception {
            when(service.cadastrar(any()))
                    .thenThrow(new AcessoNegadoException("Seu perfil não pode cadastrar blocos."));
            mock().perform(post("/blocos/cadastrar")
                            .contentType("application/json")
                            .content(json.writeValueAsString(bloco())))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.erro").value("Seu perfil não pode cadastrar blocos."));
        }

        @Test
        @DisplayName("ativar e desativar devolvem 204")
        void ativarDesativar() throws Exception {
            UUID id = UUID.randomUUID();
            mock().perform(put("/blocos/" + id + "/desativar")).andExpect(status().isNoContent());
            mock().perform(put("/blocos/" + id + "/ativar")).andExpect(status().isNoContent());
            verify(service).desativar(id);
            verify(service).ativar(id);
        }

        @Test
        @DisplayName("excluir devolve 204")
        void excluir() throws Exception {
            UUID id = UUID.randomUUID();
            mock().perform(delete("/blocos/" + id)).andExpect(status().isNoContent());
            verify(service).deletar(id);
        }

        @Test
        @DisplayName("atualizar repassa id e corpo ao serviço")
        void atualizar() throws Exception {
            UUID id = UUID.randomUUID();
            when(service.atualizar(eq(id), any())).thenReturn(bloco());
            mock().perform(put("/blocos/" + id)
                            .contentType("application/json")
                            .content(json.writeValueAsString(bloco())))
                    .andExpect(status().isOk());
            verify(service).atualizar(eq(id), any());
        }
    }

    // ── Áreas comuns ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("AreaComunController")
    class AreasComuns {

        @Mock private AreaComunService service;

        private MockMvc mock() {
            return mvc(new AreaComunController(service));
        }

        private AreaComum area() {
            AreaComum a = new AreaComum();
            a.setId("area-1");
            a.setNome("Salão de Festas");
            a.setTipo("SALAO_FESTAS");
            a.setCondominioId("cond-a");
            a.setCapacidadeMaxima(40);
            a.setArea(60.0);
            a.setPodeReservar(true);
            a.setAntecedenciaMinimaHoras(48);
            return a;
        }

        @Test
        @DisplayName("cadastrar sem condominioId no corpo é aceito — o serviço resolve")
        void cadastrarSemCondominio() throws Exception {
            AreaComum semCond = area();
            semCond.setCondominioId(null);
            when(service.cadastrar(any())).thenReturn(area());

            mock().perform(post("/areas-comuns/cadastrar")
                            .contentType("application/json")
                            .content(json.writeValueAsString(semCond)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("a resposta carrega a antecedência mínima configurada")
        void devolveAntecedencia() throws Exception {
            when(service.cadastrar(any())).thenReturn(area());
            mock().perform(post("/areas-comuns/cadastrar")
                            .contentType("application/json")
                            .content(json.writeValueAsString(area())))
                    .andExpect(jsonPath("$.antecedenciaMinimaHoras").value(48))
                    .andExpect(jsonPath("$.podeReservar").value(true));
        }

        @Test
        @DisplayName("alterar área de outro condomínio é barrado")
        void alterarDeOutro() throws Exception {
            AreaComum deOutro = area();
            deOutro.setCondominioId("cond-b");
            when(service.buscarPorId("area-1")).thenReturn(deOutro);

            mock().perform(put("/areas-comuns/area-1")
                            .contentType("application/json")
                            .content(json.writeValueAsString(area())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("listar devolve as áreas do condomínio")
        void listar() throws Exception {
            when(service.listarAtivas(anyString())).thenReturn(List.of(area()));
            mock().perform(get("/areas-comuns")).andExpect(status().isOk());
        }
    }

    // ── Apartamentos ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("ApartamentoController")
    class Apartamentos {

        @Mock private ApartamentoService service;

        private MockMvc mock() {
            return mvc(new ApartamentoController(service));
        }

        @Test
        @DisplayName("cadastro inválido vira 409 com a mensagem do serviço")
        void cadastroInvalido() throws Exception {
            when(service.cadastrar(any(ApartamentoRequestDTO.class)))
                    .thenThrow(new portaria.exception.OperacaoInvalidaException(
                            "Já existe um apartamento 101 no bloco Bloco A"));

            mock().perform(post("/apartamentos/cadastrar")
                            .contentType("application/json")
                            .content("{\"numero\":\"101\",\"andar\":1,\"blocoId\":\""
                                    + UUID.randomUUID() + "\"}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.erro").value(
                            "Já existe um apartamento 101 no bloco Bloco A"));
        }

        @Test
        @DisplayName("corpo sem campo obrigatório é 400, antes de chegar ao serviço")
        void corpoIncompleto() throws Exception {
            mock().perform(post("/apartamentos/cadastrar")
                            .contentType("application/json")
                            .content("{\"numero\":\"101\"}"))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("desativar devolve 204")
        void desativar() throws Exception {
            UUID id = UUID.randomUUID();
            mock().perform(put("/apartamentos/" + id + "/desativar"))
                    .andExpect(status().isNoContent());
            verify(service).desativar(id);
        }
    }

    // ── Vagas ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("VagaController")
    class Vagas {

        @Mock private VagaService service;

        private MockMvc mock() {
            return mvc(new VagaController(service));
        }

        private Vaga vaga() {
            Vaga v = new Vaga();
            v.setId("vaga-1");
            v.setNumero("A-12");
            v.setCondominioId("cond-a");
            return v;
        }

        @Test
        @DisplayName("cadastrar devolve 201")
        void cadastrar() throws Exception {
            when(service.cadastrar(any(), any())).thenReturn(vaga());
            mock().perform(post("/vagas/cadastrar")
                            .contentType("application/json")
                            .content(json.writeValueAsString(vaga())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.numero").value("A-12"));
        }

        @Test
        @DisplayName("morador tentando cadastrar recebe 403")
        void moradorBarrado() throws Exception {
            when(service.cadastrar(any(), any()))
                    .thenThrow(new AcessoNegadoException("Seu perfil não pode cadastrar vagas."));
            mock().perform(post("/vagas/cadastrar")
                            .contentType("application/json")
                            .content(json.writeValueAsString(vaga())))
                    .andExpect(status().isForbidden());
        }
    }
}
