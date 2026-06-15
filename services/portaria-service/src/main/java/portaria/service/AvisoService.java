package portaria.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import portaria.dto.AvisoRequestDTO;
import portaria.exception.OperacaoInvalidaException;
import portaria.exception.RecursoNaoEncontradoException;
import portaria.model.Aviso;
import portaria.repository.AvisoRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AvisoService {

    private final AvisoRepository repository;

    public Aviso criar(AvisoRequestDTO request) {
        validarPeriodo(request);
        Aviso aviso = new Aviso();
        aplicar(aviso, request);
        aviso.setCriadoEm(LocalDateTime.now());
        aviso.setAtualizadoEm(LocalDateTime.now());
        return repository.save(aviso);
    }

    public Aviso atualizar(UUID id, AvisoRequestDTO request) {
        validarPeriodo(request);
        Aviso aviso = buscarPorId(id);
        aplicar(aviso, request);
        aviso.setAtualizadoEm(LocalDateTime.now());
        return repository.save(aviso);
    }

    public Aviso encerrar(UUID id) {
        Aviso aviso = buscarPorId(id);
        aviso.setPublicado(false);
        aviso.setAtualizadoEm(LocalDateTime.now());
        return repository.save(aviso);
    }

    public void excluir(UUID id) {
        buscarPorId(id);
        repository.deleteById(id);
    }

    public List<Aviso> listarPorCondominio(String condominioId) {
        return repository.findByCondominioIdOrderByCriadoEmDesc(condominioId);
    }

    public List<Aviso> listarAtivos(String condominioId) {
        return repository.findAtivos(condominioId, LocalDate.now());
    }

    public Aviso buscarPorId(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aviso não encontrado com id: " + id));
    }

    private void aplicar(Aviso aviso, AvisoRequestDTO request) {
        aviso.setTitulo(request.getTitulo());
        aviso.setMensagem(request.getMensagem());
        aviso.setDataInicio(parseData(request.getDataInicio(), "Data inicial"));
        aviso.setDataFim(parseData(request.getDataFim(), "Data final"));
        aviso.setPublicoAlvo(request.getPublicoAlvo());
        aviso.setCondominioId(request.getCondominioId());
        aviso.setAutor(request.getAutor());
        aviso.setPublicado(request.isPublicado());
    }

    private void validarPeriodo(AvisoRequestDTO request) {
        LocalDate inicio = parseData(request.getDataInicio(), "Data inicial");
        LocalDate fim = parseData(request.getDataFim(), "Data final");
        if (inicio.isAfter(fim)) {
            throw new OperacaoInvalidaException("Informe um período de exibição válido.");
        }
    }

    private LocalDate parseData(String valor, String campo) {
        try {
            return LocalDate.parse(valor);
        } catch (Exception e) {
            throw new OperacaoInvalidaException(campo + " inválida.");
        }
    }
}
