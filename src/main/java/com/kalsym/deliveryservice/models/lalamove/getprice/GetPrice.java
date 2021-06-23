package com.kalsym.deliveryservice.models.lalamove.getprice;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class GetPrice{
    public String serviceType;
    public List<String> specialRequests;
    public List<Stop> stops;
    public Contact requesterContact;
    public List<Delivery> deliveries;

    public GetPrice(String serviceType, List<String> specialRequests, List<Stop> stops, Contact requesterContact, List<Delivery> deliveries) {
        this.serviceType = serviceType;
        this.specialRequests = specialRequests;
        this.stops = stops;
        this.requesterContact = requesterContact;
        this.deliveries = deliveries;
    }

    @Override
    public String toString() {
        return "GetPrice{" +
                "serviceType='" + serviceType + '\'' +
                ", specialRequests=" + specialRequests +
                ", stops=" + stops +
                ", requesterContact=" + requesterContact +
                ", deliveries=" + deliveries +
                '}';
    }
}