package com.kalsym.deliveryservice.models.RequestBodies.lalamoveGetPrice;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Coordinates {
    public String lat;
    public String lng;

    public Coordinates(String lat, String lng) {
        this.lat = lat;
        this.lng = lng;
    }
}
