package guru.springframework.spring6restmvc.services;

import guru.springframework.spring6restmvc.model.BeerDTO;
import guru.springframework.spring6restmvc.model.BeerStyle;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface BeerService {

    // Video - 148 - 150
    // VIDEO - 159 - RETURN - Page<BeerDTO>
    // List<BeerDTO> list();
    Page<BeerDTO> list(String name, BeerStyle style, Boolean showInventory, Integer pageNumber, Integer pageSize);

    // Optional
    // MEJOR enfoque el de poner el optional en la capa de datos DAO y que sea el * @Services * el que devuelva la excepción ?? -> spring-boot-example
    // Valorar, según el caso, dónde queremos lanzar la excepción, aquí la excepción la lanza el * @RestController *
    //Optional<BeerDTO> getById(UUID id);
    BeerDTO getById(UUID id);

    BeerDTO save(BeerDTO beer);

    BeerDTO updateById(UUID id, BeerDTO beer);

    void deleteById(UUID id);

    void patchById(UUID id, BeerDTO beer);
}

