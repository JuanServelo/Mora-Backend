package com.mora.portaria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.mora.portaria.entity.Entrega;
import java.util.List;

public interface EntregaRepository extends JpaRepository<Entrega, String> {
    List<Entrega> findByStatus(String status);
    List<Entrega> findByDestinatarioId(Long destinatarioId);
    List<Entrega> findByBloco(String bloco);
    List<Entrega> findByApartamento(String apartamento);
}

