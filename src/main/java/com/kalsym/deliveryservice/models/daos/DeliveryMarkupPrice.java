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
import java.sql.Time;

@Entity
@Table(name = "delivery_markup_price")
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeliveryMarkupPrice implements Serializable {

    @Id
    private Long id;
    private Time startTime;
    private Time endTime;
    private String deliverySpId;
    private BigDecimal markupPrice;
}
