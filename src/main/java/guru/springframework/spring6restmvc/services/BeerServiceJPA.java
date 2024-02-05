package guru.springframework.spring6restmvc.services;

import guru.springframework.spring6restmvc.entities.Beer;
import guru.springframework.spring6restmvc.exceptions.NotFoundException;
import guru.springframework.spring6restmvc.mappers.BeerMapper;
import guru.springframework.spring6restmvc.model.BeerDTO;
import guru.springframework.spring6restmvc.model.BeerStyle;
import guru.springframework.spring6restmvc.repositories.BeerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Service("jpa")
// @Primary, cuando tenemos dos implementaciones de BeerService que pueden ser candidatas a ser inyectadas anotamos con @Primary la que queremos que use
// Spring ve dos implementaciones en el class path, y usa la que está marcada con @Primary
@Primary
@RequiredArgsConstructor
public class BeerServiceJPA implements BeerService {

    private final BeerRepository beerRepository;
    private final BeerMapper beerMapper;

    // Video 158 - Create Page Request Object
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_PAGE_SIZE = 25;

    public PageRequest buildPageRequest(Integer pageNumber, Integer pageSize){
        // Warning Reasigned local variables ??
        Integer page; // = DEFAULT_PAGE;
        Integer size; // = DEFAULT_PAGE_SIZE;

        if (pageNumber != null && pageNumber > 0){
            page = pageNumber -1;
        } else {
            page = DEFAULT_PAGE;
        }

        if (pageSize == null) {
            size = DEFAULT_PAGE_SIZE;
        } else { if (pageSize > 1000){
            size = 1000;
        } else {
            size = pageSize;
        }}

        Sort sort = Sort.by(Sort.Order.asc("beerName"));

        return PageRequest.of(page, size, sort);
    }
    @Override
    public Page<BeerDTO> list(String name,
                              BeerStyle style,
                              Boolean showInventory,
                              Integer pageNumber,
                              Integer pageSize) {
        // Video - 158
        PageRequest pageRequest = buildPageRequest(pageNumber, pageSize);

        // Video - 152
        // VIDEO - 159 - RETURN - Page<BeerDTO>
        // List<Beer> list; // = new ArrayList<>();
        Page<Beer> page;

        // VALIDAMOS los parámetros y según vengan ejecutamos una consulta y otra, ! BUSCAR ALTERNATIVA CON JDBC para no tener 25 métdos en la búsqueda

        // En TestList() dentro de BeerControllerIT usando MultiValueMap y requestParams(....) se puede tener un único método de busqueda
        // MultiValueMap requestParams = new LinkedMultiValueMap<String, String>();
        // requestParams.add("pageSize", "2413");
        // UriComponents uri = UriComponentsBuilder.fromPath(BeerController.BEER_PATH).queryParams(requestParams).build();

        if(StringUtils.hasText(name) && style == null) {
            page = listByName(name, pageRequest);
        } else if (!StringUtils.hasText(name) && style != null) {
            page = listByStyle(style, pageRequest);
        } else if (StringUtils.hasText(name) && style != null){
            page = listByNameAndStyle(name, style, pageRequest);
        } else {
            page = beerRepository.findAll(pageRequest);
        }

        if(showInventory != null && !showInventory){
            page.forEach(beer -> beer.setQuantityOnHand(null));
        }

        // Conversión
        // .map(beerMapper::BeerToBeerDto) - Aquí se produce la conversión de Beer @Entity Object a BeerDTO POJO Object
        // .collect(Collectors.toList()    - Aquí los acumulamos en una lista, List<BeerDTO>
        // Vídeo - 152
        // return beerRepository.findAll()

        // VIDEO - 159 - RETURN - Page<BeerDTO>
        // return list.stream()
        //        .map((Beer c) -> {return beerMapper.beerToBeerDto(c);}) // .map(beerMapper::beerToBeerDto)
        //        .collect(Collectors.toList());

        // En el objeto page el map se hace directamente para convertir de Beer To BeerDTO
        return page.map(beerMapper::beerToBeerDto);
    }

    // Video - 152
    // VIDEO - 159 - RETURN - Page<BeerDTO>
    // public List<Beer> listByName(String name){
    public Page<Beer> listByName(String name, Pageable pageable){

        return beerRepository.findAllByBeerNameIsLikeIgnoreCase("%" + name + "%", pageable); // Añadimos aquí los wildcard + V 159 la información de paginación
    }
    public Page<Beer> listByStyle(BeerStyle style, Pageable pageable){

        return beerRepository.findAllByBeerStyle(style, pageable); // Añadimos aquí los wildcard
    }
    public Page<Beer> listByNameAndStyle(String name, BeerStyle style, Pageable pageable){

        return beerRepository.findAllByBeerNameIsLikeIgnoreCaseAndBeerStyle("%" + name + "%", style, pageable); // Añadimos aquí los wildcard
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
        // V177 - Optimistic lock and transactional context
        // Optimistic lock se basa en el @Version de la @Entity y se incrementa cada vez que se actualiza
        // Si ejecutamos el test BeerControllerIT - testUpdateBeerBadVersion y actualizamos el version aquí en la actualizacion, debería fallar .save(founBeer)
        // La explicación es que Hibernate, ejecuta el .findById + .save(founBeer) bajo un CONTEXTO TRANSACCIONAL,
        // por eso no mira el @Versión que lo hemos dejamos mal configurado con el setVersion(...)
        // EN RESUMEN, bajo un TRANSACTIONAL CONTEXT Hibernate no Ckeckea la propiedad @Version de la @Entity
        beerRepository.findById(id).ifPresentOrElse(foundBeer -> {
                foundBeer.setBeerName(beer.getBeerName());
                foundBeer.setVersion(beer.getVersion());
                foundBeer.setBeerStyle(beer.getBeerStyle());
                foundBeer.setUpc(beer.getUpc());
                foundBeer.setPrice((beer.getPrice()));
                foundBeer.setQuantityOnHand((beer.getQuantityOnHand()));
                // foundBeer.setVersion(beer.getVersion()); // V177 ESTO DEBERÍA FALLAR PERO NO FALLA CUANDO HACE EL .save(founBeer) Explicación arriba (TRANSACTIONAL CONTEXT)

                atomicReferenceToUpdatedBeer.set(beerMapper.beerToBeerDto(beerRepository.save(foundBeer)));
            }, () -> {
                throw new NotFoundException();
            }
        );
        return atomicReferenceToUpdatedBeer.get();

        // V177 - Para hacer que el test BeerControllerIT - testUpdateBeerBadVersion FALLE es ejecutarlo de la siguiente forma
        // COMENTAR TODO LO ANTERIRO Y SOLO EJECUTAR LO SIGUIENTE
        // EXCEPTION
        // jakarta.servlet.ServletException: Request processing failed: org.springframework.orm.ObjectOptimisticLockingFailureException:
        // Row was updated or deleted by another transaction (or unsaved-value mapping was incorrect) : [guru.springframework.spring6restmvc.entities.Beer#5c3de5ca-0433-4147-861f-694750272ea2]

        // EXPLICACIÓN, Estás salvando un detached object, no lo has recuperado y actualizado como hacemos antes y por lo tanto
        // El optimistic locking, chequea la @Version de la @Entity y como es diferente levanta la excepción anterior

        // return beerMapper.beerToBeerDto(beerRepository.save(beerMapper.beerDtoToBeer(beer)));

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
