package com.kalsym.deliveryservice.models.lalamove.getprice;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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