package portaria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import portaria.model.Entrega;
import java.util.List;

public interface EntregaRepository extends JpaRepository<Entrega, String> {
    List<Entrega> findByRetirada(boolean retirada);
    List<Entrega> findByDestinatario(String destinatario);
}
