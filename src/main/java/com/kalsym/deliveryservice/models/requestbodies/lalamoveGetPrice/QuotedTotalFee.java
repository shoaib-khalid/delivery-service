package com.kalsym.deliveryservice.models.RequestBodies.lalamoveGetPrice;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class QuotedTotalFee {
    public String amount;
    public String currency;

    public QuotedTotalFee(String amount, String currency) {
        this.amount = amount;
        this.currency = currency;
    }


}
