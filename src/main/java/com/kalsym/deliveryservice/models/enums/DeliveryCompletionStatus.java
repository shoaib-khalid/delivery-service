package com.kalsym.deliveryservice.models.enums;

import lombok.Getter;
import lombok.Setter;

@Getter
public enum DeliveryCompletionStatus {
    PENDING,
    NEW_ORDER,
    ASSIGNING_RIDER,
    ASSIGNING_DRIVER,
    AWAITING_PICKUP,
    BEING_DELIVERED,
    DELIVERED_TO_CUSTOMER,
    COMPLETED,
    REJECTED,
    CANCELED,
    EXPIRED,
    FAILED;


    DeliveryCompletionStatus() {

    }
}
