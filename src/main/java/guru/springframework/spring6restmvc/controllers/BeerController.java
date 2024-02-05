package guru.springframework.spring6restmvc.controllers;

import guru.springframework.spring6restmvc.model.BeerDTO;
import guru.springframework.spring6restmvc.model.BeerStyle;
import guru.springframework.spring6restmvc.services.BeerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
// @AllArgsConstructor
// In short, use @AllArgsConstructor        to generate a constructor for all of your class's fields
//           use @RequiredArgsConstructor   to generate a constructor for all class's fields that are marked as final and @NotNull
// @AllArgsConstructor generates a constructor with 1 parameter for each field in your class.
// @RequiredArgsConstructor generates a constructor with 1 parameter for each field that requires special handling. All non-initialized final fields get a parameter, as well as any fields that are marked as @NonNull that aren't initialized where they are declared.
@RequiredArgsConstructor
@RestController
//@Profile("jpa")
// @RequestMapping("/api/v1/beer")   // V80 - NO Utilizamos @RequestMapping por DRY -> Definimos las URLs en un sólo sitio y lo reutilizamos en en el controller y en los Test
public class BeerController {

    public static final String BEER_PATH = "/api/v1/beer";
    public static final String BEER_PATH_ID = BEER_PATH + "/{beerId}" ;

    // Warning - Lombok does not copy the annotation 'org.springframework.beans.factory.annotation.Qualifier' into the constructor
    // Para que lombok añada la anotación al constructor que genera hay que meter la siguiente configuración dentro del fichero lombok.config en el root del proyectos
    // # Copy the Qualifier annotation from the instance variables to the constructor
    // # see https://github.com/rzwitserloot/lombok/issues/745
    // lombok.copyableAnnotations += org.springframework.beans.factory.annotation.Qualifier
    // lombok.copyableAnnotations += org.springframework.beans.factory.annotation.Value

    // COMO USAR PROFILEs EN EL CODIGO - NO en pruebas - PROYECTO NACHO
    //@Qualifier("jpa") @NonNull
    private final BeerService beerService;

    // En este caso al ser un @RestController, la respuesta List<Beer> se devuelve al VIEW HANDLER, que en este caso será
    // Jackson para producir un JSON
    // @RequestMapping(method = RequestMethod.GET)
    @GetMapping(BEER_PATH) // @GetMapping
    // VIDEO - 159 - RETURN - Page<BeerDTO>
    // public List<BeerDTO> list()
    public Page<BeerDTO> list(@RequestParam(required = false) String name,
                              @RequestParam(required = false) BeerStyle style,
                              @RequestParam(required = false) Boolean showInventory,
                              // VIDEO 157 - Paging and Sorting
                              @RequestParam(required = false) Integer pageNumber,
                              @RequestParam(required = false) Integer pageSize){

        //return new ResponseEntity<>(beerService.list(), HttpStatus.OK);
        // VIDEO - 159 - RETURN - Page<BeerDTO>
        // return beerService.list();
        return beerService.list(name, style, showInventory, pageNumber, pageSize);
    }

    // @RequestMapping(value = {beerId}, method = RequestMethod.GET)
    @GetMapping(BEER_PATH_ID) // @GetMapping("{beerId}")
    public BeerDTO getById(@PathVariable("beerId") UUID id){

        log.debug("Get Beer by id - in controller");

        // #1 Relacionado con la prueba BeerControllerTest getBeerById, para ver que el controller no se queja al llamar al
        // BeerService generado por Mockito YA QUE el Mock proporcionado por Mockito *SIEMPRE DEVUELVE UN NULL*
        // Aquí cuando se ejecute la prueba returned siempre será null
        // Beer returned = beerService.getById(id);

        // SIN Optional en el @Service
        // return beerService.getById(id);

        // Optional -> En el @Service buscamos en el map con stream que genera un optional y no da error de NullPointerException
        // Mejor opción es que el @Service genere la excepción y el @Controller tenga el mínimo código
        return beerService.getById(id); //.orElseThrow(NotFoundException::new);

        // Modifico la respuesta con ResponseEntity y así poder configurar BODY, HEADERS, HTTP-STATUS
        //return new ResponseEntity<>(returned, HttpStatus.OK);
    }

    // @RequestMapping(method = RequestMethod.POST)
    @PostMapping(BEER_PATH) // @PostMapping()
    public ResponseEntity<BeerDTO> create(@Validated @RequestBody BeerDTO beer)
    {
        BeerDTO savedBeer = beerService.save(beer);

        // Creamos el objeto HttpHeaders y añadimos la cabecera a la respuesta
        // IMPORTANTE
        // Es una buena practica devolver en la cabecera Location la hubicación del nuevo recurso creado
        HttpHeaders headers = new HttpHeaders();
        headers.add("Location", BEER_PATH + "/" + savedBeer.getId().toString());

        return new ResponseEntity(savedBeer, headers, HttpStatus.CREATED);
    }

    // @RequestMapping(value = {beerId}, method = RequestMethod.PUT)
    @PutMapping(BEER_PATH_ID) // @PutMapping("{beerId}")
    public ResponseEntity updateById(@PathVariable("beerId") UUID id, @Validated @RequestBody BeerDTO beer)
    {
        BeerDTO updatedBeer = beerService.updateById(id, beer);

        return new ResponseEntity(updatedBeer, HttpStatus.OK); // HttpStatus.NO_CONTENT
    }

    // @RequestMapping(value = {beerId}, method = RequestMethod.DELETE)
    @DeleteMapping(BEER_PATH_ID) // @DeleteMapping("{beerId}")
    public ResponseEntity deleteById(@PathVariable("beerId") UUID id){

        beerService.deleteById(id);

        return new ResponseEntity(HttpStatus.OK); // HttpStatus.NO_CONTENT
    }

    // @RequestMapping(value = {beerId}, method = RequestMethod.PATCH)
    @PatchMapping(BEER_PATH_ID) // @PatchMapping("{beerId}")
    public ResponseEntity patchById(@PathVariable("beerId") UUID id, @RequestBody BeerDTO beer){

        beerService.patchById(id, beer);

        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    // # 1
    // @ExceptionHander(NotFoundException.class)
    // Anotamos un método dentro del controller BeerController para que maneje este tipo de excepción NotFoundException.class dentro de esta clase BeerController
    // SOLO ESTE TIPO * NotFoundException.class * Y SOLO DENTRO DE ESTE CONTROLLER * BeerController *

    // # 2
    // @ControllerAdvice from org.springframework.web.bind.annotation.ControllerAdvice
    // Utilizamos esta anotación para obtener un HANDLER GLOBAL que manejará todas las excepciones para todos los controller
    // Si tenemos en una clase un manejador específico @ExceptionHander(NotFoundException.class) y también un handler global @ControllerAdvice, la excepción se procesara primero por el handler específico

    // # 3
    // @ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Value Not Found")
    // Anotamos la * Custom Exception * NotFoundException class con @ResponseStatus, ahora la Excepción incluirá el * STATUS CODE * Apropiado y el la validación del test pasará

    // # 1 - OPCIÓN 1
    // @ExceptionHandler(NotFoundException.class) // - comentamos @ExceptionHandler(NotFoundException.class) para QUE * FALLE LA PRUEBA * y usemos el método # 3 @ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Value Not Found") en la clase NotFoundException
    protected ResponseEntity handlerNotFoundException(){

        System.out.println("BeerController class - In Not Found Exception Handler");

        return ResponseEntity.notFound().build();
    }
}
