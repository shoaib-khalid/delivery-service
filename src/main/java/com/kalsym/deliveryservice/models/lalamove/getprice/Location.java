package com.kalsym.deliveryservice.models.lalamove.getprice;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Location {
    public String lat;
    public String lng;

    public Location(String lat, String lng) {
        this.lat = lat;
        this.lng = lng;
    }
}