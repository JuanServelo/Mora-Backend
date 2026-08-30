package portaria.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portaria.dto.ApartamentoRequestDTO;
import portaria.exception.OperacaoInvalidaException;
import portaria.exception.RecursoNaoEncontradoException;
import portaria.model.Apartamento;
import portaria.model.Bloco;
import portaria.repository.ApartamentoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ApartamentoService {

    private final ApartamentoRepository apartamentoRepository;
    private final BlocoService blocoService;

    public Apartamento cadastrar(ApartamentoRequestDTO request) {
        Bloco bloco = blocoService.buscarPorId(request.getBlocoId());

        apartamentoRepository.findByNumeroAndBloco_Id(request.getNumero(), request.getBlocoId())
                .ifPresent(a -> {
                    throw new OperacaoInvalidaException(
                            "Já existe um apartamento " + request.getNumero() +
                            " no bloco " + bloco.getNome());
                });

        if (bloco.getAndares() != null && request.getAndar() > bloco.getAndares()) {
            throw new OperacaoInvalidaException(
                    "Andar " + request.getAndar() +
                    " inválido. O bloco " + bloco.getNome() +
                    " tem apenas " + bloco.getAndares() + " andares");
        }

        Apartamento apartamento = new Apartamento();
        apartamento.setNumero(request.getNumero());
        apartamento.setAndar(request.getAndar());
        apartamento.setBloco(bloco);
        apartamento.setQuartos(request.getQuartos());
        apartamento.setAreaMxComTotal(request.getAreaMxComTotal());
        apartamento.setObservacoes(request.getObservacoes());
        apartamento.setCondominioId(bloco.getCondominioId());

        return apartamentoRepository.save(apartamento);
    }

    public List<Apartamento> listarTodos(String condominioId) {
        if (condominioId != null && !condominioId.isBlank()) {
            return apartamentoRepository.findByCondominioId(condominioId);
        }
        return apartamentoRepository.findAll();
    }

    public List<Apartamento> listarAtivos(String condominioId) {
        if (condominioId != null && !condominioId.isBlank()) {
            return apartamentoRepository.findByCondominioIdAndAtivo(condominioId, true);
        }
        return apartamentoRepository.findByAtivo(true);
    }

    public List<Apartamento> listarPorBloco(UUID blocoId) {
        blocoService.buscarPorId(blocoId);
        return apartamentoRepository.findByBloco_Id(blocoId);
    }

    public List<Apartamento> listarPorBlocoAtivos(UUID blocoId) {
        blocoService.buscarPorId(blocoId);
        return apartamentoRepository.findByBloco_IdAndAtivo(blocoId, true);
    }

    public Apartamento buscarPorId(UUID id) {
        return apartamentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Apartamento não encontrado com id: " + id));
    }

    public Apartamento atualizar(UUID id, ApartamentoRequestDTO request) {
        Apartamento apartamento = buscarPorId(id);
        Bloco bloco = blocoService.buscarPorId(request.getBlocoId());

        if (bloco.getAndares() != null && request.getAndar() > bloco.getAndares()) {
            throw new OperacaoInvalidaException(
                    "Andar " + request.getAndar() +
                    " inválido. O bloco " + bloco.getNome() +
                    " tem apenas " + bloco.getAndares() + " andares");
        }

        apartamento.setNumero(request.getNumero());
        apartamento.setAndar(request.getAndar());
        apartamento.setBloco(bloco);
        apartamento.setQuartos(request.getQuartos());
        apartamento.setAreaMxComTotal(request.getAreaMxComTotal());
        apartamento.setObservacoes(request.getObservacoes());
        apartamento.setAtualizadoEm(LocalDateTime.now());

        return apartamentoRepository.save(apartamento);
    }

    public void desativar(UUID id) {
        Apartamento apartamento = buscarPorId(id);
        apartamento.setAtivo(false);
        apartamento.setAtualizadoEm(LocalDateTime.now());
        apartamentoRepository.save(apartamento);
    }

    public void ativar(UUID id) {
        Apartamento apartamento = buscarPorId(id);
        apartamento.setAtivo(true);
        apartamento.setAtualizadoEm(LocalDateTime.now());
        apartamentoRepository.save(apartamento);
    }

    public void deletar(UUID id) {
        if (!apartamentoRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Apartamento não encontrado com id: " + id);
        }
        apartamentoRepository.deleteById(id);
    }
}
