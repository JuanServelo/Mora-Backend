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
        validarCondominioObrigatorio(bloco.getCondominioId());
        blocoRepository.findByNomeAndCondominioId(bloco.getNome(), bloco.getCondominioId()).ifPresent(b -> {
            throw new OperacaoInvalidaException(
                    "Já existe um bloco com o nome '" + bloco.getNome() + "' neste condomínio.");
        });
        return blocoRepository.save(bloco);
    }

    public List<Bloco> listarTodos(String condominioId) {
        if (condominioId != null && !condominioId.isBlank()) {
            return blocoRepository.findByCondominioId(condominioId);
        }
        return blocoRepository.findAll();
    }

    public List<Bloco> listarAtivos(String condominioId) {
        if (condominioId != null && !condominioId.isBlank()) {
            return blocoRepository.findByCondominioIdAndAtivo(condominioId, true);
        }
        return blocoRepository.findByAtivo(true);
    }

    public Bloco buscarPorId(UUID id) {
        return blocoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Bloco não encontrado com id: " + id));
    }

    public Bloco atualizar(UUID id, Bloco dados) {
        Bloco bloco = buscarPorId(id);
        String condId = bloco.getCondominioId();
        validarCondominioObrigatorio(condId);

        if (!bloco.getNome().equals(dados.getNome())) {
            blocoRepository.findByNomeAndCondominioId(dados.getNome(), condId).ifPresent(b -> {
                if (!b.getId().equals(bloco.getId())) {
                    throw new OperacaoInvalidaException(
                            "Já existe um bloco com o nome '" + dados.getNome() + "' neste condomínio.");
                }
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

    private void validarCondominioObrigatorio(String condominioId) {
        if (condominioId == null || condominioId.isBlank()) {
            throw new OperacaoInvalidaException("Condomínio é obrigatório para cadastrar ou alterar bloco.");
        }
    }
}
