package com.kalsym.deliveryservice.models.lalamove.getprice;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class QuotedTotalFee {
    public String amount;
    public String currency;

    public QuotedTotalFee(String amount, String currency) {
        this.amount = amount;
        this.currency = currency;
    }
}
