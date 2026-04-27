package portaria.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import portaria.dto.CriarUsuarioDTO;
import portaria.exception.OperacaoInvalidaException;
import portaria.exception.RecursoNaoEncontradoException;
import portaria.model.Morador;
import portaria.model.Visitante;
import portaria.model.Funcionario;
import portaria.model.Apartamento;
import portaria.model.Bloco;
import portaria.repository.MoradorRepository;
import portaria.repository.VisitanteRepository;
import portaria.repository.FuncionarioRepository;
import portaria.repository.ApartamentoRepository;
import portaria.repository.BlocoRepository;
import portaria.model.enums.TipoProprietario;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final MoradorService moradorService;
    private final VisitanteService visitanteService;
    private final FuncionarioService funcionarioService;
    private final MoradorRepository moradorRepository;
    private final VisitanteRepository visitanteRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final ApartamentoRepository apartamentoRepository;
    private final BlocoRepository blocoRepository;

    public Object criarUsuario(CriarUsuarioDTO dto) {
        validarCpfUnico(dto.getCpf());

        switch (dto.getTipoUsuario()) {
            case MORADOR:
                return criarMorador(dto);
            case VISITANTE:
                return criarVisitante(dto);
            case FUNCIONARIO:
                return criarFuncionario(dto);
            default:
                throw new OperacaoInvalidaException("Tipo de usuário inválido: " + dto.getTipoUsuario());
        }
    }

    private Morador criarMorador(CriarUsuarioDTO dto) {
        Apartamento apartamento = null;
        Bloco bloco = null;

        if (dto.getApartamentoId() != null) {
            try {
                apartamento = apartamentoRepository.findById(UUID.fromString(dto.getApartamentoId()))
                        .orElseThrow(() -> new RecursoNaoEncontradoException("Apartamento não encontrado"));
            } catch (IllegalArgumentException e) {
                throw new OperacaoInvalidaException("ID de apartamento inválido");
            }
        }

        if (dto.getBlocoId() != null) {
            try {
                bloco = blocoRepository.findById(UUID.fromString(dto.getBlocoId()))
                        .orElseThrow(() -> new RecursoNaoEncontradoException("Bloco não encontrado"));
            } catch (IllegalArgumentException e) {
                throw new OperacaoInvalidaException("ID de bloco inválido");
            }
        }

        Morador morador = new Morador();
        morador.setNome(dto.getNome());
        morador.setCpf(dto.getCpf());
        morador.setEmail(dto.getEmail());
        morador.setTelefone(dto.getTelefone());
        morador.setApartamento(apartamento);
        morador.setBloco(bloco);
        morador.setAtivo(true);

        return moradorRepository.save(morador);
    }

    private Visitante criarVisitante(CriarUsuarioDTO dto) {
        Apartamento apartamento = null;
        Bloco bloco = null;

        if (dto.getApartamentoId() != null) {
            try {
                apartamento = apartamentoRepository.findById(UUID.fromString(dto.getApartamentoId()))
                        .orElseThrow(() -> new RecursoNaoEncontradoException("Apartamento não encontrado"));
            } catch (IllegalArgumentException e) {
                throw new OperacaoInvalidaException("ID de apartamento inválido");
            }
        }

        if (dto.getBlocoId() != null) {
            try {
                bloco = blocoRepository.findById(UUID.fromString(dto.getBlocoId()))
                        .orElseThrow(() -> new RecursoNaoEncontradoException("Bloco não encontrado"));
            } catch (IllegalArgumentException e) {
                throw new OperacaoInvalidaException("ID de bloco inválido");
            }
        }

        Visitante visitante = new Visitante();
        visitante.setNome(dto.getNome());
        visitante.setCpf(dto.getCpf());
        visitante.setEmail(dto.getEmail());
        visitante.setTelefone(dto.getTelefone());
        visitante.setDocumento(dto.getDocumento());
        visitante.setMotivoVisita(dto.getMotivoVisita());
        visitante.setApartamento(apartamento);
        visitante.setBloco(bloco);
        visitante.setAtivo(true);

        return visitanteRepository.save(visitante);
    }

    private Funcionario criarFuncionario(CriarUsuarioDTO dto) {
        Funcionario funcionario = new Funcionario();
        funcionario.setNome(dto.getNome());
        funcionario.setCpf(dto.getCpf());
        funcionario.setEmail(dto.getEmail());
        funcionario.setTelefone(dto.getTelefone());
        funcionario.setCargo(dto.getCargo());
        funcionario.setMatricula(dto.getMatricula());
        funcionario.setHorarioEntrada(dto.getHorarioEntrada());
        funcionario.setHorarioSaida(dto.getHorarioSaida());
        funcionario.setAtivo(true);

        return funcionarioRepository.save(funcionario);
    }

    private void validarCpfUnico(String cpf) {
        if (moradorRepository.findByCpf(cpf).isPresent()) {
            throw new OperacaoInvalidaException("CPF já cadastrado como Morador");
        }
        if (visitanteRepository.findByCpf(cpf).isPresent()) {
            throw new OperacaoInvalidaException("CPF já cadastrado como Visitante");
        }
        if (funcionarioRepository.findByCpf(cpf).isPresent()) {
            throw new OperacaoInvalidaException("CPF já cadastrado como Funcionário");
        }
    }
}
