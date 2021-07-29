package com.kalsym.deliveryservice.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeliveryOption {
    String state;
    Float price;
    String storeId;
}
