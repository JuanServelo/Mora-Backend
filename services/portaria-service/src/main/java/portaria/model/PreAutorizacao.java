package portaria.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import portaria.model.enums.StatusPreAutorizacao;
import portaria.model.enums.TipoVisita;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "pre_autorizacoes")
public class PreAutorizacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /** Id do usuário no auth-api, que é inteiro — não UUID. */
    @NotNull
    @Column(name = "morador_id")
    private String moradorId;

    /**
     * Unidade responsável pela visita; usada para escopo do morador.
     * Nula na pré-entrada de terceiro feita pelo síndico, que não tem unidade —
     * o responsável nesse caso é quem criou a liberação.
     */
    @Column(name = "unidade_id")
    private String unidadeId;

    /**
     * VISITANTE (morador pré-libera) ou TERCEIRO (síndico pré-libera).
     * Mesmo modelo para os dois: um terceiro registro de liberação só criaria
     * mais uma cópia das mesmas regras para divergir.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_pessoa")
    private TipoVisita tipoPessoa = TipoVisita.VISITA;

    /** Empresa/prestador — obrigatória quando é terceiro. */
    @Column(name = "empresa")
    private String empresa;

    @Column(name = "`condominioId`")
    private String condominioId;

    @NotBlank
    @Column(name = "nome_visitante")
    private String nomeVisitante;

    @Column(name = "cpf_visitante", length = 14)
    private String cpfVisitante;

    @Column(name = "telefone_visitante", length = 20)
    private String telefoneVisitante;

    /**
     * Visitante já cadastrado que este CPF reaproveita.
     *
     * Guardado só para a portaria concluir a entrada sem duplicar pessoa; o
     * morador nunca vê este dado, para não transformar a tela numa consulta
     * à base de visitantes do condomínio (RN-06).
     */
    @Column(name = "visitante_id")
    private String visitanteId;

    /** Preenchida quando a pré-liberação inclui veículo (RN-08). */
    @Column(name = "placa_veiculo", length = 10)
    private String placaVeiculo;

    @Column(name = "modelo_veiculo")
    private String modeloVeiculo;

    @Column(name = "cor_veiculo")
    private String corVeiculo;

    @NotNull
    @Column(name = "validade_inicio")
    private LocalDate validadeInicio;

    @NotNull
    @Column(name = "validade_fim")
    private LocalDate validadeFim;

    @Column(length = 500)
    private String observacoes;

    @Enumerated(EnumType.STRING)
    private StatusPreAutorizacao status = StatusPreAutorizacao.AGUARDANDO;

    /** Momento em que a portaria usou esta pré-liberação numa entrada. */
    @Column(name = "utilizada_em")
    private LocalDateTime utilizadaEm;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm = LocalDateTime.now();

    /** Status efetivo: AGUARDANDO vencida vira EXPIRADA sem precisar de job. */
    public StatusPreAutorizacao statusEfetivo() {
        if (status == StatusPreAutorizacao.AGUARDANDO && validadeFim != null
                && validadeFim.isBefore(LocalDate.now())) {
            return StatusPreAutorizacao.EXPIRADA;
        }
        return status;
    }

    /** Só uma pré-liberação aguardando e dentro da validade pode ser usada. */
    public boolean utilizavelHoje() {
        LocalDate hoje = LocalDate.now();
        return status == StatusPreAutorizacao.AGUARDANDO
                && validadeInicio != null && !validadeInicio.isAfter(hoje)
                && validadeFim != null && !validadeFim.isBefore(hoje);
    }
}
