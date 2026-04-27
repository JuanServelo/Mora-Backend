package portaria.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import portaria.exception.OperacaoInvalidaException;
import portaria.exception.RecursoNaoEncontradoException;
import portaria.model.Visitante;
import portaria.model.enums.StatusAcesso;
import portaria.repository.VisitanteRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VisitanteService {

    private final VisitanteRepository visitanteRepository;

    public Visitante registrarEntrada(Visitante visitante) {
        visitante.setHorarioEntrada(LocalDateTime.now());
        visitante.setStatus(StatusAcesso.DENTRO);
        return visitanteRepository.save(visitante);
    }

    public Visitante registrarSaida(String id) {
        Visitante visitante = buscarPorId(id);
        if (visitante.getStatus() != StatusAcesso.DENTRO) {
            throw new OperacaoInvalidaException("Visitante não está registrado como DENTRO do condomínio.");
        }
        visitante.setHorarioSaida(LocalDateTime.now());
        visitante.setStatus(StatusAcesso.SAIU);
        return visitanteRepository.save(visitante);
    }

    public List<Visitante> listarTodos() {
        return visitanteRepository.findAll();
    }

    public List<Visitante> listarDentro() {
        return visitanteRepository.findByStatus(StatusAcesso.DENTRO);
    }

    public Visitante buscarPorId(String id) {
        return visitanteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Visitante não encontrado com id: " + id));
    }
}
