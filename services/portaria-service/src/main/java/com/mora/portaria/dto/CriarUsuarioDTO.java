package com.mora.portaria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.mora.portaria.enums.TipoProprietario;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CriarUsuarioDTO {

    @NotNull(message = "Tipo de usuário é obrigatório")
    private TipoProprietario tipoUsuario;

    @NotBlank(message = "Nome é obrigatório")
    private String nome;

    @NotBlank(message = "CPF é obrigatório")
    private String cpf;

    @Email(message = "Email inválido")
    private String email;

    private String telefone;

    // Campos para Morador
    private String apartamentoId;
    private String blocoId;

    // Campos para Visitante
    private String documento;
    private String motivoVisita;

    // Campos para Funcionário
    private String cargo;
    private String matricula;
    private LocalTime horarioEntrada;
    private LocalTime horarioSaida;
}

