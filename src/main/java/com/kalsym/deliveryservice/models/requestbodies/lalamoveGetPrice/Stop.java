package com.kalsym.deliveryservice.models.RequestBodies.lalamoveGetPrice;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class Stop{
    public Coordinates coordinates;
    public String address;

    public Stop(Coordinates coordinates, String address) {
        this.coordinates = coordinates;
        this.address = address;
    }
}