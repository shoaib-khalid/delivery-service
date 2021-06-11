package com.kalsym.deliveryservice.service.utility.Response;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class OrderItem {


    List<OrderItemData> data;
    String message;
}
