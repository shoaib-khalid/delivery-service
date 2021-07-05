package com.kalsym.deliveryservice.models.lalamove.getprice;

import com.kalsym.deliveryservice.models.lalamove.getprice.GetPrices;


public class PlaceOrder extends GetPrices{
    public QuotedTotalFee quotedTotalFee;
    public boolean sms;
    public boolean pod;
    public String fleetOption;

    public PlaceOrder(GetPrices getPrice, QuotedTotalFee quotedTotalFee){
        super(getPrice.serviceType, getPrice.specialRequests, getPrice.stops, getPrice.requesterContact, getPrice.deliveries);
        this.quotedTotalFee = quotedTotalFee;
    }
}


