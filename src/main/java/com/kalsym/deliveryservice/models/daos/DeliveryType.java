package com.kalsym.deliveryservice.models.daos;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "delivery_type")
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeliveryType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    Long deliveryId;
    String deliveryType;
    String region;
    String storeId;
}
