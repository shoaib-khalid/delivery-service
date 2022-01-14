package com.kalsym.deliveryservice.models.RequestBodies.lalamoveGetPrice;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddPriorityFeeRequestBody {
    String tips;

    public AddPriorityFeeRequestBody(String tips) {
        this.tips = tips;
    }
}
