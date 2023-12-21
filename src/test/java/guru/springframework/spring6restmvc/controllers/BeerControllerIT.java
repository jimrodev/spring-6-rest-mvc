package guru.springframework.spring6restmvc.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import guru.springframework.spring6restmvc.entities.Beer;
import guru.springframework.spring6restmvc.exceptions.NotFoundException;
import guru.springframework.spring6restmvc.mappers.BeerMapper;
import guru.springframework.spring6restmvc.model.BeerDTO;
import guru.springframework.spring6restmvc.model.BeerStyle;
import guru.springframework.spring6restmvc.repositories.BeerRepository;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.ExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MockMvcBuilder;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// IT - INTEGRATION TEST
// Cargamos todo el contexto de Spring ya que queremos hacer una prueba de integración completa con la bbdd, etc

// Testing Srping Boot Applications con WebTestClient
// Por defecto @SpringBootTest no inicia el Web Server, Tenemos que usar el atributo [webEnvironment] de @SpringBootTest
// para definir si se inicia o no el servidor Web
// Modos [* Si arranca un real web server)
// WebEnvironment.MOCK [Default]
// WebEnvironment.RANDOM_PORT   (*)
// WebEnvironment.DEFINED_PORT  (*)
// WebEnvironment.NONE
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BeerControllerIT {
    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    BeerController beerController;

    @Autowired
    BeerRepository beerRepository;
    
    @Autowired
    BeerMapper beerMapper;

    @Autowired
    ObjectMapper objectMapper;

    // Es una referencia al Web Application Context, recuerda que hemos inicializado el Web Server con: @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    @Autowired
    WebApplicationContext wac;

    // Aquí vamos a usar MockMvc para hacer las peticiones
    MockMvc mockMvc;

    @BeforeEach
    void setup() {
        // Configuramos el entorno de mockMvc con los repositorios de Srpring Data injectados
        // Asi tendremos un entorno de pruebas con todo lo que necesitamos (EN ESTE CASO POR IR CON MockMvc no por WebTestClient)
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }
    @Test
    void testGetById() {
        Beer beer = beerRepository.findAll().get(0);

        // 1 . Llamando al método del controller
        // BeerDTO dto = beerController.getById(beer.getId());
        // assertThat(dto).isNotNull();

        // 2 . Uitlizando WebTestClient para hacer una petición HTTP al endpoint del controller
        BeerDTO dto = webTestClient.get()
                .uri(BeerController.BEER_PATH_ID, beer.getId())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<BeerDTO>(){})
                .returnResult()
                .getResponseBody();

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(beer.getId());
    }

    @Test
    void testGetByIdThrowsNotFoundException() {
        // 1 . Llamando al método del controller
        // assertThrows(NotFoundException.class, () ->  {
        //      beerController.getById(UUID.randomUUID());
        // });

        // 2 . Uitlizando WebTestClient para hacer una petición HTTP al endpoint del controller
        webTestClient.get()
                .uri(BeerController.BEER_PATH_ID, UUID.randomUUID())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound(); // .isOk() para que FALLE y ver el 404
    }

    @Test
    void testList() {

        // 1 . Llamando al método del controller
        //List<BeerDTO> dtos = beerController.list();
        // assertThat(dtos.size()).isEqualTo(3);

        // 2 . Uitlizando WebTestClient para hacer una petición HTTP al endpoint del controller
        List<BeerDTO> allBeers = webTestClient.get()
                .uri(BeerController.BEER_PATH)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(new ParameterizedTypeReference<BeerDTO>(){})
                .returnResult()
                .getResponseBody();

        assertThat(allBeers.size()).isEqualTo(3);
    }

    // 101 - SI ejecutamos todos los test de esta clase puede que no se ejecuten * en orden *
    // Y que el testEmptyList() se ejecute antes que testList() dando lugar a un problema por incostencia de los datos
    // Test splices @DataJpaTest - Spring boot automáticamente hace rollback, por lo tanto el test se ejecuta * dentro de una transaccion *
    // y vuelve al estado previo a ejecutarse
    // Al ir con SpringBootTest * este comportamiento hay que darselo con las anotaciones *
    // @Rollback      from org.springframework.test.annotation.
    // @Transactional from jakarta.transaction Ó org.springframework.transaction.annotation EL COMPORTAMIENTO ES IGUAL
    // Con estas dos anotaciones le decimos a Srping Boot que estamos en una transacci´no
    // Y que haga rollback del dicha transacción al finalizar el test
    // SOLO FUNCIONA con la opción 1 . Llamar a los métodos del controller
    // SI LO HACEMOS con la opción 2 . WebTestClient hace el Rollback antes de hacer el get() ?? y falla

    // En la Opcion 2 - Si ejecutamos el set de Test * JUNTOS * NO sabemos el orden de ejecución y puede ejecutarse el delete por eso ponemos @Rollback, @Transactional
    @Rollback
    @Transactional
    @Test
    void testEmptyList() {

        // Eliminamos los registros en la tabla de Beers
        beerRepository.deleteAll();

        // 1 . Llamando al método del controller
        List<BeerDTO> dtos = beerController.list();
        assertThat(dtos.size()).isEqualTo(0);

        // 2 . Uitlizando WebTestClient para hacer una petición HTTP al endpoint del controller
        //     Aquí no funcionan las anotaciones @Rollback, @Transactional
//        List<BeerDTO> allBeers = webTestClient.get()
//                .uri(BeerController.BEER_PATH)
//                .accept(MediaType.APPLICATION_JSON)
//                .exchange()
//                .expectStatus().isOk()
//                .expectBodyList(new ParameterizedTypeReference<BeerDTO>(){})
//                .returnResult()
//                .getResponseBody();
//
//        assertThat(allBeers.size()).isEqualTo(0);
    }

    // En la Opcion 2 - Si ejecutamos el set de Test * JUNTOS * NO sabemos el orden de ejecución y puede ejecutarse el delete por eso ponemos @Rollback, @Transactional
    @Rollback
    @Transactional
    @Test
    void testSave() {

        // Generamos toddos los campos que tienen * constrains * y que serán validados por marcar con @Validates el @RequestBody
        BeerDTO beerToSave = BeerDTO.builder()
                .beerName("New Beer")
                .beerStyle(BeerStyle.PALE_ALE)
                .upc("234545456465")
                .price(new BigDecimal("11.99"))
                .build();

        // 1 . Llamando al método del controller
        // ResponseEntity responseEntity = beerController.create(beerDTO);

        // assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(201));
        // assertThat(responseEntity.getHeaders().getLocation()).isNotNull();

        // 2 . Uitlizando WebTestClient para hacer una petición HTTP al endpoint del controller
        EntityExchangeResult<BeerDTO> exchangeResult = webTestClient.post()
                .uri(BeerController.BEER_PATH)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(beerToSave), BeerDTO.class)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists("Location")
                .expectBody(BeerDTO.class)
                .returnResult();

        HttpHeaders headers = exchangeResult.getResponseHeaders();

        String[] locationUUID = headers.getLocation().getPath().split("/");
        UUID savedUUID = UUID.fromString(locationUUID[4]);

        Beer beerSaved = beerRepository.findById(savedUUID).get();
        assertThat(beerSaved).isNotNull();
    }

    // En la Opcion 2 - Si ejecutamos el set de Test * JUNTOS * NO sabemos el orden de ejecución
    // El @Rollback, @Transactional NO funcionan con el WebTestClient ??
    //@Rollback
    //@Transactional
    @Test
    void testUpdateById() {

        Beer beer = beerRepository.findAll().get(0);
        // BeerDTO beerToUpdate = beerMapper.beerToBeerDto(beer);
        // beerToUpdate.setId(null);
        // beerToUpdate.setVersion(null);
        final String beerName = "NAME UPDATED";

        // Generamos toddos los campos que tienen * constrains * y que serán validados por marcar con @Validates el @RequestBody
        BeerDTO beerToUpdate = BeerDTO.builder()
                .beerName(beerName)
                .beerStyle(BeerStyle.PALE_ALE)
                .upc("234545456465")
                .price(new BigDecimal("11.99"))
                .build();

        // 1 . Llamando al método del controller
        // ResponseEntity responseEntity = beerController.updateById(beer.getId(), beerToUpdate);
        // assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(200));

        // 2 . Uitlizando WebTestClient para hacer una petición HTTP al endpoint del controller
        EntityExchangeResult exchangeResult = webTestClient.put()
                .uri(BeerController.BEER_PATH_ID, beer.getId())
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(beerToUpdate), BeerDTO.class)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .returnResult();

        Beer updatedBeer = beerRepository.findById(beer.getId()).get();
        assertThat(updatedBeer.getBeerName()).isEqualTo(beerName);
    }

    @Test
    void testUpdateByIdThrowsNotFoundException() {

        // Generamos toddos los campos que tienen * constrains * y que serán validados por marcar con @Validates el @RequestBody
        BeerDTO beer = BeerDTO.builder()
                .beerName("My Beer")
                .beerStyle(BeerStyle.PALE_ALE)
                .upc("234545456465")
                .price(new BigDecimal("11.99"))
                .build();

        // 1 . Llamando al método del controller
        assertThrows(NotFoundException.class, ()-> {
            beerController.updateById(
                    UUID.randomUUID(), beer
                    );
        });

        // 2 . Uitlizando WebTestClient para hacer una petición HTTP al endpoint del controller
        webTestClient.put()
                .uri(BeerController.BEER_PATH_ID, UUID.randomUUID())
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(beer), BeerDTO.class)
                .exchange()
                .expectStatus().isNotFound(); // .isOK(); //  Para que falle
    }

    @Rollback
    @Transactional
    @Test
    void testDeleteById() {

        Beer beer = beerRepository.findAll().get(0);

        ResponseEntity responseEntity = beerController.deleteById(beer.getId());

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(200));
        assertThat(beerRepository.findById(beer.getId()).isEmpty());

    }

    @Test
    void testDeleteByIdThrowsNotFoundException() {

        // 1 . Llamando al método del controller
        assertThrows(NotFoundException.class, ()-> {
            beerController.deleteById(UUID.randomUUID());
        });

        // 2 . Uitlizando WebTestClient para hacer una petición HTTP al endpoint del controller
        webTestClient.delete()
                .uri(BeerController.BEER_PATH_ID, UUID.randomUUID())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound(); // .isOK(); //  Para que falle

    }

    @Test
    void testPathByIdThrowsConstraintViolationException() throws Exception {
        Beer beer = beerRepository.findAll().get(0);

        String name = "Este es un tes con un beerName mayor de 50 para que se produzca una excepción de tipo ConstraintViolationException";

        BeerDTO beerToSave = BeerDTO.builder()
                .beerName(name)
                .build();

        // 1 . Llamando al método del controller
        // Al llamar al método del controller la excepción es del tipo TransactionSystemException QUE NO TENEMOS CONTROLADO EN EL ExceptionController
        // Si vemos en la traza la primera excepción que salta es ConstraintViolationException -> RollbackException -> TransactionSystemException

        // Expected :class jakarta.validation.ConstraintViolationException
        //assertThrows(ConstraintViolationException.class, ()-> { ... }

        // Actual   :class org.springframework.transaction.TransactionSystemException
        assertThrows(TransactionSystemException.class, ()-> {
            beerController.updateById(beer.getId(), beerToSave);
        });

        // 2 . Uitlizando WebTestClient para hacer una petición HTTP al endpoint del controller
        EntityExchangeResult exchangeResult = webTestClient.patch()
                .uri(BeerController.BEER_PATH_ID, beer.getId())
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(beerToSave), BeerDTO.class)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$[0].beerName").isEqualTo("el tamaño debe estar entre 0 y 50") // 51 para que falle
                .returnResult();

        byte[] responseBody = exchangeResult.getResponseBodyContent();
        assertNotNull(responseBody);
        assertEquals("[{\"beerName\":[\"el tamaño debe estar entre 0 y 50\"]}]", new String(responseBody, StandardCharsets.UTF_8));

        System.out.println(new String(responseBody, StandardCharsets.UTF_8));

        // Vídeo 116
        // 3 . Hacer la llamada a través de MockMvc inicializado con wac [ mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();]
        Map<String, Object> beerMap = new HashMap<>();
        beerMap.put("beerName", name);

        MvcResult mvcResult = mockMvc.perform(patch(BeerController.BEER_PATH_ID, beer.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(beerMap)))
                .andExpect(status().isBadRequest())
                .andReturn();

        System.out.println(mvcResult.getResponse().getContentAsString());
    }
}