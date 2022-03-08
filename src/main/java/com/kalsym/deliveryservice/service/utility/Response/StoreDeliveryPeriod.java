package com.kalsym.deliveryservice.service.utility.Response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StoreDeliveryPeriod {
    String id;
    String storeId;
    boolean enabled;
    String deliveryPeriod;
    DeliveryPeriodDetails deliveryPeriodDetails;


}
