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
    public Location location;
    public Addresses addresses;

    public Stop(Location location, Addresses addresses) {
        this.location = location;
        this.addresses = addresses;
    }
}