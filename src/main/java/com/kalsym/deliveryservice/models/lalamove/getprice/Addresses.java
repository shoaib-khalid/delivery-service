package com.kalsym.deliveryservice.models.lalamove.getprice;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Addresses{
    public MsMY ms_MY;

    public Addresses(MsMY ms_MY) {
        this.ms_MY = ms_MY;
    }
}