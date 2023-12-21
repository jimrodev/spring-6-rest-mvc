package guru.springframework.spring6restmvc.services;

import guru.springframework.spring6restmvc.model.BeerDTO;

import java.util.List;
import java.util.UUID;

public interface BeerService {

    List<BeerDTO> list();

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

