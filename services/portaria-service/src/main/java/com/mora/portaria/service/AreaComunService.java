package com.mora.portaria.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.mora.portaria.exception.OperacaoInvalidaException;
import com.mora.portaria.exception.RecursoNaoEncontradoException;
import com.mora.portaria.entity.AreaComum;
import com.mora.portaria.repository.AreaComunRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AreaComunService {

    private final AreaComunRepository areaComunRepository;

    public AreaComum cadastrar(AreaComum areaComum) {
        if (areaComum.getCondominioId() != null) {
            areaComunRepository.findByNomeAndCondominioId(areaComum.getNome(), areaComum.getCondominioId()).ifPresent(a -> {
                throw new OperacaoInvalidaException("Já existe uma área comum com o nome '" + areaComum.getNome() + "' neste cliente.");
            });
        } else {
            areaComunRepository.findByNome(areaComum.getNome()).ifPresent(a -> {
                throw new OperacaoInvalidaException("Já existe uma área comum cadastrada com o nome: " + areaComum.getNome());
            });
        }
        return areaComunRepository.save(areaComum);
    }

    public List<AreaComum> listarTodas(String condominioId) {
        if (condominioId != null && !condominioId.isBlank()) {
            return areaComunRepository.findByCondominioId(condominioId);
        }
        return areaComunRepository.findAll();
    }

    public List<AreaComum> listarAtivas(String condominioId) {
        if (condominioId != null && !condominioId.isBlank()) {
            return areaComunRepository.findByCondominioIdAndAtivo(condominioId, true);
        }
        return areaComunRepository.findByAtivo(true);
    }

    public List<AreaComum> listarPorTipo(String tipo) {
        return areaComunRepository.findByTipo(tipo);
    }

    public List<AreaComum> listarPorTipoAtivas(String tipo) {
        return areaComunRepository.findByTipo(tipo).stream().filter(AreaComum::isAtivo).toList();
    }

    public AreaComum buscarPorId(String id) {
        return areaComunRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Área comum não encontrada com id: " + id));
    }

    public AreaComum atualizar(String id, AreaComum dados) {
        AreaComum areaComum = buscarPorId(id);
        String condId = areaComum.getCondominioId();

        if (!areaComum.getNome().equals(dados.getNome())) {
            if (condId != null) {
                areaComunRepository.findByNomeAndCondominioId(dados.getNome(), condId).ifPresent(a -> {
                    throw new OperacaoInvalidaException("Já existe uma área comum com o nome: " + dados.getNome());
                });
            } else {
                areaComunRepository.findByNome(dados.getNome()).ifPresent(a -> {
                    throw new OperacaoInvalidaException("Já existe uma área comum com o nome: " + dados.getNome());
                });
            }
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

