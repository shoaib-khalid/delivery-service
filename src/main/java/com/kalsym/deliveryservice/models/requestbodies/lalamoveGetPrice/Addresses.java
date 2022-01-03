package com.kalsym.deliveryservice.models.RequestBodies.lalamoveGetPrice;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Addresses{
    public MsMY ms_MY;

    public Addresses(MsMY ms_MY) {
        this.ms_MY = ms_MY;
    }
}