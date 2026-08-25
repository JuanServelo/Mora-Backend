package com.mora.portaria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.mora.portaria.entity.Chave;
import java.util.List;

public interface ChaveRepository extends JpaRepository<Chave, String> {
    List<Chave> findByDisponivel(boolean disponivel);
}

