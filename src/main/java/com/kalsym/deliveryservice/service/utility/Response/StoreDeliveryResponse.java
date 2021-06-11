package com.kalsym.deliveryservice.service.utility.Response;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class StoreDeliveryResponse {
    StoreDeliveryResponseData data;
    String message;

}
