package guru.springframework.spring6restmvc.controllers;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import guru.springframework.spring6restmvc.entities.Beer;
import guru.springframework.spring6restmvc.exceptions.NotFoundException;
import guru.springframework.spring6restmvc.mappers.BeerMapper;
import guru.springframework.spring6restmvc.model.BeerDTO;
import guru.springframework.spring6restmvc.model.BeerStyle;
import guru.springframework.spring6restmvc.repositories.BeerRepository;
import lombok.val;
import org.hamcrest.core.IsNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Base64Utils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.core.Is.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
    static final String USERNAME = "user1";
    static final String PASSWORD = "password";

    static final String BASIC_AUTH_HEADER = "basic " + Base64Utils.encodeToString((USERNAME + ":" + PASSWORD).getBytes());

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

    // Para poder devolver la iterfaz * Page * con MockMvc y con WebTestClient
    // https://stackoverflow.com/questions/34099559/how-to-consume-pageentity-response-using-spring-resttemplate
    // In a modern spring boot application with Jackson modules enabled by spring boot by default you could do something as simple as:

    // REVISAR Porqué tiene que ser estatic -> A Jackson NO LE GUSTAN las Inner clases NO STATIC porque son dificiles de instanciar
    // Otra solución es NO USAR inner class -> Definir JacksonPage en su propia clas
    // https://dev.to/pavel_polivka/using-java-inner-classes-for-jackson-serialization-4ef8
    // ERROR
    // com.fasterxml.jackson.databind.exc.InvalidDefinitionException: Cannot construct instance of `guru.springframework.spring6restmvc.controllers.BeerControllerIT$JacksonPage`:
    // non-static inner classes like this can only by instantiated using default, no-argument constructor
    @JsonIgnoreProperties("pageable")
    public static class JacksonPage<T> extends PageImpl<T> {
        private JacksonPage(List<T> content, int number, int size, long totalElements) {
            super(content, PageRequest.of(number, size), totalElements);
        }
    }

    // ESTA VERSIÓN GENERA EXACTAMENTE LOS MISMOS RESULTADO QUE LA ANTERIOR
    // https://roufid.com/invaliddefinitionexception-cannot-construct-instance-org-springframework-data-domain-pageimpl/
    public static class ResponsePage<T> extends PageImpl<T> {

        private static final long serialVersionUID = 3248189030448292002L;

        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        public ResponsePage(@JsonProperty("content") List<T> content,
                            @JsonProperty("number") int number,
                            @JsonProperty("size") int size,
                            @JsonProperty("totalElements") Long totalElements,
                            @JsonProperty("pageable") JsonNode pageable,
                            @JsonProperty("last") boolean last,
                            @JsonProperty("totalPages") int totalPages,
                            @JsonProperty("sort") JsonNode sort,
                            @JsonProperty("first") boolean first,
                            @JsonProperty("numberOfElements") int numberOfElements) {
            super(content, PageRequest.of(number, size), totalElements);
        }

        public ResponsePage(List<T> content, Pageable pageable, long total) {
            super(content, pageable, total);
        }

        public ResponsePage(List<T> content) {
            super(content);
        }

        public ResponsePage() {
            super(new ArrayList<T>());
        }
    } // END OF ResponsePage

    @BeforeEach
    void setup() {
        // Configuramos el entorno de mockMvc con los repositorios de Srpring Data injectados
        // Asi tendremos un entorno de pruebas con todo lo que necesitamos (EN ESTE CASO POR IR CON MockMvc no por WebTestClient)
        // V221
        // mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(springSecurity())
                .build();
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
                .header(HttpHeaders.AUTHORIZATION, BASIC_AUTH_HEADER)
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
                .header(HttpHeaders.AUTHORIZATION, BASIC_AUTH_HEADER)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound(); // .isOk() para que FALLE y ver el 404
    }

    @Test
    void testList() throws Exception {

        // 1 . Llamando al método del controller
        // VIDEO - 159 - RETURN - Page<BeerDTO>
        // List<BeerDTO> dtos = beerController.list();
        // assertThat(dtos.size()).isEqualTo(3);
        Page<BeerDTO> dtos = beerController.list(null, null, false, 1, 2413);
        assertThat(dtos.getContent().size()).isEqualTo(1000);

        // 2 . Utilizando MockMmv
        MvcResult mvcResult = mockMvc.perform(get(BeerController.BEER_PATH)
                        .with(httpBasic(USERNAME, PASSWORD))
                .queryParam("pageSize", "2413"))   // V159
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.size()", is(1000))) // V159 - .andExpect(jsonPath("$.size()", is(2413))); - Moficamos a 100 ya que pusimos un límite
                .andReturn();
        Page<BeerDTO> page = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), new TypeReference<JacksonPage<BeerDTO>>() {});
        assertThat(page.getContent().size()).isEqualTo(1000);

        // V159 - Esta es una manera elegante de pasarle parámetro al WebTestClient
        // https://stackoverflow.com/questions/58409743/spring-webflux-webtestclient-with-query-parameter
        MultiValueMap requestParams = new LinkedMultiValueMap<String, String>();
        requestParams.add("pageSize", "2413");

        UriComponents uri = UriComponentsBuilder.fromPath(BeerController.BEER_PATH).queryParams(requestParams).build();

        // 3 . Uitlizando WebTestClient para hacer una petición HTTP al endpoint del controller
        // List<BeerDTO> allBeers = webTestClient.get()
        EntityExchangeResult exchangeResult = webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path(BeerController.BEER_PATH).queryParams(requestParams).build()) //.uri(uri.toUri())   // V159 - //.uri(BeerController.BEER_PATH)
                .accept(MediaType.APPLICATION_JSON)
                // V221
                .header(HttpHeaders.AUTHORIZATION, BASIC_AUTH_HEADER)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<JacksonPage<BeerDTO>>() {}) //.expectBodyList(new ParameterizedTypeReference<BeerDTO>(){})
                //.jsonPath("$.content.size()").isEqualTo(1000)
                //.jsonPath("$.content[0].id").isNotEmpty();
                .returnResult();
                //.getResponseBody();

        Page<BeerDTO> allBeers = (Page<BeerDTO>) exchangeResult.getResponseBody();

        assertThat(allBeers.getContent().size()).isEqualTo(1000); // V159 - assertThat(allBeers.size()).isEqualTo(2413);

        // Post donde se explica la conversión entre LIST Y PAGE <->
        // https://www.baeldung.com/spring-data-jpa-convert-list-page
    }

    @Test
    void testListByName() throws Exception {

        // 1 - Con MockMvc
        // ERROR
        // java.lang.AssertionError: JSON path "$.size()"
        // Expected: is <100>
        //     but: was <2413>
        // Aunque le estamos pasando el parámetro, este no se usa para el filtro
        //
        // Video 148 - 150
        // PRIMERO
        // Modificamos el @Controller, BeerController para que acepte el parámetro en la query
        // public List<BeerDTO> list(@RequestParam(required = false) String name){...}
        // Establecemos @RequestParam(required = false ...) porque queremos seguir teniendo la lista de Beer sin ningún filtro -> testList()

        // SEGUNDO
        // Modificamos el @Service, BeerService para acepte el parámetro
        mockMvc.perform(get(BeerController.BEER_PATH)
                        .with(httpBasic(USERNAME, PASSWORD))
                        .queryParam("name", "IPA")
                        .queryParam("pageSize", "1000"))           // Con MockMvc también se puede usar qeryParam con
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.size()", is(336)));

        // 2 - Con WebTestClient
    }

    @Test
    void testListByStyle() throws Exception {

        // 1 - Con MockMvc
        mockMvc.perform(get(BeerController.BEER_PATH)
                        .with(httpBasic(USERNAME, PASSWORD))
                        .queryParam("style", BeerStyle.IPA.name())
                        .queryParam("pageSize", "1000"))            // Con MockMvc también se puede usar qeryParam con
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.size()", is(548)));


        // 2 - Con WebTestClient
    }
    @Test
    void testListByNameAndStyle() throws Exception {
        // 1 - Con MockMvc
        mockMvc.perform(get(BeerController.BEER_PATH)
                        .with(httpBasic(USERNAME, PASSWORD))
                        .queryParam("name", "IPA")
                        .queryParam("style", BeerStyle.IPA.name())
                        .queryParam("pageSize", "1000"))            // Con MockMvc también se puede usar qeryParam con
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.size()", is(310))); // Aquí sustituimos la expresión "$.size()" por $.content.size() ya que devolvemos el objeto PAGE


        // 2 - Con WebTestClient
    }

    @Test
    void testListByNameAndStyleShowInventoryTrue() throws Exception {

        // 1 - Con MockMvc
        mockMvc.perform(get(BeerController.BEER_PATH)
                        .with(httpBasic(USERNAME, PASSWORD))
                        .queryParam("name", "IPA")
                        .queryParam("style", BeerStyle.IPA.name())
                        .queryParam("showInventory", "true")
                        .queryParam("pageSize", "1000"))            // Con MockMvc también se puede usar qeryParam con
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.size()", is(310))) // Aquí sustituimos la expresión "$.size()" por $.content.size() ya que devolvemos el objeto PAGE
                .andExpect(jsonPath("$.content[0].quantityOnHand").value(IsNull.notNullValue()));


        // 2 - Con WebTestClient

    }
    @Test
    void testListByNameAndStyleShowInventoryFalse() throws Exception {

        // 1 - Con MockMvc
        mockMvc.perform(get(BeerController.BEER_PATH)
                        .with(httpBasic(USERNAME, PASSWORD))
                        .queryParam("name", "IPA")
                        .queryParam("style", BeerStyle.IPA.name())
                        .queryParam("showInventory", "false")
                        .queryParam("pageSize", "1000"))            // Con MockMvc también se puede usar qeryParam con
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.size()", is(310))) // Aquí sustituimos la expresión "$.size()" por $.content.size() ya que devolvemos el objeto PAGE
                .andExpect(jsonPath("$.content[0].quantityOnHand").value(IsNull.nullValue()));

        // 2 - Con WebTestClient

    }

    @Test
    void testListByNameAndStyleShowInventoryTruePage2() throws Exception {
        // 1 - Con MockMvc
        mockMvc.perform(get(BeerController.BEER_PATH)
                        .with(httpBasic(USERNAME, PASSWORD))
                        .queryParam("name", "IPA")
                        .queryParam("style", BeerStyle.IPA.name())
                        .queryParam("showInventory", "false")
                        .queryParam("paging", "true")
                        .queryParam("pageNumber", "1")
                        .queryParam("pageSize", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.size()", is(50)))   // SOLO DEVOLVEMOS UNA PAGINA con 50 registros // Aquí sustituimos la expresión "$.size()" por $.content.size() ya que devolvemos el objeto PAGE
                .andExpect(jsonPath("$.content[0].quantityOnHand").value(IsNull.nullValue()));

        // 2 - Con WebTestClient
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
        // VIDEO - 159 - RETURN - Page<BeerDTO>
        // List<BeerDTO> dtos = beerController.list();
        // assertThat(dtos.size()).isEqualTo(0);
        Page<BeerDTO> dtos = beerController.list(null, null, false, 1, 25);
        assertThat(dtos.getContent().size()).isEqualTo(0);


        // 2 . Uitlizando WebTestClient para hacer una petición HTTP al endpoint del controller
        //     Aquí no funcionan las anotaciones @Rollback, @Transactional
//        List<BeerDTO> allBeers = webTestClient.get()
//                .uri(BeerController.BEER_PATH)
//                .header(HttpHeaders.AUTHORIZATION, BASIC_AUTH_HEADER)
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
                .header(HttpHeaders.AUTHORIZATION, BASIC_AUTH_HEADER)
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
                .header(HttpHeaders.AUTHORIZATION, BASIC_AUTH_HEADER)
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
                .header(HttpHeaders.AUTHORIZATION, BASIC_AUTH_HEADER)
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
                .header(HttpHeaders.AUTHORIZATION, BASIC_AUTH_HEADER)
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
                .header(HttpHeaders.AUTHORIZATION, BASIC_AUTH_HEADER)
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
                        .with(httpBasic(USERNAME, PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(beerMap)))
                .andExpect(status().isBadRequest())
                .andReturn();

        System.out.println(mvcResult.getResponse().getContentAsString());
    }

    @Disabled // just for demo purposes
    @Test
    void testUpdateBeerBadVersion() throws Exception {
        Beer beer = beerRepository.findAll().get(0);

        BeerDTO beerDTO = beerMapper.beerToBeerDto(beer);

        beerDTO.setBeerName("Updated Name");

        MvcResult result = mockMvc.perform(put(BeerController.BEER_PATH_ID, beer.getId())
                        .header(HttpHeaders.AUTHORIZATION, BASIC_AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(beerDTO)))
                .andExpect(status().isOk())
                .andReturn();

        System.out.println(result.getResponse().getContentAsString());

        beerDTO.setBeerName("Updated Name 2");

        MvcResult result2 = mockMvc.perform(put(BeerController.BEER_PATH_ID, beer.getId())
                        .header(HttpHeaders.AUTHORIZATION, BASIC_AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(beerDTO)))
                .andExpect(status().isOk())
                .andReturn();

        System.out.println(result2.getResponse().getStatus());
    }
}