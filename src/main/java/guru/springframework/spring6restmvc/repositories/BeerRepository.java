package guru.springframework.spring6restmvc.repositories;

import guru.springframework.spring6restmvc.entities.Beer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

// Usamos * JpaRepository * en vez de * CrudRepository * porque sino perdemos caracteristicas específicas de JPA como
// Flushing the JPA Session
public interface BeerRepository extends JpaRepository<Beer, UUID> {

    // JPQL
    // Contruye la Query basandose en el nombre del método "existCustomerById"

    // @Query -> or.springframework.data.jpa.repository
    // Esto es para especificar la Query en vez de JPQL
    boolean existsBeerById(UUID id);
}
