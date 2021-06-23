package com.kalsym.deliveryservice.models.lalamove.getprice;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Stop{
    public Location location;
    public Addresses addresses;

    public Stop(Location location, Addresses addresses) {
        this.location = location;
        this.addresses = addresses;
    }
}