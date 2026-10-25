package comunicacao.security;

import comunicacao.model.enums.PublicoAlvo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicoAlvoRegraTest {

    @Test
    void todosAlcancaMoradoresEPortariaMasNaoAGestao() {
        assertTrue(PublicoAlvoRegra.ehDestinatario(PublicoAlvo.TODOS, "MORADOR"));
        assertTrue(PublicoAlvoRegra.ehDestinatario(PublicoAlvo.TODOS, "DONO_ALUGUEL"));
        assertTrue(PublicoAlvoRegra.ehDestinatario(PublicoAlvo.TODOS, "PORTEIRO"));
        // A gestão publica; não é destinatária do próprio comunicado.
        assertFalse(PublicoAlvoRegra.ehDestinatario(PublicoAlvo.TODOS, "ADMIN_SINDICO"));
        assertFalse(PublicoAlvoRegra.ehDestinatario(PublicoAlvo.TODOS, "ADMIN_GERAL"));
    }

    @Test
    void funcionariosNaoChegaAoMorador() {
        assertTrue(PublicoAlvoRegra.ehDestinatario(PublicoAlvo.FUNCIONARIOS, "PORTEIRO"));
        assertFalse(PublicoAlvoRegra.ehDestinatario(PublicoAlvo.FUNCIONARIOS, "MORADOR"));
    }

    @Test
    void moradoresNaoChegaAPortaria() {
        assertTrue(PublicoAlvoRegra.ehDestinatario(PublicoAlvo.MORADORES, "DONO_ALUGUEL"));
        assertFalse(PublicoAlvoRegra.ehDestinatario(PublicoAlvo.MORADORES, "PORTEIRO"));
    }

    @Test
    void publicoSindicoAlcancaSoOSindico() {
        assertTrue(PublicoAlvoRegra.ehDestinatario(PublicoAlvo.SINDICO, "ADMIN_SINDICO"));
        assertFalse(PublicoAlvoRegra.ehDestinatario(PublicoAlvo.SINDICO, "MORADOR"));
    }

    @Test
    void semPerfilOuSemAcessoNaoRecebe() {
        assertFalse(PublicoAlvoRegra.ehDestinatario(PublicoAlvo.TODOS, null));
        assertFalse(PublicoAlvoRegra.ehDestinatario(PublicoAlvo.TODOS, "CONVIDADO"));
        assertFalse(PublicoAlvoRegra.ehDestinatario(PublicoAlvo.TODOS, "TERCEIRO"));
    }

    @Test
    void avisoSemPublicoValeComoTodos() {
        assertTrue(PublicoAlvoRegra.ehDestinatario(null, "MORADOR"));
    }
}
