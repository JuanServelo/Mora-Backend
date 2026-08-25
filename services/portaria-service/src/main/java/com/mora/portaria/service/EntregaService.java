package com.mora.portaria.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.mora.portaria.dto.EntregaRequestDTO;
import com.mora.portaria.exception.OperacaoInvalidaException;
import com.mora.portaria.exception.RecursoNaoEncontradoException;
import com.mora.portaria.entity.Entrega;
import com.mora.portaria.repository.EntregaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EntregaService {

    private final EntregaRepository entregaRepository;

    public Entrega cadastrar(EntregaRequestDTO dto) {
        Entrega entrega = new Entrega();
        entrega.setDestinatarioId(dto.getDestinatarioId());
        entrega.setDestinatarioNome(dto.getDestinatarioNome());
        entrega.setBloco(dto.getBloco());
        entrega.setApartamento(dto.getApartamento());
        entrega.setDescricao(dto.getDescricao());
        entrega.setRemetente(dto.getRemetente());
        entrega.setObservacoes(dto.getObservacoes());
        entrega.setStatus("PENDENTE");
        entrega.setDataRecebimento(
                dto.getDataRecebimento() != null
                        ? parseData(dto.getDataRecebimento())
                        : LocalDateTime.now()
        );
        entrega.setDataRetirada(null);
        entrega.setDestinatario(dto.getDestinatarioNome());
        entrega.setNomeEntregador(dto.getRemetente() != null ? dto.getRemetente() : "");
        entrega.setRetirada(false);
        return entregaRepository.save(entrega);
    }

    public Entrega atualizar(String id, EntregaRequestDTO dto) {
        Entrega entrega = buscarPorId(id);

        entrega.setDestinatarioId(dto.getDestinatarioId());
        entrega.setDestinatarioNome(dto.getDestinatarioNome());
        entrega.setBloco(dto.getBloco());
        entrega.setApartamento(dto.getApartamento());
        entrega.setDescricao(dto.getDescricao());
        entrega.setRemetente(dto.getRemetente());
        entrega.setObservacoes(dto.getObservacoes());

        if (dto.getDataRecebimento() != null) {
            entrega.setDataRecebimento(parseData(dto.getDataRecebimento()));
        }

        String novoStatus = dto.getStatus() != null ? dto.getStatus() : entrega.getStatus();
        entrega.setStatus(novoStatus);

        if ("RETIRADA".equals(novoStatus)) {
            entrega.setDataRetirada(
                    dto.getDataRetirada() != null
                            ? parseData(dto.getDataRetirada())
                            : (entrega.getDataRetirada() != null ? entrega.getDataRetirada() : LocalDateTime.now())
            );
            entrega.setRecebedorNome(
                    dto.getRecebedorNome() != null ? dto.getRecebedorNome() : entrega.getRecebedorNome()
            );
        } else {
            entrega.setDataRetirada(null);
            entrega.setRecebedorNome(null);
        }

        entrega.setDestinatario(entrega.getDestinatarioNome());
        entrega.setNomeEntregador(entrega.getRemetente() != null ? entrega.getRemetente() : "");
        entrega.setRetirada("RETIRADA".equals(novoStatus));
        entrega.setAtualizadoEm(LocalDateTime.now());
        return entregaRepository.save(entrega);
    }

    public void excluir(String id) {
        if (!entregaRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Entrega não encontrada com id: " + id);
        }
        entregaRepository.deleteById(id);
    }

    public Entrega retirar(String id, String recebedor) {
        Entrega entrega = buscarPorId(id);
        if ("RETIRADA".equals(entrega.getStatus())) {
            throw new OperacaoInvalidaException("Entrega já foi retirada.");
        }
        entrega.setStatus("RETIRADA");
        entrega.setDataRetirada(LocalDateTime.now());
        entrega.setRecebedorNome(recebedor != null && !recebedor.isBlank() ? recebedor : entrega.getDestinatarioNome());
        entrega.setDestinatario(entrega.getDestinatarioNome());
        entrega.setRetirada(true);
        entrega.setAtualizadoEm(LocalDateTime.now());
        return entregaRepository.save(entrega);
    }

    public List<Entrega> listarTodas() {
        return entregaRepository.findAll();
    }

    public List<Entrega> listarPendentes() {
        return entregaRepository.findByStatus("PENDENTE");
    }

    public Entrega buscarPorId(String id) {
        return entregaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Entrega não encontrada com id: " + id));
    }

    private LocalDateTime parseData(String data) {
        return LocalDate.parse(data).atStartOfDay();
    }
}

