/*
 *  Copyright 2019 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package guru.springframework.spring6restmvc.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.sql.Timestamp;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
// @AllArgsConstructor  V167 - Como usamos el builder method from lombk, este no usa el setter específico para la propiedad customer, hay que crear un constructor que lo use
@Builder
public class BeerOrder {

    public BeerOrder(UUID id, Long version, Timestamp createdDate, Timestamp lastModifiedDate, String customerRef, Customer customer, Set<BeerOrderLine> beerOrderLines, BeerOrderShipment beerOrderShipment) {
        this.id = id;
        this.version = version;
        this.createdDate = createdDate;
        this.lastModifiedDate = lastModifiedDate;
        this.customerRef = customerRef;
        this.setCustomer(customer);                     // V167 - Usamos el setter que mantiene la relación bidireccional
        this.beerOrderLines = beerOrderLines;
        this.setBeerOrderShipment(beerOrderShipment);   // V171 - Usamos el setter que mantiene la relación bidireccional
    }

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36, columnDefinition = "varchar(36)", updatable = false, nullable = false )
    private UUID id;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(updatable = false)
    private Timestamp createdDate;

    @UpdateTimestamp
    private Timestamp lastModifiedDate;

    public boolean isNew() {
        return this.id == null;
    }

    private String customerRef;

    @ManyToOne
    private Customer customer;

    // Helper Methods
    public void setCustomer(Customer customer) {
        this.customer = customer;
        // V167
        // Mantenemos la relación bidireccional - Tenemos que asegurarnos que el set<BeerOrder> en la entity @Customer haya sido inicializada
        customer.getBeerOrders().add(this);
    }

    // Helper Methods
    public void setBeerOrderShipment(BeerOrderShipment beerOrderShipment) {
        this.beerOrderShipment = beerOrderShipment;
        // V171
        // Mantenemos la relación bidireccional - Tenemos que asegurarnos que el set<BeerOrder> en la entity @Customer haya sido inicializada
        beerOrderShipment.setBeerOrder(this);
    }

    @OneToMany(mappedBy = "beerOrder")
    private Set<BeerOrderLine> beerOrderLines;

    // 171 - CASCADE - Con la opción @OneToOne(cascade = CascadeType.PERSIST), le indicamos a hibernate que SIEMPRE que salvemos el Objeto Beer,
    // El objeto BeerOrderShipment también será actualizado en la BBDD
    // En el V172 - REVISAR LOS DISTINTOS TIPOS DE CascadeType [ALL, PERSIST, MERGE, REMOVE, REFRESH, DETACH, * DELETE *, SAVE_UPDATE, REPLICA, LOCK ]

    // @OneToOne
    @OneToOne(cascade = CascadeType.PERSIST)
    private BeerOrderShipment beerOrderShipment;

}
