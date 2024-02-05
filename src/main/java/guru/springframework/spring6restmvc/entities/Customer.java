package guru.springframework.spring6restmvc.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

// @Data // Using @Data for JPA entities is not recomended. It can cause servere performance and memory consumption issues.
@Getter
@Setter
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    // Problema al pasar de H2 To MySql - ERROR Al crear la Tabla
    // 1 - @JdbcTypeCode(SqlTypes.CHAR)
    // Esta primera configuración es debido a como almacenamos el valor en la tabla - [BINARY] COMO UN STRING Y NO LE GUSTA A MySql
    // binding parameter [9] as [BINARY] - [3603eaec-eb85-4250-bc68-b751fcc72bbd]
    // Con esta configuración @JdbcTypeCode(SqlTypes.CHAR) le indicamos a Hibernate que almacene el UUID como CHARACTER (Indicamos a Hibernate que convierta el UUID a String)
    // 2 - columnDefinition = "varchar(36)" - En MySql el varchar necesita que se especifique la longitud - UUID Tiene 36 posiciones
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36, columnDefinition = "varchar(36)", updatable = false, nullable = false)
    private UUID id;

    @Version
    private Integer version;
    private String name;

    @Column(length = 255)
    private String email;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    private LocalDateTime updateDate;

    @Builder.Default                                        // V167 - Como usamos el patron @Builer de Lombock, con esta anotación le indicamos a lombok que * inicialice * con un HashSet<>() VACIÓ
    @OneToMany(mappedBy = "customer")  // customer es la propiedad en la @Entity * Order *
    private Set<BeerOrder> beerOrders = new HashSet<>();    // V167 - inicializamos con un HashSet<>() VACIÓ
}
