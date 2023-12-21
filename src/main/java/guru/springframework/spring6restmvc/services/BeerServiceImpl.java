package guru.springframework.spring6restmvc.services;

import guru.springframework.spring6restmvc.exceptions.NotFoundException;
import guru.springframework.spring6restmvc.model.BeerDTO;
import guru.springframework.spring6restmvc.model.BeerStyle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.*;

@Slf4j
@Service("map")
public class BeerServiceImpl implements BeerService {

    private Map<UUID, BeerDTO> beerMap;

    public BeerServiceImpl() {
        this.beerMap = new HashMap<>();

        BeerDTO beerDTO1 = BeerDTO.builder()
                .id(UUID.randomUUID())
                .version(1)
                .beerName("Galaxy Cat")
                .beerStyle(BeerStyle.PALE_ALE)
                .upc("12356")
                .price(new BigDecimal("12.99"))
                .quantityOnHand(122)
                .createdDate(LocalDateTime.now())
                .updateDate(LocalDateTime.now())
                .build();

        BeerDTO beerDTO2 = BeerDTO.builder()
                .id(UUID.randomUUID())
                .version(1)
                .beerName("Crank")
                .beerStyle(BeerStyle.PALE_ALE)
                .upc("12356222")
                .price(new BigDecimal("11.99"))
                .quantityOnHand(392)
                .createdDate(LocalDateTime.now())
                .updateDate(LocalDateTime.now())
                .build();

        BeerDTO beerDTO3 = BeerDTO.builder()
                .id(UUID.randomUUID())
                .version(1)
                .beerName("Sunshine City")
                .beerStyle(BeerStyle.IPA)
                .upc("12356")
                .price(new BigDecimal("13.99"))
                .quantityOnHand(144)
                .createdDate(LocalDateTime.now())
                .updateDate(LocalDateTime.now())
                .build();

        beerMap.put(beerDTO1.getId(), beerDTO1);
        beerMap.put(beerDTO2.getId(), beerDTO2);
        beerMap.put(beerDTO3.getId(), beerDTO3);
    }

    @Override
    public List<BeerDTO> list(){
        return new ArrayList<>(beerMap.values());
    }

    // Optional
    // 1* - Poner el optional en la capa de datos DAO y que sea el * @Services * el que devuelva la excepción ?? -> spring-boot-example
    // 2  - Que la excepción la lanza el * @RestController *
    @Override
    public BeerDTO getById(UUID id) {

        log.debug("Get Beer by Id - in service. Id: " + id.toString());

        BeerDTO beerDTO = beerMap.get(id);

        // 1 - PARA LAS PRUEBAS (Caso 1)
        // Para simular que el service dispara la excepción del tipo NotFoundException
        // Código real si el Optional lo devolviera el DAO sería: beerDAO.get(id).orElseThrow(NotFoundException::new)
        //if (beer == null) throw new NotFoundException();

        // 2 - El Optional lo ponemos AQUI en el @Service y que sea el @RestController el que lance la excepción
        // Cambio Optional.of pro stream().. ya que el Optional.of de un null da una Excepcion NullPointerException aquí y esa no está controlada
        // Así que muestra un 500 en el POSTMAN y lo que queremos ver es el 404 Not found
        // return Optional.of(beerMap.get(id));
        return beerMap.entrySet().stream()
                .filter(c -> c.getKey().equals(id))
                .map((Map.Entry<UUID, BeerDTO> c) -> {return c.getValue();}) //.map(c -> c.getValue()) //.map(Map.Entry::getValue) // Method reference
                .findFirst()
                .orElseThrow(NotFoundException::new);
    }

    @Override
    public BeerDTO save(BeerDTO beer) {

        BeerDTO savedBeerDTO = BeerDTO.builder()
                .id(UUID.randomUUID())
                .version(1)
                .createdDate(LocalDateTime.now())
                .updateDate(LocalDateTime.now())
                .beerName(beer.getBeerName())
                .beerStyle(beer.getBeerStyle())
                .quantityOnHand(beer.getQuantityOnHand())
                .upc(beer.getUpc())
                .price(beer.getPrice())
                .build();

        beerMap.put(savedBeerDTO.getId(), savedBeerDTO);

        return savedBeerDTO;
    }

    @Override
    public BeerDTO updateById(UUID id, BeerDTO beer) {
        // Recuperamos el beer by ID que queremos actualiar
        //BeerDTO existingBeerDTO = beerMap.get(id);

        BeerDTO existingBeerDTO = beerMap.entrySet().stream()
                .filter(c -> c.getKey().equals(id))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow(NotFoundException::new);

        // Actualzamos los valores - Habría que VALIDAR los valores que no son NULOS... etc
        if (beer.getVersion() != null)          existingBeerDTO.setVersion(beer.getVersion());
        if (beer.getBeerName() != null)         existingBeerDTO.setBeerName(beer.getBeerName());
        if (beer.getBeerStyle() != null)        existingBeerDTO.setBeerStyle(beer.getBeerStyle());
        if (beer.getUpc() != null)              existingBeerDTO.setUpc(beer.getUpc());
        if (beer.getPrice() != null)            existingBeerDTO.setPrice(beer.getPrice());
        if (beer.getQuantityOnHand() != null)   existingBeerDTO.setQuantityOnHand(beer.getQuantityOnHand());

        // Actualizamos la fecha
        existingBeerDTO.setUpdateDate(LocalDateTime.now());

        BeerDTO updatedBeer = beerMap.put(existingBeerDTO.getId(), existingBeerDTO);

        return updatedBeer;
    }

    @Override
    public void deleteById(UUID id) {
        beerMap.remove(id);
    }

    @Override
    public void patchById(UUID id, BeerDTO beer) {
        // Recuperamos el beer by ID que queremos actualiar
        BeerDTO existingBeerDTO = beerMap.get(id);

        if(StringUtils.hasText(beer.getBeerName())){
            existingBeerDTO.setBeerName(beer.getBeerName());
        }
        if(beer.getBeerStyle() != null){
            existingBeerDTO.setBeerStyle(beer.getBeerStyle());
        }
        if (beer.getPrice() != null){
            existingBeerDTO.setPrice(beer.getPrice());
        }
        if(beer.getQuantityOnHand() != null){
            existingBeerDTO.setQuantityOnHand(beer.getQuantityOnHand());
        }
        if(StringUtils.hasText(beer.getUpc())){
            existingBeerDTO.setUpc(beer.getUpc());
        }
    }
}
