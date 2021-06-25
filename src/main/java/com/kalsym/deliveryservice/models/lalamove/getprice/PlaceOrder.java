package com.kalsym.deliveryservice.models.lalamove.getprice;

public class PlaceOrder extends GetPrice{
    public Quotation quotedTotalFee;

    public PlaceOrder(GetPrice getPrice, Quotation quotedTotalFee){
        super(getPrice.serviceType, getPrice.specialRequests, getPrice.stops, getPrice.requesterContact, getPrice.deliveries);
        this.quotedTotalFee = quotedTotalFee;
    }
}
