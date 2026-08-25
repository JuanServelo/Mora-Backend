package com.mora.portaria.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.mora.portaria.exception.OperacaoInvalidaException;
import com.mora.portaria.exception.RecursoNaoEncontradoException;
import com.mora.portaria.entity.Carro;
import com.mora.portaria.enums.StatusAcesso;
import com.mora.portaria.enums.TipoProprietario;
import com.mora.portaria.dto.CriarCarroDTO;
import com.mora.portaria.repository.CarroRepository;
import com.mora.portaria.repository.MoradorRepository;
import com.mora.portaria.repository.VisitanteRepository;
import com.mora.portaria.repository.FuncionarioRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CarroService {

    private final CarroRepository carroRepository;
    private final MoradorRepository moradorRepository;
    private final VisitanteRepository visitanteRepository;
    private final FuncionarioRepository funcionarioRepository;

    public Carro cadastrar(CriarCarroDTO dto) {
        validarProprietario(dto.getProprietarioId(), dto.getTipoProprietario());
        
        carroRepository.findByPlaca(dto.getPlaca()).ifPresent(c -> {
            throw new OperacaoInvalidaException("Já existe um carro cadastrado com a placa: " + dto.getPlaca());
        });

        Carro carro = new Carro();
        carro.setPlaca(dto.getPlaca());
        carro.setModelo(dto.getModelo());
        carro.setProprietarioId(dto.getProprietarioId());
        carro.setTipoProprietario(dto.getTipoProprietario());
        carro.setStatus(StatusAcesso.SAIU);
        carro.setCriadoEm(LocalDateTime.now());
        carro.setAtualizadoEm(LocalDateTime.now());

        return carroRepository.save(carro);
    }

    public Carro registrarEntrada(Carro carro) {
        carroRepository.findByPlaca(carro.getPlaca()).ifPresent(c -> {
            if (c.getStatus() == StatusAcesso.DENTRO) {
                throw new OperacaoInvalidaException("Veículo com placa " + carro.getPlaca() + " já está registrado como DENTRO.");
            }
        });
        carro.setDataEntrada(LocalDateTime.now());
        carro.setStatus(StatusAcesso.DENTRO);
        carro.setAtualizadoEm(LocalDateTime.now());
        return carroRepository.save(carro);
    }

    public Carro registrarSaida(String id) {
        Carro carro = buscarPorId(id);
        if (carro.getStatus() != StatusAcesso.DENTRO) {
            throw new OperacaoInvalidaException("Veículo não está registrado como DENTRO do condomínio.");
        }
        carro.setDataSaida(LocalDateTime.now());
        carro.setStatus(StatusAcesso.SAIU);
        carro.setAtualizadoEm(LocalDateTime.now());
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

    public Carro atualizar(String id, CriarCarroDTO dados) {
        Carro carro = buscarPorId(id);
        
        if (!carro.getPlaca().equals(dados.getPlaca())) {
            carroRepository.findByPlaca(dados.getPlaca()).ifPresent(c -> {
                throw new OperacaoInvalidaException("Já existe um carro cadastrado com a placa: " + dados.getPlaca());
            });
        }

        validarProprietario(dados.getProprietarioId(), dados.getTipoProprietario());

        carro.setPlaca(dados.getPlaca());
        carro.setModelo(dados.getModelo());
        carro.setProprietarioId(dados.getProprietarioId());
        carro.setTipoProprietario(dados.getTipoProprietario());
        carro.setAtualizadoEm(LocalDateTime.now());

        return carroRepository.save(carro);
    }

    private void validarProprietario(String proprietarioId, TipoProprietario tipo) {
        switch (tipo) {
            case MORADOR:
                if (!moradorRepository.existsById(proprietarioId)) {
                    throw new RecursoNaoEncontradoException("Morador não encontrado com id: " + proprietarioId);
                }
                break;
            case VISITANTE:
                if (!visitanteRepository.existsById(proprietarioId)) {
                    throw new RecursoNaoEncontradoException("Visitante não encontrado com id: " + proprietarioId);
                }
                break;
            case FUNCIONARIO:
                if (!funcionarioRepository.existsById(proprietarioId)) {
                    throw new RecursoNaoEncontradoException("Funcionário não encontrado com id: " + proprietarioId);
                }
                break;
            default:
                throw new OperacaoInvalidaException("Tipo de proprietário inválido: " + tipo);
        }
    }
}

