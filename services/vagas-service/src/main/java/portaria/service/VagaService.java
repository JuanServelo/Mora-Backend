package portaria.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import portaria.dto.VagaRequestDTO;
import portaria.exception.OperacaoInvalidaException;
import portaria.exception.RecursoNaoEncontradoException;
import portaria.model.Apartamento;
import portaria.model.Vaga;
import portaria.repository.ApartamentoRepository;
import portaria.repository.VagaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VagaService {

    private final VagaRepository vagaRepository;
    private final ApartamentoRepository apartamentoRepository;

    public Vaga cadastrar(VagaRequestDTO dto) {
        validarVagaUnica(dto.numero());

        Apartamento apt = apartamentoRepository.findById(dto.apartamentoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Apartamento não encontrado: " + dto.apartamentoId()));

        Vaga vaga = new Vaga();
        vaga.setNumero(dto.numero());
        vaga.setLocalizacao(dto.localizacao());
        vaga.setTipo(dto.tipo());
        vaga.setApartamento(apt);
        vaga.setAtiva(true);

        return vagaRepository.save(vaga);
    }

    public List<Vaga> listarTodas() {
        return vagaRepository.findAll();
    }

    public List<Vaga> listarApenasAtivas() {
        return vagaRepository.findByAtiva(true);
    }

    public Vaga buscarPorId(String id) {
        return vagaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Vaga não encontrada com id: " + id));
    }

    public List<Vaga> buscarPorApartamento(UUID apartamentoId) {
        return vagaRepository.findByApartamentoId(apartamentoId);
    }

    public Vaga atualizar(String id, VagaRequestDTO dto) {
        Vaga vaga = buscarPorId(id);

        if (!vaga.getNumero().equals(dto.numero())) {
            validarVagaUnica(dto.numero());
        }

        Apartamento apt = apartamentoRepository.findById(dto.apartamentoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Apartamento não encontrado: " + dto.apartamentoId()));

        vaga.setNumero(dto.numero());
        vaga.setLocalizacao(dto.localizacao());
        vaga.setTipo(dto.tipo());
        vaga.setApartamento(apt);
        vaga.setAtualizadoEm(LocalDateTime.now());

        return vagaRepository.save(vaga);
    }

    public void desativar(String id) {
        Vaga vaga = buscarPorId(id);
        vaga.setAtiva(false);
        vaga.setAtualizadoEm(LocalDateTime.now());
        vagaRepository.save(vaga);
    }

    public void ativar(String id) {
        Vaga vaga = buscarPorId(id);
        vaga.setAtiva(true);
        vaga.setAtualizadoEm(LocalDateTime.now());
        vagaRepository.save(vaga);
    }

    private void validarVagaUnica(String numero) {
        vagaRepository.findByNumero(numero).ifPresent(v -> {
            throw new OperacaoInvalidaException("Já existe uma vaga cadastrada com o número: " + numero);
        });
    }
}
