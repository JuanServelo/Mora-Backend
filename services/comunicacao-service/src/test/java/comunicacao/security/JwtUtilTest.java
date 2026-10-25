package comunicacao.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtUtilTest {

    @Test
    void recusaSubirSemSegredo() {
        assertThrows(IllegalStateException.class, () -> new JwtUtil(""));
        assertThrows(IllegalStateException.class, () -> new JwtUtil(null));
    }

    @Test
    void recusaSegredoCurto() {
        assertThrows(IllegalStateException.class, () -> new JwtUtil("curto-demais"));
    }

    @Test
    void aceitaSegredoComTamanhoMinimo() {
        assertDoesNotThrow(() -> new JwtUtil("x".repeat(JwtUtil.TAMANHO_MINIMO)));
    }
}
