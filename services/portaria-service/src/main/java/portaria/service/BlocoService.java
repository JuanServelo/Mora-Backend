package portaria.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import portaria.exception.OperacaoInvalidaException;
import portaria.exception.RecursoNaoEncontradoException;
import portaria.model.Bloco;
import portaria.repository.BlocoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BlocoService {

    private final BlocoRepository blocoRepository;

    public Bloco cadastrar(Bloco bloco) {
        blocoRepository.findByNome(bloco.getNome()).ifPresent(b -> {
            throw new OperacaoInvalidaException("Já existe um bloco cadastrado com o nome: " + bloco.getNome());
        });
        return blocoRepository.save(bloco);
    }

    public List<Bloco> listarTodos() {
        return blocoRepository.findAll();
    }

    public List<Bloco> listarAtivos() {
        return blocoRepository.findByAtivo(true);
    }

    public Bloco buscarPorId(UUID id) {
        return blocoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Bloco não encontrado com id: " + id));
    }

    public Bloco atualizar(UUID id, Bloco dados) {
        Bloco bloco = buscarPorId(id);

        if (!bloco.getNome().equals(dados.getNome())) {
            blocoRepository.findByNome(dados.getNome()).ifPresent(b -> {
                throw new OperacaoInvalidaException("Já existe um bloco com o nome: " + dados.getNome());
            });
            bloco.setNome(dados.getNome());
        }

        bloco.setDescricao(dados.getDescricao());
        bloco.setAndares(dados.getAndares());
        bloco.setApartamentosPorAndar(dados.getApartamentosPorAndar());
        bloco.setAtualizadoEm(LocalDateTime.now());

        return blocoRepository.save(bloco);
    }

    public void desativar(UUID id) {
        Bloco bloco = buscarPorId(id);
        bloco.setAtivo(false);
        bloco.setAtualizadoEm(LocalDateTime.now());
        blocoRepository.save(bloco);
    }

    public void ativar(UUID id) {
        Bloco bloco = buscarPorId(id);
        bloco.setAtivo(true);
        bloco.setAtualizadoEm(LocalDateTime.now());
        blocoRepository.save(bloco);
    }

    public void deletar(UUID id) {
        if (!blocoRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Bloco não encontrado com id: " + id);
        }
        blocoRepository.deleteById(id);
    }
}