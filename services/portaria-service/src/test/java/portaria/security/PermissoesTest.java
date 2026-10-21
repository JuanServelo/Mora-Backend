package portaria.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import portaria.exception.AcessoNegadoException;
import portaria.exception.OperacaoInvalidaException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guarda das duas regras de escrita do serviço: quem pode, e em qual condomínio.
 *
 * São os dois defeitos que a tabela de pendências listava como 🔴. Testar aqui
 * vale mais do que em cada um dos doze serviços: todos chamam estes métodos, e
 * um erro aqui reabre os dois buracos de uma vez.
 */
class PermissoesTest {

    private static void autenticar(String perfil, String condominioId) {
        AuthContext.set(new JwtClaims("1", "a@b.com", perfil, condominioId, null, "Fulano"));
    }

    @AfterEach
    void limpar() {
        AuthContext.clear();
    }

    @Nested
    @DisplayName("exigirAdmin")
    class ExigirAdmin {

        @Test
        @DisplayName("deixa passar síndico e admin geral")
        void permiteAdmins() {
            autenticar("ADMIN_SINDICO", "cond-a");
            assertDoesNotThrow(() -> Permissoes.exigirAdmin("cadastrar blocos"));

            autenticar("ADMIN_GERAL", "default");
            assertDoesNotThrow(() -> Permissoes.exigirAdmin("cadastrar blocos"));
        }

        @Test
        @DisplayName("barra morador, e a mensagem diz qual ação foi negada")
        void barraMorador() {
            autenticar("MORADOR", "cond-a");
            var e = assertThrows(AcessoNegadoException.class,
                    () -> Permissoes.exigirAdmin("cadastrar blocos"));
            assertTrue(e.getMessage().contains("cadastrar blocos"),
                    "a mensagem precisa nomear a ação: " + e.getMessage());
        }

        @Test
        @DisplayName("barra porteiro — estrutura é da administração")
        void barraPorteiro() {
            autenticar("PORTEIRO", "cond-a");
            assertThrows(AcessoNegadoException.class, () -> Permissoes.exigirAdmin("cadastrar vagas"));
        }

        @Test
        @DisplayName("sem autenticação é acesso negado, não NullPointerException")
        void semAutenticacao() {
            AuthContext.clear();
            assertThrows(AcessoNegadoException.class, () -> Permissoes.exigirAdmin("cadastrar blocos"));
        }
    }

    @Nested
    @DisplayName("exigirOperacao")
    class ExigirOperacao {

        @Test
        @DisplayName("porteiro opera o dia a dia da portaria")
        void permitePorteiro() {
            autenticar("PORTEIRO", "cond-a");
            assertDoesNotThrow(() -> Permissoes.exigirOperacao("registrar entregas"));
        }

        @Test
        @DisplayName("admins também operam")
        void permiteAdmins() {
            autenticar("ADMIN_SINDICO", "cond-a");
            assertDoesNotThrow(() -> Permissoes.exigirOperacao("registrar entregas"));
        }

        @Test
        @DisplayName("morador não registra entrega de ninguém")
        void barraMorador() {
            autenticar("MORADOR", "cond-a");
            assertThrows(AcessoNegadoException.class,
                    () -> Permissoes.exigirOperacao("registrar entregas"));
        }
    }

    @Nested
    @DisplayName("condominioParaGravar")
    class CondominioParaGravar {

        @Test
        @DisplayName("ignora o corpo: é o que impede gravar no condomínio alheio")
        void ignoraCorpo() {
            autenticar("ADMIN_SINDICO", "cond-a");
            assertEquals("cond-a", Permissoes.condominioParaGravar("cond-b"));
        }

        @Test
        @DisplayName("usa a claim mesmo quando o corpo vem vazio")
        void corpoVazio() {
            autenticar("ADMIN_SINDICO", "cond-a");
            assertEquals("cond-a", Permissoes.condominioParaGravar(null));
            assertEquals("cond-a", Permissoes.condominioParaGravar(""));
        }

        @Test
        @DisplayName("admin geral não tem condomínio próprio, então escolhe pelo corpo")
        void adminGeralEscolhe() {
            autenticar("ADMIN_GERAL", "default");
            assertEquals("cond-b", Permissoes.condominioParaGravar("cond-b"));
        }

        @Test
        @DisplayName("admin geral sem informar: recusa em vez de gravar registro órfão")
        void adminGeralSemInformar() {
            autenticar("ADMIN_GERAL", "default");
            assertThrows(OperacaoInvalidaException.class,
                    () -> Permissoes.condominioParaGravar(null));
            assertThrows(OperacaoInvalidaException.class,
                    () -> Permissoes.condominioParaGravar("   "));
        }
    }

    @Nested
    @DisplayName("exigirMesmoCondominio")
    class ExigirMesmoCondominio {

        @Test
        @DisplayName("deixa alterar registro do próprio condomínio")
        void mesmo() {
            autenticar("ADMIN_SINDICO", "cond-a");
            assertDoesNotThrow(() -> Permissoes.exigirMesmoCondominio("cond-a"));
        }

        @Test
        @DisplayName("barra alterar registro de outro condomínio pelo id")
        void outro() {
            autenticar("ADMIN_SINDICO", "cond-a");
            assertThrows(AcessoNegadoException.class,
                    () -> Permissoes.exigirMesmoCondominio("cond-b"));
        }

        @Test
        @DisplayName("registro sem condomínio não é de ninguém: barra")
        void registroOrfao() {
            autenticar("ADMIN_SINDICO", "cond-a");
            assertThrows(AcessoNegadoException.class,
                    () -> Permissoes.exigirMesmoCondominio(null));
        }

        @Test
        @DisplayName("admin geral alcança qualquer condomínio")
        void adminGeral() {
            autenticar("ADMIN_GERAL", "default");
            assertDoesNotThrow(() -> Permissoes.exigirMesmoCondominio("cond-b"));
            assertDoesNotThrow(() -> Permissoes.exigirMesmoCondominio(null));
        }
    }
}
