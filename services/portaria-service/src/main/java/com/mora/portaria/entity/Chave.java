package com.mora.portaria.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import com.mora.portaria.enums.TipoResponsavel;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "chaves")
public class Chave {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotBlank(message = "Nome da chave é obrigatório")
    @Column(name = "nome_chave")
    private String nomeChave;

    @Column(name = "responsavel_id")
    private String responsavelId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_responsavel")
    private TipoResponsavel tipoResponsavel;

    @Column(name = "nome_responsavel")
    private String nomeResponsavel;

    private LocalDateTime retirada;

    private LocalDateTime devolucao;

    private boolean disponivel = true;
}

