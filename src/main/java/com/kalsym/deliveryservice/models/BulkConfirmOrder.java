package com.kalsym.deliveryservice.models;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class BulkConfirmOrder {
    int providerId;
    List<OrderConfirm> orderList;
}
