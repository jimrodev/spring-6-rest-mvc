package guru.springframework.spring6restmvc.services;

import guru.springframework.spring6restmvc.entities.Beer;
import guru.springframework.spring6restmvc.exceptions.NotFoundException;
import guru.springframework.spring6restmvc.mappers.BeerMapper;
import guru.springframework.spring6restmvc.model.BeerDTO;
import guru.springframework.spring6restmvc.repositories.BeerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service("jpa")
// @Primary, cuando tenemos dos implementaciones de BeerService que pueden ser candidatas a ser inyectadas anotamos con @Primary la que queremos que use
// Spring ve dos implementaciones en el class path, y usa la que está marcada con @Primary
@Primary
@RequiredArgsConstructor
public class BeerServiceJPA implements BeerService {

    private final BeerRepository beerRepository;
    private final BeerMapper beerMapper;

    @Override
    public List<BeerDTO> list() {

        // Conversión
        // .map(beerMapper::BeerToBeerDto) - Aquí se produce la conversión de Beer @Entity Object a BeerDTO POJO Object
        // .collect(Collectors.toList()    - Aquí los acumulamos en una lista, List<BeerDTO>
        return beerRepository.findAll()
                .stream()
                .map((Beer c) -> {return beerMapper.beerToBeerDto(c);}) // .map(beerMapper::beerToBeerDto)
                .collect(Collectors.toList());
    }

    @Override
    public BeerDTO getById(UUID id) throws NotFoundException {

//      return Optional.ofNullable(beerMapper.beerToBeerDto(beerRepository.findById(id)
//                .orElse(null)));

        // Al levantar aquí la excepción dejamos el controller con el mínimo código
        // Lo suyo es que desde el DAO devolvamos un Optional y aquí validemos y devolvamos la exccepción
        return beerMapper.beerToBeerDto(beerRepository.findById(id)
                .orElseThrow(NotFoundException::new));
    }

    @Override
    public BeerDTO save(BeerDTO beer) {

        return beerMapper.beerToBeerDto(beerRepository.save(beerMapper.beerDtoToBeer(beer)));
    }

    @Override
    public BeerDTO updateById(UUID id, BeerDTO beer) {

        // Para obtener la referencia al objeto BeerDTO que queremos devolver DENTRO DE LA LAMBDA Function
        // Usamos * AtomicReference *
        AtomicReference<BeerDTO> atomicReferenceToUpdatedBeer = new AtomicReference<>();

        //  beerRepository.findById(id).ifPresent
        beerRepository.findById(id).ifPresentOrElse(foundBeer -> {
                foundBeer.setBeerName(beer.getBeerName());
                foundBeer.setVersion(beer.getVersion());
                foundBeer.setBeerStyle(beer.getBeerStyle());
                foundBeer.setUpc(beer.getUpc());
                foundBeer.setPrice((beer.getPrice()));
                foundBeer.setQuantityOnHand((beer.getQuantityOnHand()));

                atomicReferenceToUpdatedBeer.set(beerMapper.beerToBeerDto(beerRepository.save(foundBeer)));
            }, () -> {
                throw new NotFoundException();
            }
        );

        return atomicReferenceToUpdatedBeer.get();
    }

    @Override
    public void deleteById(UUID id) {

        if(!beerRepository.existsBeerById(id)){
          throw new NotFoundException(
                  "customer with id [%s] does not exist".formatted(id)
          );
        }
        // Delete
        beerRepository.deleteById(id);;
    }

    @Override
    public void patchById(UUID id, BeerDTO beer) {

        // Para obtener la referencia al objeto BeerDTO que queremos devolver DENTRO DE LA LAMBDA Function
        // Usamos * AtomicReference *
        AtomicReference<BeerDTO> atomicReferenceToUpdatedBeer = new AtomicReference<>();

        beerRepository.findById(id).ifPresentOrElse(foundBeer -> {
                if(StringUtils.hasText(beer.getBeerName())){
                    foundBeer.setBeerName(beer.getBeerName());
                }
                if (beer.getBeerStyle() != null){
                    foundBeer.setBeerStyle(beer.getBeerStyle());
                }
                if (beer.getUpc() != null){
                    foundBeer.setUpc(beer.getUpc());
                }
                if (beer.getPrice() != null){
                    foundBeer.setPrice((beer.getPrice()));
                }
                if (beer.getQuantityOnHand() != null){
                    foundBeer.setQuantityOnHand((beer.getQuantityOnHand()));
                }
                atomicReferenceToUpdatedBeer.set(beerMapper.beerToBeerDto(beerRepository.save(foundBeer)));
            }, () -> {
                throw new NotFoundException();
            }
        );

    }
}
