package com.mora.portaria.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.mora.portaria.dto.chave.RetirarChaveRequest;
import com.mora.portaria.exception.OperacaoInvalidaException;
import com.mora.portaria.exception.RecursoNaoEncontradoException;
import com.mora.portaria.entity.Chave;
import com.mora.portaria.enums.TipoResponsavel;
import com.mora.portaria.repository.ChaveRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChaveService {

    private final ChaveRepository chaveRepository;
    private final MoradorService moradorService;
    private final FuncionarioService funcionarioService;

    public Chave cadastrar(Chave chave) {
        chave.setDisponivel(true);
        return chaveRepository.save(chave);
    }

    public void deletar(String id) {
        Chave chave = buscarPorId(id);
        if (!chave.isDisponivel()) {
            throw new OperacaoInvalidaException("Não é possível deletar uma chave que está retirada.");
        }
        chaveRepository.delete(chave);
    }

    public Chave retirar(String id, RetirarChaveRequest request) {
        Chave chave = buscarPorId(id);
        if (!chave.isDisponivel()) {
            throw new OperacaoInvalidaException("Chave indisponível — já está retirada.");
        }

        String nomeResponsavel = resolverNomeResponsavel(request.getResponsavelId(), request.getTipoResponsavel());

        chave.setDisponivel(false);
        chave.setResponsavelId(request.getResponsavelId());
        chave.setTipoResponsavel(request.getTipoResponsavel());
        chave.setNomeResponsavel(nomeResponsavel);
        chave.setRetirada(LocalDateTime.now());
        chave.setDevolucao(null);
        return chaveRepository.save(chave);
    }

    public Chave devolver(String id) {
        Chave chave = buscarPorId(id);
        if (chave.isDisponivel()) {
            throw new OperacaoInvalidaException("Chave já está disponível.");
        }
        chave.setDisponivel(true);
        chave.setDevolucao(LocalDateTime.now());
        chave.setResponsavelId(null);
        chave.setTipoResponsavel(null);
        chave.setNomeResponsavel(null);
        return chaveRepository.save(chave);
    }

    public List<Chave> listarTodas() {
        return chaveRepository.findAll();
    }

    public List<Chave> listarDisponiveis() {
        return chaveRepository.findByDisponivel(true);
    }

    public Chave buscarPorId(String id) {
        return chaveRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Chave não encontrada com id: " + id));
    }

    private String resolverNomeResponsavel(String responsavelId, TipoResponsavel tipo) {
        if (tipo == TipoResponsavel.MORADOR) {
            return moradorService.buscarPorId(responsavelId).getNome();
        } else {
            return funcionarioService.buscarPorId(responsavelId).getNome();
        }
    }
}

