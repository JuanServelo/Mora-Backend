package portaria.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import portaria.exception.OperacaoInvalidaException;
import portaria.exception.RecursoNaoEncontradoException;
import portaria.model.AreaComum;
import portaria.repository.AreaComunRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AreaComunService {

    private final AreaComunRepository areaComunRepository;

    public AreaComum cadastrar(AreaComum areaComum) {
        areaComunRepository.findByNome(areaComum.getNome()).ifPresent(a -> {
            throw new OperacaoInvalidaException("Já existe uma área comum cadastrada com o nome: " + areaComum.getNome());
        });
        return areaComunRepository.save(areaComum);
    }

    public List<AreaComum> listarTodas() {
        return areaComunRepository.findAll();
    }

    public List<AreaComum> listarAtivas() {
        return areaComunRepository.findByAtivo(true);
    }

    public List<AreaComum> listarPorTipo(String tipo) {
        return areaComunRepository.findByTipo(tipo);
    }

    public List<AreaComum> listarPorTipoAtivas(String tipo) {
        List<AreaComum> areas = areaComunRepository.findByTipo(tipo);
        return areas.stream().filter(AreaComum::isAtivo).toList();
    }

    public AreaComum buscarPorId(String id) {
        return areaComunRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Área comum não encontrada com id: " + id));
    }

    public AreaComum atualizar(String id, AreaComum dados) {
        AreaComum areaComum = buscarPorId(id);
        
        // Valida se o novo nome já existe (excluindo a área atual)
        if (!areaComum.getNome().equals(dados.getNome())) {
            areaComunRepository.findByNome(dados.getNome()).ifPresent(a -> {
                throw new OperacaoInvalidaException("Já existe uma área comum com o nome: " + dados.getNome());
            });
            areaComum.setNome(dados.getNome());
        }
        
        areaComum.setTipo(dados.getTipo());
        areaComum.setDescricao(dados.getDescricao());
        areaComum.setLocalizacao(dados.getLocalizacao());
        areaComum.setCapacidadeMaxima(dados.getCapacidadeMaxima());
        areaComum.setArea(dados.getArea());
        areaComum.setPodeReservar(dados.isPodeReservar());
        areaComum.setObservacoes(dados.getObservacoes());
        areaComum.setAtualizadoEm(LocalDateTime.now());
        
        return areaComunRepository.save(areaComum);
    }

    public void desativar(String id) {
        AreaComum areaComum = buscarPorId(id);
        areaComum.setAtivo(false);
        areaComum.setAtualizadoEm(LocalDateTime.now());
        areaComunRepository.save(areaComum);
    }

    public void ativar(String id) {
        AreaComum areaComum = buscarPorId(id);
        areaComum.setAtivo(true);
        areaComum.setAtualizadoEm(LocalDateTime.now());
        areaComunRepository.save(areaComum);
    }

    public void deletar(String id) {
        if (!areaComunRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Área comum não encontrada com id: " + id);
        }
        areaComunRepository.deleteById(id);
    }
}
