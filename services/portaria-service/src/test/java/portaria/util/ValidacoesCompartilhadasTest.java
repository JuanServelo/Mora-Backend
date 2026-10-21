package portaria.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import portaria.exception.OperacaoInvalidaException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Placa e CPF: a mesma regra usada no atendimento, no cadastro de veículo e na
 * pré-autorização. Eram três cópias antes de virarem estas classes — e três
 * cópias divergem na primeira mudança de formato, que no caso da placa já
 * aconteceu uma vez com o Mercosul.
 */
class ValidacoesCompartilhadasTest {

    @Nested
    @DisplayName("Placa")
    class Placa {

        @ParameterizedTest
        @ValueSource(strings = {"ABC1234", "abc1234", "ABC-1234", "abc-1234", " ABC 1234 "})
        @DisplayName("formato antigo, em qualquer grafia, normaliza para ABC1234")
        void formatoAntigo(String entrada) {
            assertEquals("ABC1234", PlacaUtils.normalizarEValidar(entrada));
        }

        @ParameterizedTest
        @ValueSource(strings = {"ABC1D23", "abc1d23", "ABC-1D23"})
        @DisplayName("formato Mercosul é aceito")
        void mercosul(String entrada) {
            assertEquals("ABC1D23", PlacaUtils.normalizarEValidar(entrada));
        }

        @ParameterizedTest
        @ValueSource(strings = {"AB1234", "ABCD1234", "1234ABC", "ABC12345", "ABC123", ""})
        @DisplayName("formatos que não existem são recusados")
        void invalidas(String entrada) {
            assertThrows(OperacaoInvalidaException.class,
                    () -> PlacaUtils.normalizarEValidar(entrada));
        }

        @Test
        @DisplayName("nulo é recusado, não aceito como vazio")
        void nulo() {
            assertNull(PlacaUtils.normalizar(null));
            assertFalse(PlacaUtils.valida(null));
            assertThrows(OperacaoInvalidaException.class,
                    () -> PlacaUtils.normalizarEValidar(null));
        }

        @Test
        @DisplayName("a mensagem mostra os dois formatos aceitos")
        void mensagemOrienta() {
            var e = assertThrows(OperacaoInvalidaException.class,
                    () -> PlacaUtils.normalizarEValidar("XX"));
            assertTrue(e.getMessage().contains("Mercosul"), e.getMessage());
        }
    }

    @Nested
    @DisplayName("CPF")
    class Cpf {

        @ParameterizedTest
        @ValueSource(strings = {"529.982.247-25", "52998224725", " 529 982 247 25 "})
        @DisplayName("CPF válido, com ou sem máscara, normaliza para 11 dígitos")
        void valido(String entrada) {
            assertEquals("52998224725", CpfUtils.normalizarEValidar(entrada));
        }

        @Test
        @DisplayName("dígito verificador errado é recusado")
        void digitoErrado() {
            assertFalse(CpfUtils.valido("52998224724"));
            assertThrows(OperacaoInvalidaException.class,
                    () -> CpfUtils.normalizarEValidar("529.982.247-24"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"00000000000", "11111111111", "99999999999"})
        @DisplayName("todos os dígitos iguais é recusado, mesmo passando no cálculo")
        void digitosRepetidos(String entrada) {
            assertFalse(CpfUtils.valido(entrada),
                    entrada + " passa no cálculo do DV mas não é um CPF real");
        }

        @ParameterizedTest
        @ValueSource(strings = {"123", "529982247250", "abcdefghijk"})
        @DisplayName("tamanho errado ou não numérico é recusado")
        void tamanhoErrado(String entrada) {
            assertThrows(OperacaoInvalidaException.class,
                    () -> CpfUtils.normalizarEValidar(entrada));
        }

        @Test
        @DisplayName("nulo é recusado")
        void nulo() {
            assertNull(CpfUtils.normalizar(null));
            assertFalse(CpfUtils.valido(null));
            assertThrows(OperacaoInvalidaException.class,
                    () -> CpfUtils.normalizarEValidar(null));
        }
    }
}
