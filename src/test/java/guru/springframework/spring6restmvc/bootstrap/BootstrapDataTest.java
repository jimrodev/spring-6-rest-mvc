package guru.springframework.spring6restmvc.bootstrap;

import guru.springframework.spring6restmvc.repositories.BeerRepository;
import guru.springframework.spring6restmvc.repositories.CustomerRepository;
import guru.springframework.spring6restmvc.services.BeerCsvService;
import guru.springframework.spring6restmvc.services.BeerCsvServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;

import static org.assertj.core.api.Assertions.assertThat;

// NO FUNCIONA CON SOLO @DataJpaTest
//@DataJpaTest

/*
 ERROR
 org.springframework.beans.factory.UnsatisfiedDependencyException: Error creating bean with name 'guru.springframework.spring6restmvc.bootstrap.BootstrapDataTest': Unsatisfied dependency expressed through field 'csvService': No qualifying bean of type 'guru.springframework.spring6restmvc.services.BeerCsvService' available: expected at least 1 bean which qualifies as autowire candidate. Dependency annotations: {@org.springframework.beans.factory.annotation.Autowired(required=true)}

 Al ejecutar la prueba con * slide @DataJpaTest * nos da el error anterior, viendo la documentación de Spring boot
 https://stackoverflow.com/questions/48347088/spring-test-with-datajpatest-cant-autowire-class-with-repository-but-with-in

 @DataJpaTest can be used if you want to test JPA applications. By default it will configure an in-memory embedded database, scan for @Entity classes and configure Spring Data JPA repositories. Regular @Component beans will not be loaded into the ApplicationContext.

 SOLO ESCANEA POR @Entity, por eso no encuentra el @Bean tipificado como @Service, @Component, @Controller, etc,
 este SLIDE se utiliza para testear @Entity del tipo public interface PersonCrudRepository * extends CrudRepository *

 SOLUCIÓN 1 - Usar @SpringBootTest, El inconveniente es que cargamos todo el contexto de aplicación

 SOLUCIÓN 2 - Instanciar la clase en el Setup() -> csvService = new BeerCsvServiceImpl(); quitando el @Autowired

 SOLUCIÓN 3 - Poniendo el filtro en el @ComponentScan,
 https://www.baeldung.com/spring-componentscan-filter-type
 * IMPORTANTE * En este caso el filtro es del tipo ASSIGNABLE_TYPE Porque estamos buscando una implementación de la interfaz BeerCsvService -> BeerCsvServiceImpl

 Si sólo ponemos FilterType.ANNOTATION * NO ENCUENTRA LA DEPENDENCIA * @DataJpaTest(includeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = BeerCsvService.class))

 SOLUCIÓN 4 - Importar las clases que necesitamos
 @Import(BeerCsvServiceImpl.class)

 */
// SOLUCIÓN 1
// @SpringBootTest

// SOLUCIÓN 3
// @DataJpaTest(includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = BeerCsvService.class))

// SOLUCIÓN 4 - Importar las clases que necesitamos, en nuestro caso BeerCsvServiceImpl
@DataJpaTest
@Import(BeerCsvServiceImpl.class)
class BootstrapDataTest {

    @Autowired
    BeerRepository beerRepository;

    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    BeerCsvService csvService;

    BootstrapData bootstrapData;

    @BeforeEach
    void setUp() {
        // SOLUCIÓN 2 - quitando el @Autowired
        // csvService = new BeerCsvServiceImpl();

        bootstrapData = new BootstrapData(beerRepository, customerRepository, csvService);
    }

    @Test
    void Testrun() throws Exception {
        bootstrapData.run(null);

        assertThat(beerRepository.count()).isEqualTo(2413);
        assertThat(customerRepository.count()).isEqualTo(3);
    }
}





