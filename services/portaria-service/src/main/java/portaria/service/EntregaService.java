package portaria.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import portaria.exception.OperacaoInvalidaException;
import portaria.exception.RecursoNaoEncontradoException;
import portaria.model.Entrega;
import portaria.repository.EntregaRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EntregaService {

    private final EntregaRepository entregaRepository;

    public Entrega registrar(Entrega entrega) {
        entrega.setDataRecebimento(LocalDateTime.now());
        entrega.setRetirada(false);
        entrega.setDataRetirada(null);
        entrega.setRecebedor(null);
        return entregaRepository.save(entrega);
    }

    public Entrega retirar(String id, String recebedor) {
        Entrega entrega = buscarPorId(id);
        if (entrega.isRetirada()) {
            throw new OperacaoInvalidaException("Entrega já foi retirada.");
        }
        entrega.setRetirada(true);
        entrega.setDataRetirada(LocalDateTime.now());
        entrega.setRecebedor(recebedor);
        return entregaRepository.save(entrega);
    }

    public List<Entrega> listarTodas() {
        return entregaRepository.findAll();
    }

    public List<Entrega> listarPendentes() {
        return entregaRepository.findByRetirada(false);
    }

    public Entrega buscarPorId(String id) {
        return entregaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Entrega não encontrada com id: " + id));
    }
}
