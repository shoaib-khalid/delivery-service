package com.kalsym.deliveryservice.models.lalamove.getprice;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Contact {
    public String name;
    public String phone;

    public Contact(String name, String phone) {
        this.name = name;
        this.phone = phone;
    }
}
