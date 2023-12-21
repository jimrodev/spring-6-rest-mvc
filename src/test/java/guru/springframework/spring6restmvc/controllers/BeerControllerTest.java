package guru.springframework.spring6restmvc.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import guru.springframework.spring6restmvc.exceptions.NotFoundException;
import guru.springframework.spring6restmvc.mappers.BeerMapper;
import guru.springframework.spring6restmvc.model.BeerDTO;
import guru.springframework.spring6restmvc.services.BeerService;
import guru.springframework.spring6restmvc.services.BeerServiceImpl;
import org.aspectj.weaver.ast.Not;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// CONTROLLER TEST
// Test CON TODO EL SPRING CONTEXT CARGADO @SpringBootTest
/*
@SpringBootTest
class BeerControllerTest {

    @Autowired
    BeerController beerController;

    @Test
    void getBeerById() {

        // Aquí sout usa el método toString generado por Lombok al añadir la anotación @Data from lombok.Data
        // @Data is Equivalent to @Getter @Setter @RequiredArgsConstructor @ToString @EqualsAndHashCode.
        System.out.println(beerController.getBeerById(UUID.randomUUID()));
    }
}
*/
// CONTROLLER TEST
// Test SIN EL SPRING CONTEXT CARGADO (Test Splice @WebMvcTest)
// Indicamos la clase que estamos probando BeerController.class SI-NO, intentará proporcionar TODOS los controller que encuentre en el path
@WebMvcTest(BeerController.class)
class BeerControllerTest {

    @Autowired
    MockMvc mockMvc;

    // ObjectMapper from import com.fasterxml.jackson.databind.ObjectMapper
    // readValue  - Deserializar o parsear el contenido de un Json en un objeto Java:   JSON -> JAVA OBJECT
    // writeValue - Serializar, un objeto Java en su representación Json:               JAVA OBJECT -> JSON
    @Autowired
    ObjectMapper objectMapper;

    // La anotación @MockBean le dice a Mockito que proporcione un MOCK para BeerService y lo ponga en el SPRING CONTEXT ya que
    // Es la dependencia que necesita el BeerController
    // IMPORTANTE
    // Por defecto Mockito mocks devuelve una respueta NULL, es decir, en la ejecución del test beerService.getBeerById() return NULL
    @MockBean
    BeerService beerService;

    @Captor
    ArgumentCaptor<UUID> uuidArgumentCaptor;

    @Captor
    ArgumentCaptor<BeerDTO> beerArgumentCaptor;

    // Creamos una instancia de BeerServiceImpl para hacer que la respuesta de Mockito NO SEA NULL, en este caso la obtenemos del HashMap
    // Pero podemos crear la instancia manualmente Beer testBeer = new Beer() ....
    BeerServiceImpl beerServiceImpl;

    @BeforeEach
    void setUp() {
        beerServiceImpl = new BeerServiceImpl();
    }

    @Test
    void testGetById() throws Exception {

        // Objetenemos un objeto Beer del map que se creca en el constructor de beerServiceImpl
        // Pero podemos crear la instancia manualmente Beer testBeer = new Beer() ....
        BeerDTO testBeerDTO = beerServiceImpl.list().get(0);

        // given() from import static org.mockito.BDDMockito.given
        // INDICAMOS A Mockito que dada una llamada a beerService.getBeerById(ANY ID) devolverá el objeto testBeer recuperado antes
        // O cualquier objeto Beer que creamos Beer testBeer = new Beer()...
        // any() from import static org.mockito.ArgumentMatchers.any

        // Optional Optional.of(testBeer), lo quitamos porque queremos que sea el @Service el que lance la excepción en caso de no encontrar el Id
        //given(beerService.getById(any(UUID.class))).willReturn(Optional.of(testBeerDTO));
        given(beerService.getById(any(UUID.class))).willReturn(testBeerDTO);

        // Aquí hacemos una llamada GET al controller BeerController y Path /api/v1/beer/{beerId} en la que
        // Aceptamos APPLICATION_JSON como datos de entrada   [Accept:"application/json"]
        // Esperamos un 200 como código de respueta
        // Esperamos APPLICATION_JSON como datos de respuesta [Content-Type:"application/json"]
        // get(), status(), content(), jsonPath() from import static org.springframework.test.web.servlet.* [.request.MockMvcRequestBuilders/.result.MockMvcResultMatchers.]
        // is() from import static org.hamcrest.core.Is.is
        mockMvc.perform(get(BeerController.BEER_PATH_ID, testBeerDTO.getId())  //get("/api/v1/beer/" + UUID.randomUUID())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))            // MediaType.APPLICATION_JSON_UTF8     Para que falle y ver la traza
                .andExpect(jsonPath("$.id", is(testBeerDTO.getId().toString())))
                .andExpect(jsonPath("$.beerName", is(testBeerDTO.getBeerName()))); // is(testBeer.getBeerName()+ "ERROR") Para que falle y ver la traza
    }

    @Test
    void testList() throws Exception {
        // Objetenemos un objeto Beer del map que se creca en el constructor de beerServiceImpl
        // Pero podemos crear la instancia manualmente Beer testBeer = new Beer() ....
        BeerDTO testBeerDTO = beerServiceImpl.list().get(0);

        // INDICAMOS A Mockito que dada una llamada a beerService.listBeer() devolverá la lista que genera el constructor de beerServiceImpl
        // O cualquier Lista de Beer que creamos, private Map<UUID, Beer> beerMap;
        given(beerService.list()).willReturn(beerServiceImpl.list());

        mockMvc.perform(get(BeerController.BEER_PATH)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()", is(3)));  // is(4) para que falle y ver la traza
    }

    @Test
    void testCreate() throws Exception {
        BeerDTO beerDTO = beerServiceImpl.list().get(1);
        // CUIDADO - Si devolvemos beerServiceImpl.listBeers().get(1) y lo modificamos, esas modificaciones se almacenan en el HashMap y da error de null el getId()
        //beer.setVersion(null);
        //beer.setId(null);

        // Esto no es necesario si incluimos ObjectMapper en el Spring Context @Autowire
        // ObjectMapper objectMapper = new ObjectMapper();
        // objectMapper.findAndRegisterModules();
        // IMPORTANTE - Mejor añadir el ObjectMapper al Spring context así tenemos la misma configuración para el Controller y los Test
        //System.out.println(objectMapper.writeValueAsString(beer));

        given(beerService.save(any(BeerDTO.class))).willReturn(beerServiceImpl.list().get(1));

        mockMvc.perform(post(BeerController.BEER_PATH)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(beerDTO)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location")) // "locationS" para que falle y ver la traza
                .andExpect(content().json(objectMapper.writeValueAsString(beerDTO)));  // Con esto validamos que que la respuesta que va a devolver es la que queremos
    }

    @Test
    void testUpdate() throws Exception {

        BeerDTO beerDTO = beerServiceImpl.list().get(0);

        // Aquí le decimos al service que devuelva una instnacia de BeerDTO
        given(beerService.updateById(any(), any())).willReturn(beerDTO);

        mockMvc.perform(put(BeerController.BEER_PATH_ID, beerDTO.getId())
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(beerDTO)))
                .andExpect(status().isOk()) // .isNotFound() para que falle y ver el resultado
                .andExpect(content().json(objectMapper.writeValueAsString(beerDTO)));

        // Esto verifica que el método updateById de beerService FUÉ LLAMADO UNA VEZ, no valida los parámeros por eso ponemos any()
        // Si quisiéramos validar los parámetros tendríamos que poner una * ArgumentCaptor * (VER testDelete)
        verify(beerService).updateById(any(UUID.class), any(BeerDTO.class));
    }

    @Test
    void testDelete() throws Exception {

        BeerDTO beerDTO = beerServiceImpl.list().get(0);

        mockMvc.perform(delete(BeerController.BEER_PATH_ID, beerDTO.getId())
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // NO ES NECESARIO INICIALIZAR AQUI el ArgumentCaptor si usamos la anotación @Captor para reutilizar el ArgumentCaptor en los distintos test
        // @Capture
        // ArgumentCaptor<UUID> uuidArgumentCaptor;
        //ArgumentCaptor<UUID> uuidArgumentCaptor = ArgumentCaptor.forClass(UUID.class);

        // Verificamos que el métido deletById de beerServie FUÉ LLAMADO UNA VEZ
        // SIN VALIDAR LOS PARÁMETROS
        // verify(beerService).deleteById(any(UUID.class));

        // VALIDANDO LOS PARÁMETROS con ArgumenCaptor
        // 1 - Primero los capturamos
        verify(beerService).deleteById(uuidArgumentCaptor.capture());

        // 2 - y luego los validados con assertThat from import static org.assertj.core.api.Assertions.assertThat
        assertThat(beerDTO.getId()).isEqualTo(uuidArgumentCaptor.getValue());
    }

    @Test
    void testPatch() throws Exception {

        BeerDTO beerDTO = beerServiceImpl.list().get(0);

        Map<String, Object> beerMap = new HashMap<>();
        beerMap.put("beerName", "New Name");

        mockMvc.perform(patch(BeerController.BEER_PATH_ID, beerDTO.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(beerMap)))
                .andExpect(status().isNoContent());

        verify(beerService).patchById(uuidArgumentCaptor.capture(), beerArgumentCaptor.capture());

        assertThat(beerDTO.getId()).isEqualTo(uuidArgumentCaptor.getValue());
        assertThat(beerMap.get("beerName")).isEqualTo(beerArgumentCaptor.getValue().getBeerName());

    }

    @Test
    void testGetByIdThrowsNotFoundException() throws Exception {

        // IMPORTANTE - Con Mockito configuramos para que toda llamada a getById de beerService dispare una "Custom Exception" de tipo NOT FOUND EXCEPTION - 404
        given(beerService.getById(any(UUID.class))).willThrow(NotFoundException.class);

        // Optional (Ver si queremos lanzar la excepción en el @Service, ó en el @RestController)
        //given(beerService.getById(any(UUID.class))).willReturn(Optional.empty());

        // EL TEST SIEMPRE FALLA HASTA QUE INCLUYAMOS UN MANEJADOR DE LA EXCEPCIÓN

        // Existen diferentes formas de procesar la exceptión generadas para que el proceso finalice y se devuelva la respusta correcta al Cliente
        // # 1
        // @ExceptionHander(NotFoundException.class)
        // Anotamos un método dentro del controller BeerController para que maneje este tipo de excepción dentro de esta clase, SOLO ESTE TIPO Y SOLO DENTRO DE ESTE CONTROLLER

        mockMvc.perform(get(BeerController.BEER_PATH_ID, UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateByIdThrowsNotFoundException() throws Exception {

        BeerDTO beerDTO = beerServiceImpl.list().get(0);

        // Usamos doThrow ya que given no acepta * void methods *
        doThrow(new NotFoundException()).when(beerService).updateById(any(UUID.class),any(BeerDTO.class));

        mockMvc.perform(put(BeerController.BEER_PATH_ID, UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(beerDTO)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteByIdThrowsNotFoundException() throws Exception {

        // Usamos doThrow ya que given no acepta * void methods *
        doThrow(new NotFoundException()).when(beerService).deleteById(any(UUID.class));

        mockMvc.perform(delete(BeerController.BEER_PATH_ID, UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // Para este test tenemos que definir las constraints en el objeto DTO que utilizamos en la llamada al Controller
    // @NotBlank
    // @NotNull
    // Para indicarle a Spring MVC que el objeto DTO debe ser validado acorde a las constrains definidas en él tenemos que anotar con
    // @Validated el @RequestBody del método del @Controller
    @Test
    void testCreateThrowsMethodArgumentNotValidException() throws Exception {

        BeerDTO beer = BeerDTO.builder().build();

        given(beerService.save(any(BeerDTO.class))).willReturn(beerServiceImpl.list().get(1));

        MvcResult mvcResult = mockMvc.perform(post(BeerController.BEER_PATH)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(beer)))
                .andExpect(status().isBadRequest())
                //.andExpect(jsonPath("$.length()", is(2)))   // Se producen dos errores, @NotNull, @NotBlank (SOLO EN EL CASO DEL NAME)
                .andReturn();

        System.out.println(mvcResult.getResponse().getContentAsString());
    }
}




