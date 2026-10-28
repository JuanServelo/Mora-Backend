package ocorrencias.security;

import ocorrencias.exception.AcessoNegadoException;
import ocorrencias.exception.OperacaoInvalidaException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AutorizacaoTest {

    @AfterEach
    void limpar() {
        AuthContext.clear();
    }

    private static void como(String perfil, String condominioId) {
        AuthContext.set(new JwtClaims("7", "x@mora.com", perfil, condominioId));
    }

    @Test
    void sindicoLeSempreOProprioCondominioMesmoPedindoOutro() {
        como("ADMIN_SINDICO", "cond-a");
        assertEquals("cond-a", Autorizacao.condominioDeLeitura("cond-b"));
    }

    @Test
    void adminGeralEscolheOCondominioOuVeTodos() {
        como("ADMIN_GERAL", "default");
        assertEquals("cond-b", Autorizacao.condominioDeLeitura("cond-b"));
        assertNull(Autorizacao.condominioDeLeitura(null));
    }

    @Test
    void adminGeralAcompanhaMasNaoEscreve() {
        como("ADMIN_GERAL", "default");
        Autorizacao.exigirVisaoDeGestao("ver");
        assertThrows(AcessoNegadoException.class, () -> Autorizacao.exigirSindico("responder"));
    }

    @Test
    void moradorNaoTemVisaoDeGestao() {
        como("MORADOR", "cond-a");
        assertThrows(AcessoNegadoException.class, () -> Autorizacao.exigirVisaoDeGestao("ver"));
        assertFalse(Autorizacao.gestaoAlcanca("cond-a"));
    }

    @Test
    void sindicoNaoAlcancaOutroCondominio() {
        como("ADMIN_SINDICO", "cond-a");
        assertTrue(Autorizacao.gestaoAlcanca("cond-a"));
        assertFalse(Autorizacao.gestaoAlcanca("cond-b"));
    }

    @Test
    void escritaSemCondominioERecusada() {
        como("ADMIN_GERAL", "default");
        assertThrows(OperacaoInvalidaException.class, Autorizacao::exigirCondominio);
    }
}
