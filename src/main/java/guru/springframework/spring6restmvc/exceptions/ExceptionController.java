package guru.springframework.spring6restmvc.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.HandlerMethod;

import java.security.KeyPair;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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

// Post sobre control de excepciones avanzado
// https://reflectoring.io/spring-boot-exception-handling/

// # 2 - OPCIÓN 2 - HANDLER GLOBAL
// @ControllerAdvice    // Comentamos @ControllerAdvice y @ExceptionHandler(NotFoundException.class) para QUE * FALLE LA PRUEBA * y usemo el método # 3 @ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Value Not Found") en la clase NotFoundException
@ControllerAdvice
public class ExceptionController {

    private record ResponseBody(LocalDateTime timestamp,
                           Integer status,
                           String error,
                           String message,
                           String path)
    {};

    // Lo comentamos para que vaya por la opción #3 NotFoundException class with @ResponseStatus(code = HttpStatus.NOT_FOUND)
    // Como crear una respuesta identica a la que obtenemos con NotFoundException class whit @ResponseStatus(code = HttpStatus.NOT_FOUND)
    // https://stackoverflow.com/questions/59824851/get-controller-name-and-java-service-path-in-controller-advice
    @ExceptionHandler(NotFoundException.class)
    protected ResponseEntity handlerNotFoundException(NotFoundException exception, HandlerMethod handlerMethod, HttpServletRequest request){

        System.out.println("ExceptionController class - In Not Found Exception Handler");

        // Este SÓLO devuelve las cabeceras - HeadersBuilder
        // return ResponseEntity.notFound().build();

        // Creamos un Pretty body de respuesta al cliente y se lo añadimos al ResponseEntity
        // Con esto tendriamos un comportamiento similar al método #3
        ResponseBody body = new ResponseBody(LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                exception.getMessage(),
                request.getServletPath().toString()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    // Video 112
    // Aunque tengamos las constraints @NotNull, @NotBlanck,... establecidas en los DTO, Entities, etc, estas no se valida nada hasta que anotemos con @Validated el RequestBody en el método del controller
    // @Validation le decimos a spring MVC que el objeto DTO, Entity, etc debe ser validado acorde a las constrains definidas en él
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity handlerArgumentNotValidException(MethodArgumentNotValidException exception){

        // 1 - Devuelve mucha información que no es amigable para devolver tal cual al cliente
        // return ResponseEntity.badRequest().body(exception.getBindingResult().getFieldErrors());

        // 2 - Creamos una lista de los validaciones\constraint que no han pasado dicha validación
        // #1 - El crea un Lis<Map<String, String>> que no consigo ordenar
        // List errorList = exception.getFieldErrors().stream()
        // #2 - Alternativa, creamos una Map<String, String> que se basa en un stream de Pair<String, String> agrupado por First
        Map errorList = exception.getFieldErrors().stream()
                .map(fieldError -> {
                   // #1
                   //Map<String, String> errorMap = new HashMap<>();
                   //errorMap.put(fieldError.getField(), fieldError.getDefaultMessage());

                   // #2
                   Pair<String, String> errorMap = Pair.of(fieldError.getField(), fieldError.getDefaultMessage());
                   return errorMap;
                // #1
                //}).collect(Collectors.toList());
                // #2
                }).collect(Collectors.groupingBy( c -> c.getFirst(), Collectors.mapping( c -> c.getSecond(), Collectors.toList())));

        // Ordenamos la lista resultande de errores de validación
        // https://stackoverflow.com/questions/67816642/java-sort-a-list-of-pairs-using-both-indexes
        //Collections.sort(errorList, Comparator.comparing((Map.Entry<String, String> c) -> {return c.getKey();}));

        // Devolvemos el mapa ordenado por la key
        // https://www.delftstack.com/es/howto/java/how-to-sort-a-map-by-value-in-java/
        return ResponseEntity.badRequest().body(errorList.entrySet().stream().sorted(Map.Entry.comparingByKey()));
    }


    // Vídeo 118
    @ExceptionHandler(ConstraintViolationException.class)
    protected ResponseEntity handlerConstraintViolationException(ConstraintViolationException exception){

        Map errorList = exception.getConstraintViolations().stream()
                .map(fieldError -> {
                    // #1
                    //Map<String, String> errorMap = new HashMap<>();
                    //errorMap.put(fieldError.getField(), fieldError.getDefaultMessage());

                    // #2
                    Pair<String, String> errorMap = Pair.of(fieldError.getPropertyPath().toString(), fieldError.getMessage());
                    return errorMap;
                    // #1
                    //}).collect(Collectors.toList());
                    // #2
                }).collect(Collectors.groupingBy( c -> c.getFirst(), Collectors.mapping( c -> c.getSecond(), Collectors.toList())));

        return ResponseEntity.badRequest().body(errorList.entrySet().stream().sorted(Map.Entry.comparingByKey()));
    }

}
