package com.kalsym.deliveryservice.models.RequestBodies.lalamoveGetPrice;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Delivery{
    public int toStop;
    public Contact toContact;
    public String remarks;

    public Delivery(int toStop, Contact toContact, String remarks) {
        this.toStop = toStop;
        this.toContact = toContact;
        this.remarks = remarks;
    }
}