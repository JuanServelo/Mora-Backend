package portaria.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portaria.client.AuthApiClient;
import portaria.dto.meuveiculo.*;
import portaria.exception.AcessoNegadoException;
import portaria.exception.OperacaoInvalidaException;
import portaria.exception.RecursoNaoEncontradoException;
import portaria.model.Vaga;
import portaria.model.Veiculo;
import portaria.model.VeiculoPessoa;
import portaria.model.enums.CategoriaVeiculo;
import portaria.model.enums.StatusAcesso;
import portaria.model.enums.TipoProprietario;
import portaria.repository.VagaRepository;
import portaria.repository.VeiculoPessoaRepository;
import portaria.repository.VeiculoRepository;
import portaria.security.CondominioUtils;
import portaria.util.PlacaUtils;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Gestão dos veículos pelo próprio morador.
 *
 * Todo método recebe a unidade vinda do JWT e nunca do corpo da requisição —
 * é o que garante que o morador só alcance o que é da unidade dele (RN-01).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class MeusVeiculosService {

    private final VeiculoRepository veiculoRepository;
    private final VeiculoPessoaRepository vinculoRepository;
    private final VagaRepository vagaRepository;
    private final AuthApiClient authApiClient;

    // ─── Listagem (RN-02) ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public MeusVeiculosResponseDTO listar(String unidadeId) {
        List<VeiculoPessoa> vinculos = vinculoRepository.findByUnidadeId(unidadeId);

        Map<String, List<VeiculoPessoa>> porVeiculo = new LinkedHashMap<>();
        for (VeiculoPessoa v : vinculos) {
            porVeiculo.computeIfAbsent(v.getVeiculoId(), k -> new ArrayList<>()).add(v);
        }

        List<Veiculo> veiculos = porVeiculo.isEmpty()
                ? List.of()
                : veiculoRepository.findAllById(porVeiculo.keySet());

        List<MeuVeiculoDTO> veiculosDTO = veiculos.stream()
                .map(v -> toDTO(v, porVeiculo.getOrDefault(v.getId(), List.of())))
                .sorted(Comparator.comparing(MeuVeiculoDTO::placa))
                .toList();

        return new MeusVeiculosResponseDTO(veiculosDTO, vagasDaUnidade(unidadeId, porVeiculo.keySet()));
    }

    private MeuVeiculoDTO toDTO(Veiculo v, List<VeiculoPessoa> vinculos) {
        List<PessoaVinculadaDTO> pessoas = vinculos.stream()
                .map(p -> new PessoaVinculadaDTO(p.getPessoaId(), p.getPessoaNome()))
                .sorted(Comparator.comparing(p -> p.nome() == null ? "" : p.nome()))
                .toList();

        return new MeuVeiculoDTO(
                v.getId(), v.getPlaca(), v.getModelo(), v.getCor(), v.getObs(),
                v.getVaga() != null ? v.getVaga().getId() : null,
                v.getVaga() != null ? v.getVaga().getNumero() : null,
                v.getStatus(), pessoas);
    }

    /** Situação das vagas: ocupada é quem está DENTRO agora, não quem tem vínculo (RN-07). */
    private List<VagaUnidadeDTO> vagasDaUnidade(String unidadeId, Set<String> veiculosDaUnidade) {
        return vagasAtivas(unidadeId).stream().map(vaga -> {
            Veiculo ocupante = veiculoRepository.findByVagaId(vaga.getId()).stream()
                    .filter(v -> v.getStatus() == StatusAcesso.DENTRO)
                    .findFirst().orElse(null);

            if (ocupante == null) {
                return new VagaUnidadeDTO(vaga.getId(), vaga.getNumero(), vaga.getLocalizacao(),
                        false, null, null, null);
            }
            // Veículo sem vínculo nesta unidade é visitante: somente leitura.
            boolean daUnidade = veiculosDaUnidade.contains(ocupante.getId());
            return new VagaUnidadeDTO(vaga.getId(), vaga.getNumero(), vaga.getLocalizacao(),
                    true, ocupante.getPlaca(), ocupante.getProprietarioNome(),
                    daUnidade ? "MORADOR" : "VISITANTE");
        }).toList();
    }

    private List<Vaga> vagasAtivas(String unidadeId) {
        try {
            return vagaRepository.findByApartamentoId(UUID.fromString(unidadeId)).stream()
                    .filter(Vaga::isAtiva)
                    .sorted(Comparator.comparing(Vaga::getNumero))
                    .toList();
        } catch (IllegalArgumentException e) {
            throw new OperacaoInvalidaException("Unidade inválida.");
        }
    }

    @Transactional(readOnly = true)
    public List<PessoaVinculadaDTO> pessoasDaUnidade() {
        return authApiClient.pessoasDaMinhaUnidade().stream()
                .map(p -> new PessoaVinculadaDTO(p.id(), p.nome()))
                .toList();
    }

    // ─── Cadastro (RN-03) ────────────────────────────────────────────────────

    public MeuVeiculoDTO cadastrar(String unidadeId, String autorId, CadastrarMeuVeiculoDTO dto) {
        String placa = PlacaUtils.normalizarEValidar(dto.placa());
        String condominioId = CondominioUtils.condominioIdEfetivo();

        veiculoRepository.findByPlaca(placa).ifPresent(existente -> {
            boolean naMinhaUnidade = vinculoRepository.findByVeiculoId(existente.getId()).stream()
                    .anyMatch(v -> unidadeId.equals(v.getUnidadeId()));
            if (naMinhaUnidade) {
                throw new OperacaoInvalidaException(
                        "O veículo " + placa + " já está cadastrado na sua unidade. "
                        + "Adicione seu vínculo a ele em vez de cadastrá-lo de novo.");
            }
            throw new OperacaoInvalidaException("O veículo " + placa + " está vinculado a outra unidade.");
        });

        List<AuthApiClient.PessoaUnidade> pessoas = resolverPessoas(dto.pessoaIds(), autorId);
        Vaga vaga = resolverVagaOpcional(dto.vagaId(), unidadeId);

        AuthApiClient.PessoaUnidade titular = pessoas.stream()
                .filter(p -> p.id().equals(autorId))
                .findFirst().orElse(pessoas.get(0));

        Veiculo veiculo = new Veiculo();
        veiculo.setPlaca(placa);
        veiculo.setModelo(dto.modelo());
        veiculo.setCor(dto.cor());
        veiculo.setObs(dto.obs());
        veiculo.setCategoria(CategoriaVeiculo.CARRO);
        veiculo.setTipoProprietario(TipoProprietario.MORADOR);
        // Campo legado de dono único: as telas da portaria ainda leem daqui.
        veiculo.setProprietarioId(titular.id());
        veiculo.setProprietarioNome(titular.nome());
        veiculo.setCondominioId(condominioId);
        veiculo.setVaga(vaga);
        veiculo.setStatus(StatusAcesso.SAIU);
        Veiculo salvo = veiculoRepository.save(veiculo);

        List<VeiculoPessoa> vinculos = pessoas.stream()
                .map(p -> novoVinculo(salvo.getId(), p, unidadeId, condominioId))
                .toList();
        vinculoRepository.saveAll(vinculos);

        return toDTO(salvo, vinculos);
    }

    private VeiculoPessoa novoVinculo(String veiculoId, AuthApiClient.PessoaUnidade pessoa,
                                      String unidadeId, String condominioId) {
        VeiculoPessoa vp = new VeiculoPessoa();
        vp.setVeiculoId(veiculoId);
        vp.setPessoaId(pessoa.id());
        vp.setPessoaNome(pessoa.nome());
        vp.setUnidadeId(unidadeId);
        vp.setCondominioId(condominioId);
        return vp;
    }

    /** Só pessoas da própria unidade entram como vínculo (RN-06). */
    private List<AuthApiClient.PessoaUnidade> resolverPessoas(List<String> pessoaIds, String autorId) {
        List<AuthApiClient.PessoaUnidade> daUnidade = authApiClient.pessoasDaMinhaUnidade();
        Map<String, AuthApiClient.PessoaUnidade> porId = new HashMap<>();
        daUnidade.forEach(p -> porId.put(p.id(), p));

        // Sem escolha explícita, vincula quem está cadastrando.
        List<String> ids = (pessoaIds == null || pessoaIds.isEmpty())
                ? List.of(autorId)
                : pessoaIds.stream().distinct().toList();

        List<AuthApiClient.PessoaUnidade> resolvidas = new ArrayList<>();
        for (String id : ids) {
            AuthApiClient.PessoaUnidade p = porId.get(id);
            if (p == null) {
                throw new AcessoNegadoException("Só é possível vincular pessoas da sua própria unidade.");
            }
            resolvidas.add(p);
        }
        if (resolvidas.isEmpty()) {
            throw new OperacaoInvalidaException("Vincule ao menos uma pessoa ao veículo.");
        }
        return resolvidas;
    }

    // ─── Edição (RN-04) ──────────────────────────────────────────────────────

    public MeuVeiculoDTO atualizar(String veiculoId, String unidadeId, AtualizarMeuVeiculoDTO dto) {
        Veiculo veiculo = exigirVeiculoDaUnidade(veiculoId, unidadeId);

        veiculo.setModelo(dto.modelo());
        veiculo.setCor(dto.cor());
        veiculo.setObs(dto.obs());

        String vagaAtualId = veiculo.getVaga() != null ? veiculo.getVaga().getId() : null;
        if (!Objects.equals(vagaAtualId, dto.vagaId())) {
            Vaga destino = resolverVagaOpcional(dto.vagaId(), unidadeId);
            // Com o carro dentro, trocar de vaga é mover a ocupação: o destino
            // precisa estar livre, senão duas ocupações coexistiriam.
            if (veiculo.getStatus() == StatusAcesso.DENTRO && destino != null) {
                boolean ocupada = veiculoRepository.findByVagaId(destino.getId()).stream()
                        .anyMatch(v -> v.getStatus() == StatusAcesso.DENTRO && !v.getId().equals(veiculoId));
                if (ocupada) {
                    throw new OperacaoInvalidaException(
                            "A vaga " + destino.getNumero() + " está ocupada no momento. "
                            + "Escolha outra vaga ou registre a saída do veículo que está nela.");
                }
            }
            veiculo.setVaga(destino);
        }

        veiculo.setAtualizadoEm(LocalDateTime.now());
        Veiculo salvo = veiculoRepository.save(veiculo);
        return toDTO(salvo, vinculoRepository.findByVeiculoId(veiculoId));
    }

    private Vaga resolverVagaOpcional(String vagaId, String unidadeId) {
        if (vagaId == null || vagaId.isBlank()) return null;
        Vaga vaga = vagaRepository.findById(vagaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Vaga não encontrada: " + vagaId));
        boolean daUnidade = vaga.getApartamento() != null
                && unidadeId.equals(vaga.getApartamento().getId().toString());
        if (!daUnidade) {
            throw new AcessoNegadoException("Esta vaga não pertence à sua unidade.");
        }
        return vaga;
    }

    // ─── Desvinculação (RN-05) ───────────────────────────────────────────────

    public DesvincularResultadoDTO desvincular(String veiculoId, String unidadeId, String pessoaId) {
        Veiculo veiculo = exigirVeiculoDaUnidade(veiculoId, unidadeId);

        if (veiculo.getStatus() == StatusAcesso.DENTRO) {
            throw new OperacaoInvalidaException(
                    "O veículo " + veiculo.getPlaca() + " está no condomínio. "
                    + "Registre a saída antes de desvinculá-lo.");
        }

        VeiculoPessoa meu = vinculoRepository.findByVeiculoIdAndPessoaId(veiculoId, pessoaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Você não está vinculado ao veículo " + veiculo.getPlaca() + "."));

        long total = vinculoRepository.countByVeiculoId(veiculoId);
        if (total > 1) {
            vinculoRepository.delete(meu);
            return new DesvincularResultadoDTO(
                    DesvincularResultadoDTO.VINCULO_REMOVIDO,
                    "Seu vínculo com o veículo " + veiculo.getPlaca()
                            + " foi removido. Ele continua cadastrado para as outras pessoas da unidade.");
        }

        // Último vínculo: o veículo deixa o cadastro e a vaga fica livre.
        // O histórico é preservado — movimentacoes_veiculo guarda placa e nome,
        // e referencia veiculo_id sem chave estrangeira.
        vinculoRepository.delete(meu);
        veiculoRepository.delete(veiculo);
        return new DesvincularResultadoDTO(
                DesvincularResultadoDTO.VEICULO_REMOVIDO,
                "O veículo " + veiculo.getPlaca()
                        + " foi removido do cadastro da unidade e a vaga foi liberada.");
    }

    // ─── Pessoas vinculadas (RN-06) ──────────────────────────────────────────

    public MeuVeiculoDTO vincularPessoa(String veiculoId, String unidadeId, String pessoaId) {
        Veiculo veiculo = exigirVeiculoDaUnidade(veiculoId, unidadeId);

        if (vinculoRepository.existsByVeiculoIdAndPessoaId(veiculoId, pessoaId)) {
            throw new OperacaoInvalidaException("Esta pessoa já está vinculada ao veículo.");
        }

        AuthApiClient.PessoaUnidade pessoa = authApiClient.pessoasDaMinhaUnidade().stream()
                .filter(p -> p.id().equals(pessoaId))
                .findFirst()
                .orElseThrow(() -> new AcessoNegadoException(
                        "Só é possível vincular pessoas da sua própria unidade."));

        vinculoRepository.save(novoVinculo(veiculoId, pessoa, unidadeId, veiculo.getCondominioId()));
        return toDTO(veiculo, vinculoRepository.findByVeiculoId(veiculoId));
    }

    public MeuVeiculoDTO desvincularPessoa(String veiculoId, String unidadeId, String pessoaId) {
        Veiculo veiculo = exigirVeiculoDaUnidade(veiculoId, unidadeId);

        VeiculoPessoa vinculo = vinculoRepository.findByVeiculoIdAndPessoaId(veiculoId, pessoaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Vínculo não encontrado."));

        // Remover o último aqui deixaria o veículo órfão; para isso existe a RN-05.
        if (vinculoRepository.countByVeiculoId(veiculoId) <= 1) {
            throw new OperacaoInvalidaException(
                    "Este é o último vínculo do veículo. Use \"desvincular veículo\" para retirá-lo da unidade.");
        }

        vinculoRepository.delete(vinculo);
        return toDTO(veiculo, vinculoRepository.findByVeiculoId(veiculoId));
    }

    // ─── Escopo (RN-01) ──────────────────────────────────────────────────────

    /** 403 para qualquer veículo que não tenha vínculo com a unidade do chamador. */
    private Veiculo exigirVeiculoDaUnidade(String veiculoId, String unidadeId) {
        Veiculo veiculo = veiculoRepository.findById(veiculoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Veículo não encontrado: " + veiculoId));
        boolean daUnidade = vinculoRepository.findByVeiculoId(veiculoId).stream()
                .anyMatch(v -> unidadeId.equals(v.getUnidadeId()));
        if (!daUnidade) {
            throw new AcessoNegadoException("Este veículo não pertence à sua unidade.");
        }
        return veiculo;
    }
}
