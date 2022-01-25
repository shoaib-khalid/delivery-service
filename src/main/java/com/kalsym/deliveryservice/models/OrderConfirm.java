package com.kalsym.deliveryservice.models;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class OrderConfirm {
    String orderId;
    Long deliveryQuotationId;
}
