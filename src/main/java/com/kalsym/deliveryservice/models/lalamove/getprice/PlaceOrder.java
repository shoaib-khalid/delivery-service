package com.kalsym.deliveryservice.models.lalamove.getprice;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class PlaceOrder {
    public QuotedTotalFee quotedTotalFees;
    public boolean sms;
    public boolean pod;
    public String fleetOption;
    public GetPrice getPrice;

    public PlaceOrder(QuotedTotalFee quotedTotalFee, boolean sms, boolean pod, String fleetOption,
                      GetPrice getPrice) {
        this.quotedTotalFees = quotedTotalFee;
        this.sms = sms;
        this.pod = pod;
        this.fleetOption = fleetOption;
        this.getPrice = getPrice;
    }

//    @Override
//    public String toString(){
//        return "PlaceOrder{" +
//
//    }
}
