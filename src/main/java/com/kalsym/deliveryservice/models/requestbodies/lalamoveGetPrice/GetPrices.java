package com.kalsym.deliveryservice.models.RequestBodies.lalamoveGetPrice;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class GetPrices {
    public String serviceType;
    public List<String> specialRequests;
    public List<Stop> stops;
    public Contact requesterContact;
    public List<Delivery> deliveries;


    public GetPrices(String serviceType, List<String> specialRequests, List<Stop> stops, Contact requesterContact, List<Delivery> deliveries) {
        this.serviceType = serviceType;
        this.specialRequests = specialRequests;
        this.stops = stops;
        this.requesterContact = requesterContact;
        this.deliveries = deliveries;
    }

    @Override
    public String toString() {
        return "GetPrices{" +
                "serviceType='" + serviceType + '\'' +
                ", specialRequests=" + specialRequests +
                ", stops=" + stops +
                ", requesterContact=" + requesterContact +
                ", deliveries=" + deliveries +
                '}';
    }
}