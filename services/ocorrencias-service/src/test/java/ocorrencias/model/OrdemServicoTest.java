package ocorrencias.model;

import ocorrencias.model.enums.StatusOrdemServico;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class OrdemServicoTest {

    private static OrdemServico concluida(LocalDate prazo, LocalDateTime em) {
        OrdemServico os = new OrdemServico();
        os.setPrazo(prazo);
        os.setStatus(StatusOrdemServico.CONCLUIDA);
        os.setConcluidaEm(em);
        return os;
    }

    @Test
    void concluidaNoUltimoDiaDoPrazoContaComoNoPrazo() {
        assertTrue(concluida(LocalDate.of(2026, 10, 20), LocalDateTime.of(2026, 10, 20, 23, 59)).isConcluidaNoPrazo());
    }

    @Test
    void concluidaDepoisDoPrazoNaoConta() {
        assertFalse(concluida(LocalDate.of(2026, 10, 20), LocalDateTime.of(2026, 10, 21, 0, 1)).isConcluidaNoPrazo());
    }

    @Test
    void naoConcluidaNaoConta() {
        OrdemServico os = new OrdemServico();
        os.setPrazo(LocalDate.of(2026, 10, 20));
        os.setStatus(StatusOrdemServico.EM_EXECUCAO);
        assertFalse(os.isConcluidaNoPrazo());
    }

    @Test
    void semOcorrenciaDeOrigemEPreventiva() {
        OrdemServico os = new OrdemServico();
        assertTrue(os.isPreventiva());
        os.setReclamacao(new Reclamacao());
        assertFalse(os.isPreventiva());
    }
}
