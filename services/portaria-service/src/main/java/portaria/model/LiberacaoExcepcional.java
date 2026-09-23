package portaria.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import portaria.model.enums.StatusPreAutorizacao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Autorização do síndico para o funcionário entrar fora do turno (RN-05).
 *
 * Reaproveita os quatro estados da pré-liberação, inclusive a derivação de
 * EXPIRADA na leitura — a regra de ciclo de vida é a mesma e duplicá-la só
 * criaria duas versões para divergir.
 *
 * Sobrepõe APENAS a validação de turno: nunca libera quem está suspenso,
 * afastado ou demitido.
 */
@Data
@Entity
@Table(name = "liberacoes_excepcionais")
public class LiberacaoExcepcional {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /** Id do funcionário no auth-api. */
    @NotBlank
    @Column(name = "auth_user_id", nullable = false)
    private String authUserId;

    @Column(name = "funcionario_nome")
    private String funcionarioNome;

    @Column(name = "`condominioId`")
    private String condominioId;

    @NotNull
    private LocalDate data;

    @NotNull
    @Column(name = "hora_inicio")
    private LocalTime horaInicio;

    @NotNull
    @Column(name = "hora_fim")
    private LocalTime horaFim;

    @NotBlank
    @Column(length = 500)
    private String motivo;

    @Column(length = 500)
    private String observacoes;

    @Enumerated(EnumType.STRING)
    private StatusPreAutorizacao status = StatusPreAutorizacao.AGUARDANDO;

    @Column(name = "autorizado_por_id")
    private String autorizadoPorId;

    @Column(name = "autorizado_por_nome")
    private String autorizadoPorNome;

    @Column(name = "autorizado_em")
    private LocalDateTime autorizadoEm = LocalDateTime.now();

    @Column(name = "utilizada_em")
    private LocalDateTime utilizadaEm;

    /** Janela concreta; fim anterior ao início vira o dia, como nos turnos. */
    public LocalDateTime inicioEm() {
        return data.atTime(horaInicio);
    }

    public LocalDateTime fimEm() {
        return horaFim.isAfter(horaInicio)
                ? data.atTime(horaFim)
                : data.plusDays(1).atTime(horaFim);
    }

    public StatusPreAutorizacao statusEfetivo() {
        if (status == StatusPreAutorizacao.AGUARDANDO && LocalDateTime.now().isAfter(fimEm())) {
            return StatusPreAutorizacao.EXPIRADA;
        }
        return status;
    }

    public boolean vigenteEm(LocalDateTime instante) {
        return status == StatusPreAutorizacao.AGUARDANDO
                && !instante.isBefore(inicioEm())
                && !instante.isAfter(fimEm());
    }
}
