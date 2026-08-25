package com.mora.portaria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.mora.portaria.entity.Visitante;
import com.mora.portaria.enums.StatusAcesso;
import java.util.List;
import java.util.Optional;

public interface VisitanteRepository extends JpaRepository<Visitante, String> {
    List<Visitante> findByStatus(StatusAcesso status);
    Optional<Visitante> findByCpf(String cpf);
}

