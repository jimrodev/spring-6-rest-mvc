package guru.springframework.spring6restmvc.repositories;

import guru.springframework.spring6restmvc.bootstrap.BootstrapData;
import guru.springframework.spring6restmvc.entities.Beer;
import guru.springframework.spring6restmvc.model.BeerStyle;
import guru.springframework.spring6restmvc.services.BeerCsvServiceImpl;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

// debido a que usamos @DataJpaTest * slice * que sólo carga el contexto que sólo carga aquellos @Beans que implementan JPA,
// MISMO ERROR EN * BootstrapDataTest * Ver detalle de la soluciones [1,2,3,4] para cargar el contexto EXTRICTAMENTE NECESARIO
// SOLUCIÓN 4 - Importar las clases que necesitamos, en nuestro caso BootstrapData, BeerCsvServiceImpl
// Que son necesarias para llenar la bbdd en memoria H2, que es la que usamos en las pruebas.
@DataJpaTest
@Import({BootstrapData.class, BeerCsvServiceImpl.class})
class BeerRepositoryTest {

    @Autowired
    BeerRepository beerRepository;

    @Test
    void testSave(){
        Beer savedBeer = beerRepository.save(Beer.builder()
                .beerName("My Beer")
                .beerStyle(BeerStyle.PALE_ALE)
                .upc("234545456465")
                .price(new BigDecimal("11.99"))
                .build());

        // Esto le dice a Hibernate que escriba inmediatamente a la bbdd y se producirá el error de validación en caso de tener que darse
        // Es debido a que Hibernate * Batch * operaciones contra la bbdd - "Lazy write", sin el flush no se ejecutan inmediatamente
        beerRepository.flush();

        assertThat(savedBeer).isNotNull();
        assertThat(savedBeer.getId()).isNotNull();
    }

    @Test
    void testSaveThrowsConstraintViolationException(){
        Beer savedBeer = beerRepository.save(Beer.builder()
                .beerName("My Beer")
                .build());

        // jakarta.validation.ConstraintViolationException: Validation failed for classes
        // List of constraint violations:[
        // ConstraintViolationImpl{interpolatedMessage='no debe ser nulo', propertyPath=upc, rootBeanClass=class guru.springframework.spring6restmvc.entities.Beer, messageTemplate='{jakarta.validation.constraints.NotNull.message}'}
        // ConstraintViolationImpl{interpolatedMessage='no debe ser nulo', propertyPath=price, rootBeanClass=class guru.springframework.spring6restmvc.entities.Beer, messageTemplate='{jakarta.validation.constraints.NotNull.message}'}
        // ConstraintViolationImpl{interpolatedMessage='no debe ser nulo', propertyPath=beerStyle, rootBeanClass=class guru.springframework.spring6restmvc.entities.Beer, messageTemplate='{jakarta.validation.constraints.NotNull.message}'}
        // ConstraintViolationImpl{interpolatedMessage='no debe estar vacío', propertyPath=upc, rootBeanClass=class guru.springframework.spring6restmvc.entities.Beer, messageTemplate='{jakarta.validation.constraints.NotBlank.message}'}
        // ]
        assertThrows(ConstraintViolationException.class, () -> {
            // Esto le dice a Hibernate que escriba inmediatamente a la bbdd y se producirá el error de validación en caso de tener que darse
            beerRepository.flush();

        });
    }

    @Test
    void testSaveThrowsDataIntegrityViolationException(){
        Beer savedBeer = beerRepository.save(Beer.builder()
                .beerName("My Beer Name with a lengt greater than 50 characters. Hibernate by default set 250 characters to Field")
                .beerStyle(BeerStyle.PALE_ALE)
                .upc("234545456465")
                .price(new BigDecimal("11.99"))
                .build());

        // Esto le dice a Hibernate que escriba inmediatamente a la bbdd y se producirá el error de validación en caso de tener que darse
        // Es debido a que Hibernate * Batch * operaciones contra la bbdd - "Lazy write", sin el flush no se ejecutan inmediatamente

        // # 1
        // SI DEJAMOS SOLO @Column(length = 50) a nivel de Entity - la excepción será DataIntegrityViolationException
        //assertThrows(DataIntegrityViolationException.class, () -> {

        // # 2
        // SI AÑADIMOS     @Size(max = 50)      a nivel de Entity - la excepción será ConstraintViolationException
        assertThrows(ConstraintViolationException.class, () -> {
            // Esto le dice a Hibernate que escriba inmediatamente a la bbdd y se producirá el error de validación en caso de tener que darse
            beerRepository.flush();

        });
    }

    @Test
    void testGetListByName() {

        // * Importante * utilizamos Wildcard % para hacer la busqueda "%IPA%" sobre el método ...IsLike...
        // VIDEO - 159 - RETURN - Page<BeerDTO>
        // List<Beer> list = beerRepository.findAllByBeerNameIsLikeIgnoreCase("%IPA%", null);
        Page<Beer> page = beerRepository.findAllByBeerNameIsLikeIgnoreCase("%IPA%", null);

        // FALLA EL TEST
        // org.opentest4j.AssertionFailedError:
        // expected: 30
        // but was: 0
        // Expected :30
        // Actual   :0
        // NO SE ESTÁ EJECUTANDO el código del @Bean BootstrapData y la bbdd está vacía (En este caso H2)
        //
        // Es debido a que usamos @DataJpaTest * slice * que sólo carga el contexto que sólo carga aquellos @Beans que implementan JPA,
        // MISMO ERROR EN * BootstrapDataTest * Ver detalle de la soluciones [1,2,3,4] para cargar el contexto EXTRICTAMENTE NECESARIO
        // SOLUCIÓN 4 - Importar las clases que necesitamos, en nuestro caso
        // VIDEO - 159 - RETURN - Page<BeerDTO>
        // assertThat(list.size()).isEqualTo(336);
        assertThat(page.getContent().size()).isEqualTo(336);
    }
}