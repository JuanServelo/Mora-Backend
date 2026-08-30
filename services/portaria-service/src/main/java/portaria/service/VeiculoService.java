package portaria.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portaria.dto.AlterarVagaDTO;
import portaria.dto.CriarVeiculoDTO;
import portaria.dto.VeiculoResponseDTO;
import portaria.exception.AcessoNegadoException;
import portaria.exception.OperacaoInvalidaException;
import portaria.exception.RecursoNaoEncontradoException;
import portaria.model.Morador;
import portaria.model.Vaga;
import portaria.model.Veiculo;
import portaria.model.enums.CategoriaVeiculo;
import portaria.model.enums.StatusAcesso;
import portaria.model.enums.TipoProprietario;
import portaria.repository.MoradorRepository;
import portaria.repository.VagaRepository;
import portaria.repository.VeiculoRepository;
import portaria.security.AuthContext;
import portaria.security.CondominioUtils;
import portaria.security.JwtClaims;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class VeiculoService {

    private final VeiculoRepository veiculoRepository;
    private final MoradorRepository moradorRepository;
    private final VagaRepository vagaRepository;

    // ─── Grupos de perfis ──────────────────────────────────────────────────────

    // Perfis emitidos pelo auth-api no claim "perfil" do JWT.
    private static final Set<String> ADMIN_PERFIS = Set.of(
            "ADMIN_GERAL", "ADMIN_SINDICO"
    );
    private static final Set<String> MORADOR_PERFIS = Set.of(
            "MORADOR", "DONO_ALUGUEL"
    );

    private boolean isAdmin(String perfil) { return ADMIN_PERFIS.contains(perfil); }
    private boolean isPorteiro(String perfil) { return "PORTEIRO".equals(perfil); }
    private boolean isMorador(String perfil) { return MORADOR_PERFIS.contains(perfil); }

    // ─── Helpers de autenticação ───────────────────────────────────────────────

    private JwtClaims authRequired() {
        JwtClaims claims = AuthContext.get();
        if (claims == null || claims.perfil() == null) {
            throw new AcessoNegadoException("Autenticação necessária.");
        }
        return claims;
    }

    private Optional<Morador> moradorDoClaims(JwtClaims claims) {
        if (claims.email() == null) return Optional.empty();
        return moradorRepository.findByEmail(claims.email());
    }

    // ─── Validação de permissão para edição (atualizar / alterar vaga) ─────────

    private void validarPermissaoEdicao(JwtClaims claims, Veiculo veiculo) {
        String perfil = claims.perfil();

        if (isAdmin(perfil)) return;

        boolean isServico = veiculo.isVeiculoServico();

        if (isPorteiro(perfil)) {
            if (!isServico) {
                throw new AcessoNegadoException(
                        "Porteiros não podem alterar dados de veículos de moradores. " +
                        "Apenas o registro de entrada e saída é permitido.");
            }
            return;
        }

        if (isMorador(perfil)) {
            if (isServico) {
                throw new AcessoNegadoException("Moradores não podem alterar veículos de serviço.");
            }
            Morador atual = moradorDoClaims(claims)
                    .orElseThrow(() -> new AcessoNegadoException(
                            "Registro de morador não encontrado para o usuário autenticado."));
            if (!atual.getId().equals(veiculo.getProprietarioId())) {
                throw new AcessoNegadoException("Você só pode alterar seus próprios veículos.");
            }
            return;
        }

        throw new AcessoNegadoException("Perfil não autorizado para esta operação.");
    }

    // ─── Operações públicas ────────────────────────────────────────────────────

    public VeiculoResponseDTO cadastrar(CriarVeiculoDTO dto) {
        JwtClaims claims = authRequired();
        String perfil = claims.perfil();

        if (dto.getCategoria() == CategoriaVeiculo.VEICULO_SERVICO) {
            // Somente porteiro ou admin podem cadastrar veículos de serviço
            if (!isPorteiro(perfil) && !isAdmin(perfil)) {
                throw new AcessoNegadoException(
                        "Apenas porteiros e administradores podem cadastrar veículos de serviço.");
            }
        } else {
            // CARRO / MOTO: porteiro não pode cadastrar
            if (isPorteiro(perfil)) {
                throw new AcessoNegadoException(
                        "Porteiros não podem cadastrar veículos de moradores. " +
                        "O próprio morador deve realizar o cadastro.");
            }
            // Morador só pode cadastrar para si mesmo
            if (isMorador(perfil)) {
                Morador atual = moradorDoClaims(claims)
                        .orElseThrow(() -> new AcessoNegadoException(
                                "Registro de morador não encontrado para o usuário autenticado."));
                if (!atual.getId().equals(dto.getProprietarioId())) {
                    throw new AcessoNegadoException("Moradores só podem cadastrar veículos para si mesmos.");
                }
            }
        }

        validarPlacaUnica(dto.getPlaca(), null);

        Veiculo veiculo = new Veiculo();
        veiculo.setPlaca(dto.getPlaca().toUpperCase().trim());
        veiculo.setModelo(dto.getModelo());
        veiculo.setCategoria(dto.getCategoria());
        veiculo.setStatus(StatusAcesso.SAIU);
        veiculo.setCriadoEm(LocalDateTime.now());
        veiculo.setAtualizadoEm(LocalDateTime.now());
        if (claims.condominioId() != null && !"default".equals(claims.condominioId())) {
            veiculo.setCondominioId(claims.condominioId());
        }

        if (dto.getCategoria() != CategoriaVeiculo.VEICULO_SERVICO) {
            if (dto.getProprietarioId() == null || dto.getProprietarioId().isBlank()) {
                throw new OperacaoInvalidaException("Proprietário é obrigatório para veículos do tipo " + dto.getCategoria() + ".");
            }
            if (dto.getVagaId() == null || dto.getVagaId().isBlank()) {
                throw new OperacaoInvalidaException("Vaga é obrigatória para veículos do tipo " + dto.getCategoria() + ".");
            }

            Morador morador = moradorRepository.findByIdComApartamento(dto.getProprietarioId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Morador não encontrado: " + dto.getProprietarioId()));

            Vaga vaga = buscarVagaAtiva(dto.getVagaId());
            validarMesmoApartamento(morador, vaga);

            veiculo.setProprietarioId(dto.getProprietarioId());
            veiculo.setTipoProprietario(TipoProprietario.MORADOR);
            veiculo.setVaga(vaga);
        }

        return VeiculoResponseDTO.fromEntity(veiculoRepository.save(veiculo));
    }

    public VeiculoResponseDTO atualizar(String id, CriarVeiculoDTO dto) {
        JwtClaims claims = authRequired();
        Veiculo veiculo = buscarEntidade(id);
        validarPermissaoEdicao(claims, veiculo);
        validarPlacaUnica(dto.getPlaca(), id);

        veiculo.setPlaca(dto.getPlaca().toUpperCase().trim());
        veiculo.setModelo(dto.getModelo());
        veiculo.setAtualizadoEm(LocalDateTime.now());

        if (dto.getCategoria() != CategoriaVeiculo.VEICULO_SERVICO) {
            if (dto.getProprietarioId() != null && !dto.getProprietarioId().isBlank()) {
                Morador morador = moradorRepository.findByIdComApartamento(dto.getProprietarioId())
                        .orElseThrow(() -> new RecursoNaoEncontradoException("Morador não encontrado: " + dto.getProprietarioId()));

                Vaga vaga = veiculo.getVaga();
                if (dto.getVagaId() != null && !dto.getVagaId().isBlank()) {
                    vaga = buscarVagaAtiva(dto.getVagaId());
                }
                if (vaga == null) {
                    throw new OperacaoInvalidaException("Vaga é obrigatória para veículos do tipo " + dto.getCategoria() + ".");
                }
                validarMesmoApartamento(morador, vaga);

                veiculo.setProprietarioId(dto.getProprietarioId());
                veiculo.setTipoProprietario(TipoProprietario.MORADOR);
                veiculo.setVaga(vaga);
            }
        }

        return VeiculoResponseDTO.fromEntity(veiculoRepository.save(veiculo));
    }

    public VeiculoResponseDTO alterarVaga(String veiculoId, AlterarVagaDTO dto) {
        JwtClaims claims = authRequired();
        Veiculo veiculo = buscarEntidade(veiculoId);
        validarPermissaoEdicao(claims, veiculo);

        if (dto.getVagaId() == null || dto.getVagaId().isBlank()) {
            if (!veiculo.isVeiculoServico()) {
                throw new OperacaoInvalidaException("Não é possível desvincular a vaga de um veículo do tipo " + veiculo.getCategoria() + ".");
            }
            veiculo.setVaga(null);
        } else {
            Vaga novaVaga = buscarVagaAtiva(dto.getVagaId());

            if (!veiculo.isVeiculoServico()) {
                Morador morador = moradorRepository.findByIdComApartamento(veiculo.getProprietarioId())
                        .orElseThrow(() -> new RecursoNaoEncontradoException("Morador do veículo não encontrado."));
                validarMesmoApartamento(morador, novaVaga);
            }

            veiculo.setVaga(novaVaga);
        }

        veiculo.setAtualizadoEm(LocalDateTime.now());
        return VeiculoResponseDTO.fromEntity(veiculoRepository.save(veiculo));
    }

    public VeiculoResponseDTO registrarEntrada(String id) {
        JwtClaims claims = authRequired();
        if (!isAdmin(claims.perfil()) && !isPorteiro(claims.perfil())) {
            throw new AcessoNegadoException("Apenas porteiros podem registrar a entrada de veículos.");
        }

        Veiculo veiculo = buscarEntidade(id);

        if (veiculo.getStatus() == StatusAcesso.DENTRO) {
            throw new OperacaoInvalidaException("Veículo " + veiculo.getPlaca() + " já está registrado como DENTRO.");
        }
        if (!veiculo.isVeiculoServico() && veiculo.getVaga() == null) {
            throw new OperacaoInvalidaException(
                    "Veículo sem vaga vinculada. Vincule uma vaga antes de registrar a entrada.");
        }

        veiculo.setDataEntrada(LocalDateTime.now());
        veiculo.setStatus(StatusAcesso.DENTRO);
        veiculo.setAtualizadoEm(LocalDateTime.now());
        return VeiculoResponseDTO.fromEntity(veiculoRepository.save(veiculo));
    }

    public VeiculoResponseDTO registrarEntradaPorPlaca(String placa) {
        JwtClaims claims = authRequired();
        if (!isAdmin(claims.perfil()) && !isPorteiro(claims.perfil())) {
            throw new AcessoNegadoException("Apenas porteiros podem registrar a entrada de veículos.");
        }

        Veiculo veiculo = veiculoRepository.findByPlaca(placa.toUpperCase().trim())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Veículo com placa " + placa + " não encontrado."));
        return registrarEntrada(veiculo.getId());
    }

    public VeiculoResponseDTO registrarSaida(String id) {
        JwtClaims claims = authRequired();
        if (!isAdmin(claims.perfil()) && !isPorteiro(claims.perfil())) {
            throw new AcessoNegadoException("Apenas porteiros podem registrar a saída de veículos.");
        }

        Veiculo veiculo = buscarEntidade(id);
        if (veiculo.getStatus() != StatusAcesso.DENTRO) {
            throw new OperacaoInvalidaException("Veículo não está registrado como DENTRO do condomínio.");
        }
        veiculo.setDataSaida(LocalDateTime.now());
        veiculo.setStatus(StatusAcesso.SAIU);
        veiculo.setAtualizadoEm(LocalDateTime.now());
        return VeiculoResponseDTO.fromEntity(veiculoRepository.save(veiculo));
    }

    public List<VeiculoResponseDTO> listarTodos() {
        JwtClaims claims = authRequired();
        String condominioId = CondominioUtils.condominioIdEfetivo();
        if (isMorador(claims.perfil())) {
            return moradorDoClaims(claims)
                    .map(m -> veiculoRepository.findByProprietarioId(m.getId())
                            .stream().map(VeiculoResponseDTO::fromEntity).toList())
                    .orElse(List.of());
        }
        if (condominioId != null) {
            return veiculoRepository.findByCondominioId(condominioId).stream()
                    .map(VeiculoResponseDTO::fromEntity).toList();
        }
        return veiculoRepository.findAll().stream().map(VeiculoResponseDTO::fromEntity).toList();
    }

    public List<VeiculoResponseDTO> listarDentro() {
        JwtClaims claims = authRequired();
        String condominioId = CondominioUtils.condominioIdEfetivo();
        if (isMorador(claims.perfil())) {
            return moradorDoClaims(claims)
                    .map(m -> veiculoRepository.findByProprietarioId(m.getId())
                            .stream()
                            .filter(v -> v.getStatus() == StatusAcesso.DENTRO)
                            .map(VeiculoResponseDTO::fromEntity).toList())
                    .orElse(List.of());
        }
        if (condominioId != null) {
            return veiculoRepository.findByCondominioIdAndStatus(condominioId, StatusAcesso.DENTRO).stream()
                    .map(VeiculoResponseDTO::fromEntity).toList();
        }
        return veiculoRepository.findByStatus(StatusAcesso.DENTRO).stream().map(VeiculoResponseDTO::fromEntity).toList();
    }

    public List<VeiculoResponseDTO> listarPorProprietario(String proprietarioId) {
        JwtClaims claims = authRequired();
        // Moradores só podem listar seus próprios veículos
        if (isMorador(claims.perfil())) {
            Morador atual = moradorDoClaims(claims)
                    .orElseThrow(() -> new AcessoNegadoException(
                            "Registro de morador não encontrado para o usuário autenticado."));
            if (!atual.getId().equals(proprietarioId)) {
                throw new AcessoNegadoException("Você só pode consultar seus próprios veículos.");
            }
        }
        return veiculoRepository.findByProprietarioId(proprietarioId).stream()
                .map(VeiculoResponseDTO::fromEntity).toList();
    }

    public VeiculoResponseDTO buscarPorId(String id) {
        authRequired();
        return VeiculoResponseDTO.fromEntity(buscarEntidade(id));
    }

    // ─── Helpers internos ──────────────────────────────────────────────────────

    private Veiculo buscarEntidade(String id) {
        return veiculoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Veículo não encontrado com id: " + id));
    }

    private Vaga buscarVagaAtiva(String vagaId) {
        Vaga vaga = vagaRepository.findById(vagaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Vaga não encontrada: " + vagaId));
        if (!vaga.isAtiva()) {
            throw new OperacaoInvalidaException("A vaga " + vaga.getNumero() + " está desativada.");
        }
        return vaga;
    }

    private void validarMesmoApartamento(Morador morador, Vaga vaga) {
        if (morador.getApartamento() == null) {
            throw new OperacaoInvalidaException("O morador não está vinculado a nenhum apartamento.");
        }
        if (vaga.getApartamento() == null) {
            throw new OperacaoInvalidaException("A vaga não está vinculada a nenhum apartamento.");
        }
        UUID aptMorador = morador.getApartamento().getId();
        UUID aptVaga = vaga.getApartamento().getId();
        if (!aptMorador.equals(aptVaga)) {
            throw new OperacaoInvalidaException(
                    "A vaga selecionada pertence a um apartamento diferente do proprietário do veículo.");
        }
    }

    private void validarPlacaUnica(String placa, String veiculoIdAtual) {
        veiculoRepository.findByPlaca(placa.toUpperCase().trim()).ifPresent(v -> {
            if (!v.getId().equals(veiculoIdAtual)) {
                throw new OperacaoInvalidaException("Já existe um veículo cadastrado com a placa: " + placa);
            }
        });
    }
}
