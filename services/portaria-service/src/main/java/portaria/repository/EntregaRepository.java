package portaria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import portaria.model.Entrega;
import java.util.List;

public interface EntregaRepository extends JpaRepository<Entrega, String> {
    List<Entrega> findByStatus(String status);
    List<Entrega> findByDestinatarioId(Long destinatarioId);
    List<Entrega> findByBloco(String bloco);
    List<Entrega> findByApartamento(String apartamento);
}
