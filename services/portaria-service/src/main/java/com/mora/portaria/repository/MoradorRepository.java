package com.mora.portaria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.mora.portaria.entity.Morador;
import java.util.List;
import java.util.Optional;

public interface MoradorRepository extends JpaRepository<Morador, String> {
    Optional<Morador> findByCpf(String cpf);
    Optional<Morador> findByEmail(String email);
    List<Morador> findByAtivo(boolean ativo);

    @Query("SELECT m FROM Morador m LEFT JOIN FETCH m.apartamento WHERE m.id = :id")
    Optional<Morador> findByIdComApartamento(@Param("id") String id);
}

