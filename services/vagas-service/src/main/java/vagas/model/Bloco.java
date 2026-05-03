package vagas.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;
import java.util.List;

@Data
@Entity
@Table(name = "blocos")
public class Bloco {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String nome;
    private boolean ativo = true;

    @OneToMany(mappedBy = "bloco", fetch = FetchType.LAZY)
    private List<Apartamento> apartamentos;
}
