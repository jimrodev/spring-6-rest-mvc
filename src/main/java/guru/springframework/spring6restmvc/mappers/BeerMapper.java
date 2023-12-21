package guru.springframework.spring6restmvc.mappers;


import guru.springframework.spring6restmvc.entities.Beer;
import guru.springframework.spring6restmvc.model.BeerDTO;
import org.mapstruct.Mapper;

// MapStruct
// @Mapper from org.mapstruct.Mapper, ver la configuación dentro del pom.xml, maven-compiler-plugin

// Al anotar esta interfaz con @Mapper -> mapstruct genera la implementación de la interfaz y uitliza los builder de lombok para la generación de mappers.
//

// Importante -> dentro del pom.xml y en la sección <plugin> y el maven-compiler-plugin le indicamos con <compilerArg>-Amapstruct.defaultComponentModel=spring</compilerArg>
// Que las clases generadas  * BeerMapperImpl * SEAN anotadas con el stereotype @Component, esto hara que sea cargada automáticamente en el Spring Context
// <compilerArgs>
//    <compilerArg>-Amapstruct.defaultComponentModel=spring</compilerArg>
// </compilerArgs>
// La clase * BeerMapperImpl * marcada como @Component será recogida por el * Component Scan * y lo añade al Spring Context y usada para la inyección de dependencias.
// La clase * BeerMapperImpl * es una clase generada que está en el target ... classes
@Mapper
public interface BeerMapper {

    Beer beerDtoToBeer(BeerDTO dto);

    BeerDTO beerToBeerDto(Beer beer);
}
