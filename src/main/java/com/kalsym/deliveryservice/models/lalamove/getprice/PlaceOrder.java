package com.kalsym.deliveryservice.models.lalamove.getprice;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class PlaceOrder {
    public QuotedTotalFee quotedTotalFees;
    public boolean sms;
    public boolean pod;
    public String fleetOption;


    public PlaceOrder(QuotedTotalFee quotedTotalFee, boolean sms, boolean pod, String fleetOption
                      ) {
        this.quotedTotalFees = quotedTotalFee;
        this.sms = sms;
        this.pod = pod;
        this.fleetOption = fleetOption;
    }
}
