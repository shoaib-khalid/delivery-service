package com.kalsym.deliveryservice.service.utility.Response;


import com.kalsym.deliveryservice.models.enums.VehicleType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CartDetails {
    Double totalWeight;
    VehicleType vehicleType;
    Integer totalPcs;
}
