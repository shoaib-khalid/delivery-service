package com.kalsym.deliveryservice.models.daos;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "delivery_zone_price")
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeliveryZonePrice implements Serializable {

    @Id
    private Long id;
    private String spId;
    private double weight;
    private BigDecimal withInCity;
    private BigDecimal sameZone;
    private BigDecimal differentZone;
}


