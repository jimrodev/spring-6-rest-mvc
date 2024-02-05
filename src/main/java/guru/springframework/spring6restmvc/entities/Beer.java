package guru.springframework.spring6restmvc.entities;

import guru.springframework.spring6restmvc.model.BeerStyle;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
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
public class Beer {

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

    @NotNull
    @NotBlank
    @Size(max = 50)     // Esta validación se ejecuta antes de operar contra la bbdd (Buena práctica)
    @Column(length = 50)
    private String beerName;

    @NotNull
    private BeerStyle beerStyle;

    @NotNull
    @NotBlank
    @Size(max = 255)
    private String upc;
    private Integer quantityOnHand;

    @NotNull
    private BigDecimal price;

    @OneToMany(mappedBy = "beer")
    private Set<BeerOrderLine> beerOrderLines;

    // V168 - V169 - DEFAULT INITIALIZATION lombok->Builder
    @Builder.Default
    @ManyToMany
    @JoinTable(name = "beer_category",
        joinColumns = @JoinColumn(name = "beer_id"),
        inverseJoinColumns = @JoinColumn(name = "category_id"))
    private Set<Category> categories = new HashSet<>();

    // V169 - Helper Methods para mantener la relación Bidireccional, en este caso sólo en la @Entit Beer porque
    // Categgory será una lista discreta de valores, tabla tipos, aquí la @Entity principal es Beer
    public void addCategory(Category category){
        this.categories.add(category);
        category.getBeers().add(this);
    }

    public void removeCategory(Category category){
        this.categories.remove(category);
        category.getBeers().remove(this);
    }

    //  @CreationTimestamp, @UpdateTimestamp, Cuando el Record es creado, HIBERNATE lo rellena por nosotros
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    private LocalDateTime updateDate;
}

