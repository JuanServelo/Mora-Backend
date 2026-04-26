package portaria.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
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
public class ApartamentoService {

    private final ApartamentoRepository apartamentoRepository;
    private final BlocoService blocoService;

    public Apartamento cadastrar(Apartamento apartamento, UUID blocoId) {

        Bloco bloco = blocoService.buscarPorId(blocoId);

        apartamentoRepository.findByNumeroAndBloco_Id(apartamento.getNumero(), blocoId)
                .ifPresent(a -> {
                    throw new OperacaoInvalidaException(
                            "Já existe um apartamento " + apartamento.getNumero() +
                            " no bloco " + bloco.getNome());
                });

        if (bloco.getAndares() != null && apartamento.getAndar() > bloco.getAndares()) {
            throw new OperacaoInvalidaException(
                    "Andar " + apartamento.getAndar() +
                    " inválido. O bloco " + bloco.getNome() +
                    " tem apenas " + bloco.getAndares() + " andares");
        }

        apartamento.setBloco(bloco);

        return apartamentoRepository.save(apartamento);
    }

    public List<Apartamento> listarTodos() {
        return apartamentoRepository.findAll();
    }

    public List<Apartamento> listarAtivos() {
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

    public Apartamento atualizar(UUID id, Apartamento dados, UUID blocoId) {

        Apartamento apartamento = buscarPorId(id);
        Bloco bloco = blocoService.buscarPorId(blocoId);

        if (bloco.getAndares() != null && dados.getAndar() > bloco.getAndares()) {
            throw new OperacaoInvalidaException(
                    "Andar " + dados.getAndar() +
                    " inválido. O bloco " + bloco.getNome() +
                    " tem apenas " + bloco.getAndares() + " andares");
        }

        apartamento.setNumero(dados.getNumero());
        apartamento.setAndar(dados.getAndar());
        apartamento.setBloco(bloco);
        apartamento.setQuartos(dados.getQuartos());
        apartamento.setAreaMxComTotal(dados.getAreaMxComTotal());
        apartamento.setObservacoes(dados.getObservacoes());
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