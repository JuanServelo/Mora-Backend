package portaria.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@MappedSuperclass
public abstract class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    protected String id;

    @NotBlank(message = "Nome é obrigatório")
    protected String nome;

    @NotBlank(message = "CPF é obrigatório")
    @Column(unique = true)
    protected String cpf;

    @Email(message = "Email inválido")
    protected String email;

    protected String telefone;

    protected boolean ativo = true;

    @Column(name = "criado_em")
    protected LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "atualizado_em")
    protected LocalDateTime atualizadoEm = LocalDateTime.now();
}
