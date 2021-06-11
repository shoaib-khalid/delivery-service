package com.kalsym.deliveryservice.service.utility.Response;


import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class OrderItemData {
    String id;
    String orderId;
    String productId;
    Float price;
    Float productPrice;
    Float weight;
    String SKU;
    int quantity;
    String itemCode;
}
