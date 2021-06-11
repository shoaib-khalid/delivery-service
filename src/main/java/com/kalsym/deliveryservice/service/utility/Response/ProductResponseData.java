package com.kalsym.deliveryservice.service.utility.Response;


import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ProductResponseData {

    String id;
    String name;
    String stock;
    String storedId;
    String categoryId;
    String status;
    String region;
    Double weight;
    String deliveryType;
    String itemType;
}
