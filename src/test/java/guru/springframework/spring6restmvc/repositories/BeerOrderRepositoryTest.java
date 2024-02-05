package guru.springframework.spring6restmvc.repositories;

import guru.springframework.spring6restmvc.entities.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

//@DataJpaTest
@SpringBootTest   // PARA QUE CARGUE TODO EL CONTEXTO, BottStrapData para cargar la lista de Beers, los 3 customer, etc
class BeerOrderRepositoryTest {

    @Autowired
    BeerOrderRepository beerOrderRepository;

    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    BeerRepository beerRepository;

    Customer testCustomer;
    Beer testBeer;

    @BeforeEach
    void setUp() {
        testCustomer = customerRepository.findAll().get(0);
        testBeer = beerRepository.findAll().get(0);
    }

    @Transactional
    @Test
    void testBeerOrder() {
//        System.out.println(beerOrderRepository.count());
//        System.out.println(customerRepository.count());
//        System.out.println(beerRepository.count());
//        System.out.println(testCustomer.getName());
//        System.out.println(testBeer.getBeerName());

        BeerOrder beerOrder = BeerOrder.builder()
                .customerRef("Test order")
                .customer(testCustomer)
                .beerOrderShipment(BeerOrderShipment.builder()   // V171
                        .trackingNumber("12345")
                        .build())
                .build();


        // INSPECCIONAMOS LA RELACIÓN BI DIRECCONAL (Punto de persistencia en el sout)
        // Si miramos la referencia savedBeerOrder/customer/beerOrder, es decir la relación bidireccional aparece el siguiente error
        // beerOrders = Unable to evaluate the expression Method threw 'org.hibernate.LazyInitializationException' exception.

        // Explicación:
        // Esto es debido a que lo estamos ejecutando fuera de una contexto TRANSACCCIONAL, La solución es poner la anotación @Transactional deltante del método

        // beerOrders = size = 0
        // Ahora nos aparece que no hay relación desde Customer a BeerOrder

        // Explicación:
        // Y esto es debido a que Hibernate piensa que esa relación AÚN NO SE HA HECHO, estamos haciendo un * lazy load * del valor

        // SOLUCIÓN:
        // Si ejecutamos * saveAndFlux * que le indica a HIBERNATE que persista inmediatamente esto en la bbdd.
        // Cuando quiere cargar el valor lo encuentra
        // BeerOrder savedBeerOrder = beerOrderRepository.save(beerOrder);
        // saveAndFlux ES UNA TRANSACCIÓN EXPLÍCITA !!!

        // IMPORTANTE - saveAndFlux puede causar DEGRADACIÓN DEL RENDIMIENTO en casos complejos.
        // * Como mantener * la relación BIDIRECCIONAL * en el objeto savedBeerOrder SIN usar la * TRANSACCIÓN EXPLICITA * savedAndFlux que puede causar * DEGRADACIÓN DE RENDIMIENTO *
        // SOLUCIÓN: VIDEO V167 - ASSOCIATION HELPER METHODS
        // 1 - Ejecutar de nuevo el método save
        // 2 - Usar Helper methods en las @Entity
        //   2.1 - Modificar la @Entity @Customer  ( Nos aseguramos que el set<BeerOrder> esté inicializado, @Builder form lombok -> @Builder.Default   )
        //   2.2 - Modificar la @Entity @BeerOrder ( Eliminamos la anotación @AllArgsConstructor y creamos un costructor con los argumentos que use el * setter modificado setCustomer
        // BeerOrder savedBeerOrder = beerOrderRepository.saveAndFlush(beerOrder);

        BeerOrder savedBeerOrder = beerOrderRepository.save(beerOrder);

        System.out.println(savedBeerOrder.getCustomerRef());

    }
}