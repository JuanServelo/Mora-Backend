package com.mora.portaria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.mora.portaria.entity.Carro;
import com.mora.portaria.enums.StatusAcesso;
import java.util.List;
import java.util.Optional;

public interface CarroRepository extends JpaRepository<Carro, String> {
    Optional<Carro> findByPlaca(String placa);
    List<Carro> findByStatus(StatusAcesso status);
}

