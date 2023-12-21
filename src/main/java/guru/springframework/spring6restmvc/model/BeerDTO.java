package guru.springframework.spring6restmvc.model;

import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Created by jt, Spring Framework Guru.
 */
// @Data is Equivalent to @Getter @Setter @RequiredArgsConstructor @ToString @EqualsAndHashCode.
// Para validarlo dentro de Maven View -> Lifecycle -> compile
// vamos a target -> classes .... buscamos la clase Beer dentro del package model
// vemos el código generado y compilado por maven
// Importante, lo que vemos es una DECOMPILACION hecha por IntelliJ, lo que hay en target es código compilado

// @Builder, Lombok genera el código del código compilado de [public static class BeerBuilder]

// Con la clase seleccionada en el menú Refactor -> Delombok podemos sustutuir la anotación
// @Data, @Builder por el código que genera (NO RECOMENDADO)
@Data
@Builder
public class BeerDTO {
    private UUID id;
    private Integer version;

    // Aquí establecemos los constraints pero no se valida nada hasta que anotemos con @Validated el RequestBody en el método del controller
    // @Validation le decimos a spring MVC que el objeto DTO debe ser validado acorde a las constrains definidas en él
    @NotNull
    @NotBlank
    @Size(max = 50)     // Esta validación se ejecuta antes de operar contra la bbdd (Buena práctica)
    private String beerName;

    @NotNull
    private BeerStyle beerStyle;

    @NotNull
    @NotBlank
    private String upc;
    private Integer quantityOnHand;

    @NotNull
    private BigDecimal price;
    private LocalDateTime createdDate;   // !!! TODO Hay que revisar esta validación, las fechas no vienen en la petición NO FALLA 
    private LocalDateTime updateDate;
}
