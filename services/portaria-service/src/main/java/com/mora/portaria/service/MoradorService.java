package com.mora.portaria.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.mora.portaria.exception.OperacaoInvalidaException;
import com.mora.portaria.exception.RecursoNaoEncontradoException;
import com.mora.portaria.entity.Morador;
import com.mora.portaria.repository.MoradorRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MoradorService {

    private final MoradorRepository moradorRepository;

    public Morador cadastrar(Morador morador) {
        moradorRepository.findByCpf(morador.getCpf()).ifPresent(m -> {
            throw new OperacaoInvalidaException("Já existe um morador cadastrado com o CPF: " + morador.getCpf());
        });
        return moradorRepository.save(morador);
    }

    public List<Morador> listarTodos() {
        return moradorRepository.findAll();
    }

    public List<Morador> listarAtivos() {
        return moradorRepository.findByAtivo(true);
    }

    public Morador buscarPorId(String id) {
        return moradorRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Morador não encontrado com id: " + id));
    }

    public Morador atualizar(String id, Morador dados) {
        Morador morador = buscarPorId(id);
        morador.setNome(dados.getNome());
        morador.setApartamento(dados.getApartamento());
        morador.setBloco(dados.getBloco());
        morador.setTelefone(dados.getTelefone());
        return moradorRepository.save(morador);
    }

    public void desativar(String id) {
        Morador morador = buscarPorId(id);
        morador.setAtivo(false);
        moradorRepository.save(morador);
    }
}

