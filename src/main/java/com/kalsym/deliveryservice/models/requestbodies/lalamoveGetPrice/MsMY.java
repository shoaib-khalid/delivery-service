package com.kalsym.deliveryservice.models.RequestBodies.lalamoveGetPrice;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class MsMY{
    public String displayString;
    public String country;

    public MsMY(String displayString, String country) {
        this.displayString = displayString;
        this.country = country;
    }
}