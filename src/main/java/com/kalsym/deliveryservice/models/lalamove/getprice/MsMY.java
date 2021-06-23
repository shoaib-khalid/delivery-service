package com.kalsym.deliveryservice.models.lalamove.getprice;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MsMY{
    public String displayString;
    public String country;

    public MsMY(String displayString, String country) {
        this.displayString = displayString;
        this.country = country;
    }
}