package com.kalsym.deliveryservice.models.daos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "delivery_vehicle_types")
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeliveryVehicleTypes {

    @Id
    public String vehicleType;
    @JsonIgnore
    private BigDecimal height;
    @JsonIgnore
    private BigDecimal width;
    @JsonIgnore
    private BigDecimal length;
    @JsonIgnore
    private BigDecimal weight;
    @JsonIgnore
    private Boolean view;

}
