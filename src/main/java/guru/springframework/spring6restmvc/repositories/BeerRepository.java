package guru.springframework.spring6restmvc.repositories;

import guru.springframework.spring6restmvc.entities.Beer;
import guru.springframework.spring6restmvc.model.BeerStyle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    // VIDEO - 159 - RETURN - Page<BeerDTO>
    // List<Beer> findAllByBeerNameIsLikeIgnoreCase(String beerName);
    ///List<Beer> findAllByBeerStyle(BeerStyle style);
    // List<Beer> findAllByBeerNameIsLikeIgnoreCaseAndBeerStyle(String beerName, BeerStyle beerStyle);

    Page<Beer> findAllByBeerNameIsLikeIgnoreCase(String beerName, Pageable pageable);
    Page<Beer> findAllByBeerStyle(BeerStyle style, Pageable pageable);
    Page<Beer> findAllByBeerNameIsLikeIgnoreCaseAndBeerStyle(String beerName, BeerStyle beerStyle, Pageable pageable);
}
