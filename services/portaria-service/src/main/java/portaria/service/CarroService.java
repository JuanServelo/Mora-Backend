package portaria.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import portaria.exception.OperacaoInvalidaException;
import portaria.exception.RecursoNaoEncontradoException;
import portaria.model.Carro;
import portaria.model.enums.StatusAcesso;
import portaria.repository.CarroRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CarroService {

    private final CarroRepository carroRepository;

    public Carro registrarEntrada(Carro carro) {
        carroRepository.findByPlaca(carro.getPlaca()).ifPresent(c -> {
            if (c.getStatus() == StatusAcesso.DENTRO) {
                throw new OperacaoInvalidaException("Veículo com placa " + carro.getPlaca() + " já está registrado como DENTRO.");
            }
        });
        carro.setDataEntrada(LocalDateTime.now());
        carro.setStatus(StatusAcesso.DENTRO);
        return carroRepository.save(carro);
    }

    public Carro registrarSaida(String id) {
        Carro carro = buscarPorId(id);
        if (carro.getStatus() != StatusAcesso.DENTRO) {
            throw new OperacaoInvalidaException("Veículo não está registrado como DENTRO do condomínio.");
        }
        carro.setDataSaida(LocalDateTime.now());
        carro.setStatus(StatusAcesso.SAIU);
        return carroRepository.save(carro);
    }

    public List<Carro> listarTodos() {
        return carroRepository.findAll();
    }

    public List<Carro> listarDentro() {
        return carroRepository.findByStatus(StatusAcesso.DENTRO);
    }

    public Carro buscarPorId(String id) {
        return carroRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Veículo não encontrado com id: " + id));
    }
}
