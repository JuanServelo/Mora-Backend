package com.mora.portaria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.mora.portaria.entity.Turno;
import java.util.List;

public interface TurnoRepository extends JpaRepository<Turno, String> {
    List<Turno> findByFuncionario(String funcionario);
}

