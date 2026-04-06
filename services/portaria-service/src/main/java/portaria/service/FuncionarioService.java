package portaria.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import portaria.exception.OperacaoInvalidaException;
import portaria.exception.RecursoNaoEncontradoException;
import portaria.model.Funcionario;
import portaria.repository.FuncionarioRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FuncionarioService {

    private final FuncionarioRepository funcionarioRepository;

    public Funcionario cadastrar(Funcionario funcionario) {
        funcionarioRepository.findByCpf(funcionario.getCpf()).ifPresent(f -> {
            throw new OperacaoInvalidaException("Já existe um funcionário cadastrado com o CPF: " + funcionario.getCpf());
        });
        return funcionarioRepository.save(funcionario);
    }

    public List<Funcionario> listarTodos() {
        return funcionarioRepository.findAll();
    }

    public List<Funcionario> listarAtivos() {
        return funcionarioRepository.findByAtivo(true);
    }

    public Funcionario buscarPorId(String id) {
        return funcionarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Funcionário não encontrado com id: " + id));
    }

    public Funcionario atualizar(String id, Funcionario dados) {
        Funcionario funcionario = buscarPorId(id);
        funcionario.setNome(dados.getNome());
        funcionario.setCargo(dados.getCargo());
        funcionario.setMatricula(dados.getMatricula());
        funcionario.setTelefone(dados.getTelefone());
        return funcionarioRepository.save(funcionario);
    }

    public void desativar(String id) {
        Funcionario funcionario = buscarPorId(id);
        funcionario.setAtivo(false);
        funcionarioRepository.save(funcionario);
    }
}
